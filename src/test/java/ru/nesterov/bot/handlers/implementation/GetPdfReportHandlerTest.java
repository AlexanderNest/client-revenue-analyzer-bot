package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.dto.GetActiveClientResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.GetPdfReport.GetPdfReportHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        GetPdfReportHandler.class
})
public class GetPdfReportHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private GetPdfReportHandler handler;

    private static final String COMMAND = "Сформировать PDF-отчет";
    private static final String CLIENT_NAME = "Клиент 1";
    private static final String ENTER_FIRST_DATE = "Введите первую дату";
    private static final String ENTER_SECOND_DATE = "Введите вторую дату";

    @AfterEach
    public void resetHandler() {
        handler.resetState(1);
    }

    @Test
    void handleCommandShouldReturnClientNamesKeyboard() {
        when(client.getActiveClients(anyLong())).thenReturn(createActiveClients());

        List<PartialBotApiMethod<?>> result = handler.handle(createUpdateWithCommand());

        assertInstanceOf(SendMessage.class, result.get(0));
        SendMessage sendMessage = (SendMessage) result.get(0);
        assertEquals("Выберите клиента, для которого хотите получить PDF-отчет:", sendMessage.getText());

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);
        List<List<InlineKeyboardButton>> keyboard = ((InlineKeyboardMarkup) markup).getKeyboard();
        assertEquals(2, keyboard.size());
        assertEquals(CLIENT_NAME, keyboard.get(0).get(0).getText());
    }

    @Test
    void handleClientNameShouldReturnCalendarForFirstDate() {
        handler.handle(createUpdateWithCommand());

        List<PartialBotApiMethod<?>> result = handler.handle(createUpdateWithCallbackQuery(CLIENT_NAME));

        assertInstanceOf(EditMessageText.class, result.get(0));
        EditMessageText editMessage = (EditMessageText) result.get(0);
        assertEquals(ENTER_FIRST_DATE, editMessage.getText());
        assertNotNull(editMessage.getReplyMarkup());
    }

    @Test
    void handleFirstDateShouldReturnCalendarForSecondDate() {
        selectClient();

        LocalDate firstDate = LocalDate.now();
        List<PartialBotApiMethod<?>> result = handler.handle(createUpdateWithCallbackQuery(String.valueOf(firstDate)));

        assertInstanceOf(EditMessageText.class, result.get(0));
        EditMessageText editMessage = (EditMessageText) result.get(0);
        assertEquals(ENTER_SECOND_DATE, editMessage.getText());
        assertNotNull(editMessage.getReplyMarkup());
    }

    /**
     * Основной сценарий задачи: по выбранному периоду бот забирает PDF с бэка и отправляет его документом.
     */
    @Test
    void handleSecondDateShouldSendPdfReportBuiltFromSelectedPeriod() {
        InputStream pdfStream = new ByteArrayInputStream("%PDF-1.4 fake".getBytes());
        when(client.getClientPdfReportInputStream(anyLong(), anyString(), any(), any())).thenReturn(pdfStream);

        LocalDate firstDate = LocalDate.now();
        LocalDate secondDate = firstDate.plusDays(5);
        selectClientAndFirstDate(firstDate);

        List<PartialBotApiMethod<?>> result = handler.handle(createUpdateWithCallbackQuery(String.valueOf(secondDate)));

        // вторая дата включается в период целиком, поэтому на бэк уходит следующий день
        verify(client).getClientPdfReportInputStream(
                1L,
                CLIENT_NAME,
                firstDate.atStartOfDay(),
                secondDate.plusDays(1).atStartOfDay()
        );

        assertEquals(2, result.size());

        SendDocument sendDocument = (SendDocument) result.get(1);
        assertEquals("1", sendDocument.getChatId());
        assertSame(pdfStream, sendDocument.getDocument().getNewMediaStream());

        String fileName = sendDocument.getDocument().getMediaName();
        assertTrue(fileName.startsWith("report_" + CLIENT_NAME + "_"), "Некорректное имя файла: " + fileName);
        assertTrue(fileName.endsWith(".pdf"), "Некорректное имя файла: " + fileName);
    }

    /**
     * После отправки отчета календарь должен исчезнуть: сообщение с ним заменяется текстом без клавиатуры.
     */
    @Test
    void handleSecondDateShouldHideCalendarKeyboard() {
        when(client.getClientPdfReportInputStream(anyLong(), anyString(), any(), any()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1}));

        LocalDate firstDate = LocalDate.now();
        selectClientAndFirstDate(firstDate);

        List<PartialBotApiMethod<?>> result = handler.handle(
                createUpdateWithCallbackQuery(String.valueOf(firstDate.plusDays(1)))
        );

        assertInstanceOf(EditMessageText.class, result.get(0));
        EditMessageText editMessage = (EditMessageText) result.get(0);
        assertEquals("PDF-отчет отправлен", editMessage.getText());
        assertNull(editMessage.getReplyMarkup(), "Клавиатура календаря должна пропасть после отправки отчета");
    }

    @Test
    void handleSecondDateEarlierThanFirstShouldReturnErrorAndNotCallBackend() {
        LocalDate firstDate = LocalDate.now();
        selectClientAndFirstDate(firstDate);

        List<PartialBotApiMethod<?>> result = handler.handle(
                createUpdateWithCallbackQuery(String.valueOf(firstDate.minusDays(1)))
        );

        assertInstanceOf(SendMessage.class, result.get(0));
        SendMessage sendMessage = (SendMessage) result.get(0);
        assertTrue(
                sendMessage.getText().startsWith("Вторая дата не может быть раньше первой"),
                "Неожиданный текст ошибки: " + sendMessage.getText()
        );

        verify(client, never()).getClientPdfReportInputStream(anyLong(), anyString(), any(), any());
    }

    /**
     * После отказа по некорректной дате обработчик остается на шаге выбора второй даты,
     * поэтому пользователь может выбрать корректную дату без перезапуска команды.
     */
    @Test
    void handleSecondDateAfterValidationErrorShouldStillAcceptCorrectDate() {
        when(client.getClientPdfReportInputStream(anyLong(), anyString(), any(), any()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1}));

        LocalDate firstDate = LocalDate.now();
        selectClientAndFirstDate(firstDate);

        handler.handle(createUpdateWithCallbackQuery(String.valueOf(firstDate.minusDays(1))));

        LocalDate secondDate = firstDate.plusDays(2);
        List<PartialBotApiMethod<?>> result = handler.handle(createUpdateWithCallbackQuery(String.valueOf(secondDate)));

        assertInstanceOf(SendDocument.class, result.get(1));
        verify(client).getClientPdfReportInputStream(
                1L,
                CLIENT_NAME,
                firstDate.atStartOfDay(),
                secondDate.plusDays(1).atStartOfDay()
        );
    }

    /**
     * Отчет за один день - границу валидации отклонять нельзя.
     */
    @Test
    void handleSecondDateEqualToFirstShouldBeAllowed() {
        when(client.getClientPdfReportInputStream(anyLong(), anyString(), any(), any()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1}));

        LocalDate firstDate = LocalDate.now();
        selectClientAndFirstDate(firstDate);

        List<PartialBotApiMethod<?>> result = handler.handle(createUpdateWithCallbackQuery(String.valueOf(firstDate)));

        assertInstanceOf(SendDocument.class, result.get(1));
        verify(client).getClientPdfReportInputStream(
                1L,
                CLIENT_NAME,
                firstDate.atStartOfDay(),
                firstDate.plusDays(1).atStartOfDay()
        );
    }

    @Test
    void handleSwitchMonthShouldKeepPromptForSecondDate() {
        LocalDate firstDate = LocalDate.now();
        selectClientAndFirstDate(firstDate);

        List<PartialBotApiMethod<?>> result = handler.handle(createUpdateWithCallbackQuery("Next"));

        assertInstanceOf(EditMessageText.class, result.get(0));
        EditMessageText editMessage = (EditMessageText) result.get(0);
        assertEquals(ENTER_SECOND_DATE, editMessage.getText());
        assertNotNull(editMessage.getReplyMarkup());
    }

    private void selectClient() {
        handler.handle(createUpdateWithCommand());
        handler.handle(createUpdateWithCallbackQuery(CLIENT_NAME));
    }

    private void selectClientAndFirstDate(LocalDate firstDate) {
        selectClient();
        handler.handle(createUpdateWithCallbackQuery(String.valueOf(firstDate)));
    }

    private List<GetActiveClientResponse> createActiveClients() {
        GetActiveClientResponse client1 = new GetActiveClientResponse();
        client1.setId(1);
        client1.setName(CLIENT_NAME);

        GetActiveClientResponse client2 = new GetActiveClientResponse();
        client2.setId(2);
        client2.setName("Клиент 2");

        return new ArrayList<>(List.of(client1, client2));
    }

    private Update createUpdateWithCommand() {
        Chat chat = new Chat();
        chat.setId(1L);
        User user = new User();
        user.setId(1L);

        Message message = new Message();
        message.setText(COMMAND);
        message.setChat(chat);
        message.setFrom(user);

        Update update = new Update();
        update.setMessage(message);

        return update;
    }

    private Update createUpdateWithCallbackQuery(String callbackValue) {
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId(String.valueOf(1));

        User user = new User();
        user.setId(1L);
        callbackQuery.setFrom(user);

        Message message = new Message();
        message.setMessageId(1);
        Chat chat = new Chat();
        chat.setId(1L);
        message.setChat(chat);
        callbackQuery.setMessage(message);

        ButtonCallback buttonCallback = new ButtonCallback();
        buttonCallback.setCommand(COMMAND);
        buttonCallback.setValue(callbackValue);
        callbackQuery.setData(buttonCallbackService.getTelegramButtonCallbackString(buttonCallback));

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        return update;
    }
}
