package ru.nesterov.bot.handlers.implementation.invocable;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
import ru.nesterov.bot.handlers.implementation.invocable.stateful.deleteClient.DeleteClientHandler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        DeleteClientHandler.class
})
public class DeleteClientHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private DeleteClientHandler handler;

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
    void fullDeleteClientFlow() {
        List<GetActiveClientResponse> clients = createClients();
        when(client.getActiveClients(anyLong())).thenReturn(clients);

        Update update = createUpdateWithMessage(handler.getCommand());
        List<BotApiMethod<?>> handle = handler.handle(update);
        assertEquals(1, handle.size());
        SendMessage sendMessage = (SendMessage) handle.get(0);
        assertEquals("Выберите клиента для удаления:", sendMessage.getText());
        assertInstanceOf(InlineKeyboardMarkup.class, sendMessage.getReplyMarkup());

        InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) sendMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertEquals("Анна", rows.get(0).get(0).getText());
        assertEquals("Макс", rows.get(1).get(0).getText());

        ButtonCallback selectCallback = new ButtonCallback();
        selectCallback.setCommand("Удалить клиента");
        selectCallback.setValue("Макс");

        Update selectUpdate = createUpdateWithCallbackQuery("Макс");
        List<BotApiMethod<?>> step2Result = handler.handle(selectUpdate);

        assertEquals(1, step2Result.size());
        SendMessage confirmationMessage = (SendMessage) step2Result.get(0);
        assertEquals("Подтвердите удаление", confirmationMessage.getText());

        assertInstanceOf(InlineKeyboardMarkup.class, confirmationMessage.getReplyMarkup());
        InlineKeyboardMarkup confirmKeyboard = (InlineKeyboardMarkup) confirmationMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> confirmRows = confirmKeyboard.getKeyboard();
        assertEquals(1, confirmRows.size());
        assertEquals(2, confirmRows.get(0).size());
        assertEquals("Да", confirmRows.get(0).get(0).getText());
        assertEquals("Нет", confirmRows.get(0).get(1).getText());

        ButtonCallback confirmCallback = new ButtonCallback();
        confirmCallback.setCommand("Удалить клиента");
        confirmCallback.setValue("Да");

        Update confirmUpdate = createUpdateWithCallbackQuery("Да");
        List<BotApiMethod<?>> step3Result = handler.handle(confirmUpdate);

        assertEquals(1, step3Result.size());
        SendMessage successMessage = (SendMessage) step3Result.get(0);
        assertTrue(successMessage.getText().contains("Пользователь Макс успешно удален"));
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
}
