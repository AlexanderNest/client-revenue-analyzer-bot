package ru.nesterov.bot.handlers.implementation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
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
import ru.nesterov.bot.dto.GetClientStatisticResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getClientStatisticHandler.GetClientStatisticHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        GetClientStatisticHandler.class
})
public class GetClientStatisticHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private GetClientStatisticHandler handler;

    private static final String COMMAND = "Узнать статистику по клиенту";

    @AfterEach
    public void resetHandler() {
        handler.resetState(1);
    }

    @Test
    void handleCommandWhenNoClientsFound() {
        Update update = createUpdateWithCommand();

        List<BotApiMethod<?>> botApiMethod = handler.handle(update);
        assertInstanceOf(SendMessage.class, botApiMethod.get(0));
        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);

        assertEquals("Нет доступных клиентов", sendMessage.getText());
    }

    @Test
    void handleCommandWhenClientsFound() throws ParseException {
        Update update = createUpdateWithCommand();

        List<GetActiveClientResponse> clients = createActiveClients();
        when(client.getActiveClients(anyLong())).thenReturn(clients);

        List<BotApiMethod<?>> botApiMethod = handler.handle(update);
        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);

        assertEquals("Выберите клиента, чью статистику хотите узнать:", sendMessage.getText());

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        List<List<InlineKeyboardButton>> clientNamesKeyboard = inlineKeyboardMarkup.getKeyboard();

        assertEquals(4, clientNamesKeyboard.size());

        for (int i = 0; i < clientNamesKeyboard.size(); i++) {
            InlineKeyboardButton button = clientNamesKeyboard.get(i).get(0);
            assertEquals("Клиент " + (i + 1), button.getText());
        }

        GetClientStatisticResponse client1 = new GetClientStatisticResponse();
        client1.setName("Клиент 1");
        client1.setPhone("+123456789");
        client1.setDescription("Test Client");
        client1.setStartDate(new SimpleDateFormat("dd.MM.yyyy").parse("20.09.2025"));
        client1.setServiceDuration(30);
        client1.setSuccessfulMeetingsHours(10);
        client1.setCancelledMeetingsHours(2);
        client1.setIncomePerHour(500);
        client1.setSuccessfulEventsCount(10);
        client1.setPlannedCancelledEventsCount(1);
        client1.setNotPlannedCancelledEventsCount(1);
        client1.setTotalIncome(5000);

        when(client.getClientStatistic(anyLong(), eq("Клиент 1"))).thenReturn(client1);

        ButtonCallback clientCallback = new ButtonCallback();
        clientCallback.setCommand("Выберите клиента, чью статистику хотите узнать:");
        clientCallback.setValue("Клиент 1");
        Update updateWithClientName = createUpdateWithCallbackQuery(clientCallback.getValue());
        List<BotApiMethod<?>> botApiMethod2 = handler.handle(updateWithClientName);

        assertInstanceOf(EditMessageText.class, botApiMethod2.get(0));

        String clientStaticticString =
                "📊 *Статистика клиента*\n" +
                        "\n" +
                        "Имя: Клиент 1\n" +
                        "ID: 0\n" +
                        "Телефон: +123456789\n" +
                        "─────────────────────────────\n" +
                        "Описание: Test Client\n" +
                        "Начало обучения: 20.09.2025\n" +
                        "Продолжительность обучения: 30 месяцев\n" +
                        "─────────────────────────────\n" +
                        "Состоявшихся занятий: 10 часов\n" +
                        "Отмененных занятий: 2 часов\n" +
                        "Доход в час: 500 ₽/час\n" +
                        "Состоявшиеся занятия: 10\n" +
                        "Запланированные отмены: 1\n" +
                        "Незапланированные отмены: 1\n" +
                        "─────────────────────────────\n" +
                        "Суммарный доход: 5\u00A0000 ₽\n";

        EditMessageText editMessageText = (EditMessageText) botApiMethod2.get(0);
        assertEquals(clientStaticticString, editMessageText.getText());
    }

    private List<GetActiveClientResponse> createActiveClients() {
        GetActiveClientResponse client1 = new GetActiveClientResponse();
        client1.setId(1);
        client1.setName("Клиент 1");

        GetActiveClientResponse client2 = new GetActiveClientResponse();
        client2.setId(2);
        client2.setName("Клиент 2");

        GetActiveClientResponse client3 = new GetActiveClientResponse();
        client3.setId(3);
        client3.setName("Клиент 3");

        GetActiveClientResponse client4 = new GetActiveClientResponse();
        client4.setId(4);
        client4.setName("Клиент 4");

        return new ArrayList<>(List.of(client1, client2, client3, client4));
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

    @SneakyThrows
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
