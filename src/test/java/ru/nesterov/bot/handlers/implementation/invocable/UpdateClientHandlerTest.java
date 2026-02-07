package ru.nesterov.bot.handlers.implementation.invocable;

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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.dto.GetActiveClientResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient.UpdateClientHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient.UpdateClientResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        UpdateClientHandler.class
})
public class UpdateClientHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private UpdateClientHandler handler;

    @AfterEach
    public void resetHandler() {
        handler.resetState(1);
    }

    @Test
    void shouldReturnNoClientsMessage() {
        Update update = createUpdateWithMessage(handler.getCommand());
        List<BotApiMethod<?>> handle = handler.handle(update);
        assertEquals(1, handle.size());
        SendMessage sendMessage = (SendMessage) handle.get(0);
        assertEquals("Нет доступных клиентов", sendMessage.getText());
    }

    @Test
    void fullUpdateClientFlowWithoutChanges() {
        List<GetActiveClientResponse> clients = createClients();
        when(client.getActiveClients(anyLong())).thenReturn(clients);

        Update update = createUpdateWithMessage(handler.getCommand());
        List<BotApiMethod<?>> step1Result = handler.handle(update);

        assertEquals(1, step1Result.size());
        SendMessage clientSelectionMessage = (SendMessage) step1Result.get(0);
        assertEquals("Выберите клиента для обновления данных:", clientSelectionMessage.getText());
        assertInstanceOf(InlineKeyboardMarkup.class, clientSelectionMessage.getReplyMarkup());

        InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) clientSelectionMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertEquals("Анна", rows.get(0).get(0).getText());
        assertEquals("Макс", rows.get(1).get(0).getText());

        Update selectUpdate = createUpdateWithCallbackQuery("Макс");
        List<BotApiMethod<?>> step2Result = handler.handle(selectUpdate);

        assertEquals(1, step2Result.size());
        SendMessage nameConfirmationMessage = (SendMessage) step2Result.get(0);
        assertEquals("Обновить имя клиента?", nameConfirmationMessage.getText());

        assertInstanceOf(InlineKeyboardMarkup.class, nameConfirmationMessage.getReplyMarkup());
        InlineKeyboardMarkup confirmKeyboard = (InlineKeyboardMarkup) nameConfirmationMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> confirmRows = confirmKeyboard.getKeyboard();
        assertEquals(1, confirmRows.size());
        assertEquals(2, confirmRows.get(0).size());
        assertEquals("Да", confirmRows.get(0).get(0).getText());
        assertEquals("Нет", confirmRows.get(0).get(1).getText());

        Update noNameUpdate = createUpdateWithCallbackQuery("Нет");
        List<BotApiMethod<?>> step3Result = handler.handle(noNameUpdate);

        assertEquals(1, step3Result.size());
        EditMessageText priceConfirmationMessage = (EditMessageText) step3Result.get(0);
        assertEquals("Обновить стоимость за час клиента?", priceConfirmationMessage.getText());

        Update noPriceUpdate = createUpdateWithCallbackQuery("Нет");
        List<BotApiMethod<?>> step4Result = handler.handle(noPriceUpdate);

        assertEquals(1, step4Result.size());
        EditMessageText descConfirmationMessage = (EditMessageText) step4Result.get(0);
        assertEquals("Обновить описание клиента?", descConfirmationMessage.getText());

        Update noDescUpdate = createUpdateWithCallbackQuery("Нет");
        List<BotApiMethod<?>> step5Result = handler.handle(noDescUpdate);

        assertEquals(1, step5Result.size());
        EditMessageText phoneConfirmationMessage = (EditMessageText) step5Result.get(0);
        assertEquals("Обновить номер телефона клиента?", phoneConfirmationMessage.getText());

        UpdateClientResponse updateResponse = createUpdateClientResponse();
        when(client.updateClient(eq(1L), any())).thenReturn(updateResponse);

        Update noPhoneUpdate = createUpdateWithCallbackQuery("Нет");
        List<BotApiMethod<?>> step6Result = handler.handle(noPhoneUpdate);

        assertEquals(1, step6Result.size());
        EditMessageText successMessage = (EditMessageText) step6Result.get(0);
        assertTrue(successMessage.getText().contains("Клиент успешно обновлен!"));
        assertTrue(successMessage.getText().contains("Имя: Макс"));
        assertTrue(successMessage.getText().contains("Стоимость за час: 999"));
    }

    @Test
    void fullUpdateClientFlowWithAllChanges() {
        List<GetActiveClientResponse> clients = createClients();
        when(client.getActiveClients(anyLong())).thenReturn(clients);

        UpdateClientResponse updateResponse = createUpdateClientResponse();
        updateResponse.setName("Максим");
        updateResponse.setPricePerHour(1200);
        updateResponse.setDescription("Новое описание");
        updateResponse.setPhone("+79998887766");
        when(client.updateClient(eq(1L), any())).thenReturn(updateResponse);

        Update update = createUpdateWithMessage(handler.getCommand());
        handler.handle(update);

        Update selectUpdate = createUpdateWithCallbackQuery("Макс");
        handler.handle(selectUpdate);

        Update yesNameUpdate = createUpdateWithCallbackQuery("Да");
        List<BotApiMethod<?>> step3Result = handler.handle(yesNameUpdate);

        assertEquals(1, step3Result.size());
        SendMessage askNameMessage = (SendMessage) step3Result.get(0);
        assertEquals("Введите новое имя:", askNameMessage.getText());

        Update newNameUpdate = createUpdateWithTextMessage("Максим");
        List<BotApiMethod<?>> step4Result = handler.handle(newNameUpdate);

        assertEquals(1, step4Result.size());
        SendMessage priceConfirmationMessage = (SendMessage) step4Result.get(0);
        assertEquals("Обновить стоимость за час клиента?", priceConfirmationMessage.getText());

        Update yesPriceUpdate = createUpdateWithCallbackQuery("Да");
        List<BotApiMethod<?>> step5Result = handler.handle(yesPriceUpdate);

        assertEquals(1, step5Result.size());
        SendMessage askPriceMessage = (SendMessage) step5Result.get(0);
        assertEquals("Введите новую стоимость клиента за час: ", askPriceMessage.getText());

        Update newPriceUpdate = createUpdateWithTextMessage("1200");
        List<BotApiMethod<?>> step6Result = handler.handle(newPriceUpdate);

        assertEquals(1, step6Result.size());
        SendMessage descConfirmationMessage = (SendMessage) step6Result.get(0);
        assertEquals("Обновить описание клиента?", descConfirmationMessage.getText());

        Update yesDescUpdate = createUpdateWithCallbackQuery("Да");
        List<BotApiMethod<?>> step7Result = handler.handle(yesDescUpdate);

        assertEquals(1, step7Result.size());
        SendMessage askDescMessage = (SendMessage) step7Result.get(0);
        assertEquals("Введите новое описание клиента: ", askDescMessage.getText());

        Update newDescUpdate = createUpdateWithTextMessage("Новое описание");
        List<BotApiMethod<?>> step8Result = handler.handle(newDescUpdate);

        assertEquals(1, step8Result.size());
        SendMessage phoneConfirmationMessage = (SendMessage) step8Result.get(0);
        assertEquals("Обновить номер телефона клиента?", phoneConfirmationMessage.getText());

        Update yesPhoneUpdate = createUpdateWithCallbackQuery("Да");
        List<BotApiMethod<?>> step9Result = handler.handle(yesPhoneUpdate);

        assertEquals(1, step9Result.size());
        SendMessage askPhoneMessage = (SendMessage) step9Result.get(0);
        assertEquals("Введите новый номер телефона: ", askPhoneMessage.getText());

        Update newPhoneUpdate = createUpdateWithTextMessage("+79998887766");
        List<BotApiMethod<?>> step10Result = handler.handle(newPhoneUpdate);

        assertEquals(1, step10Result.size());
        SendMessage successMessage = (SendMessage) step10Result.get(0);
        assertTrue(successMessage.getText().contains("Клиент успешно обновлен!"));
        assertTrue(successMessage.getText().contains("Имя: Максим"));
        assertTrue(successMessage.getText().contains("Стоимость за час: 1200"));
        assertTrue(successMessage.getText().contains("Описание: Новое описание"));
        assertTrue(successMessage.getText().contains("Номер телефона: +79998887766"));
    }

    private List<GetActiveClientResponse> createClients() {
        GetActiveClientResponse client1 = new GetActiveClientResponse();
        client1.setName("Макс");
        client1.setPricePerHour(999);
        client1.setDescription("SSS");
        GetActiveClientResponse client2 = new GetActiveClientResponse();
        client2.setName("Анна");
        client2.setPricePerHour(100);
        client2.setDescription("Zzz");
        return new ArrayList<>(List.of(client1, client2));
    }

    private UpdateClientResponse createUpdateClientResponse() {
        UpdateClientResponse response = new UpdateClientResponse();
        response.setName("Макс");
        response.setPricePerHour(999);
        response.setDescription("SSS");
        response.setPhone("+79990001122");
        return response;
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
        buttonCallback.setCommand(handler.getCommand());
        buttonCallback.setValue(callbackValue);
        callbackQuery.setData(buttonCallbackService.getTelegramButtonCallbackString(buttonCallback));

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        return update;
    }

    @SneakyThrows
    private Update createUpdateWithTextMessage(String text) {
        Message message = new Message();
        message.setMessageId(1);
        message.setText(text);

        Chat chat = new Chat();
        chat.setId(1L);
        message.setChat(chat);

        User user = new User();
        user.setId(1L);
        message.setFrom(user);

        Update update = new Update();
        update.setMessage(message);

        return update;
    }
}
