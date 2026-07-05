package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.Assertions;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.dto.GetAllUsersByRoleAndSourceResponse;
import ru.nesterov.bot.dto.Role;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.adminsHandlers.SendMessageToUsersHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        SendMessageToUsersHandler.class,
})
public class SendMessageToUsersHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private SendMessageToUsersHandler sendMessageToUsersHandler;

    @Test
    void sendOriginalMessage() {
        User user = new User();
        user.setId(1L);

        Chat chat = new Chat();
        chat.setId(1L);

        Message message = new Message();
        message.setText(sendMessageToUsersHandler.getCommand());
        message.setChat(chat);
        message.setFrom(user);

        Update update1 = new Update();
        update1.setMessage(message);

        List<PartialBotApiMethod<?>> botApiMethod = sendMessageToUsersHandler.handle(update1);
        SendMessage sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Введите текст рассылки", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);

        Update update2 = new Update();
        message.setText("Рассылка");
        update2.setMessage(message);

        botApiMethod = sendMessageToUsersHandler.handle(update2);
        sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Редактировать сообщение?", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);

        Update update3 = new Update();

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        List<List<InlineKeyboardButton>> keyboard = inlineKeyboardMarkup.getKeyboard();

        assertEquals(1, keyboard.size());

        String firstButtonText = keyboard.get(0).get(0).getText();
        String secondButtonText = keyboard.get(0).get(1).getText();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId(String.valueOf(1L));
        ButtonCallback callback = new ButtonCallback();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callback.setCommand(sendMessageToUsersHandler.getCommand());
        callback.setValue("false");
        callbackQuery.setData(buttonCallbackService.getTelegramButtonCallbackString(callback));

        update3.setCallbackQuery(callbackQuery);
        message.setText("Рассылка");
        update3.setMessage(message);

        GetAllUsersByRoleAndSourceResponse response = new GetAllUsersByRoleAndSourceResponse();
        response.setUserIds(List.of("100", "150"));
        when(client.getUsersIdByRoleAndSource(chat.getId(), Role.USER, "telegram")).thenReturn(response);

        botApiMethod = sendMessageToUsersHandler.handle(update3);

        sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Рассылка", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);
        sendMessage = (SendMessage) (botApiMethod.get(botApiMethod.size() - 1));
        Assertions.assertEquals("Рассылка завершена", sendMessage.getText());
    }

    @Test
    void sendEditedMessage() {
        User user = new User();
        user.setId(2L);

        Chat chat = new Chat();
        chat.setId(2L);

        Message message = new Message();
        message.setText(sendMessageToUsersHandler.getCommand());
        message.setChat(chat);
        message.setFrom(user);

        Update update1 = new Update();
        update1.setMessage(message);

        List<PartialBotApiMethod<?>> botApiMethod = sendMessageToUsersHandler.handle(update1);
        SendMessage sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Введите текст рассылки", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);

        Update update2 = new Update();
        message.setText("Рассылка");
        update2.setMessage(message);

        botApiMethod = sendMessageToUsersHandler.handle(update2);
        sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Редактировать сообщение?", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);

        Update update3 = new Update();

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        List<List<InlineKeyboardButton>> keyboard = inlineKeyboardMarkup.getKeyboard();

        assertEquals(1, keyboard.size());

        String firstButtonText = keyboard.get(0).get(0).getText();
        String secondButtonText = keyboard.get(0).get(1).getText();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId(String.valueOf(2L));
        ButtonCallback callback = new ButtonCallback();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callback.setCommand(sendMessageToUsersHandler.getCommand());
        callback.setValue("true");
        callbackQuery.setData(buttonCallbackService.getTelegramButtonCallbackString(callback));

        update3.setCallbackQuery(callbackQuery);
        message.setText("Рассылка");
        update3.setMessage(message);

        botApiMethod = sendMessageToUsersHandler.handle(update3);
        sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Введите исправленный текст рассылки", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);

        Update update4 = new Update();
        message.setText("Новая рассылка");
        update4.setMessage(message);

        botApiMethod = sendMessageToUsersHandler.handle(update4);
        sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Редактировать сообщение?", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);

        Update update5 = new Update();

        markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        keyboard = inlineKeyboardMarkup.getKeyboard();

        assertEquals(1, keyboard.size());

        firstButtonText = keyboard.get(0).get(0).getText();
        secondButtonText = keyboard.get(0).get(1).getText();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        callbackQuery = new CallbackQuery();
        callbackQuery.setId(String.valueOf(2L));
        callback = new ButtonCallback();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callback.setCommand(sendMessageToUsersHandler.getCommand());
        callback.setValue("false");
        callbackQuery.setData(buttonCallbackService.getTelegramButtonCallbackString(callback));


        GetAllUsersByRoleAndSourceResponse response = new GetAllUsersByRoleAndSourceResponse();
        response.setUserIds(List.of("100", "150"));
        when(client.getUsersIdByRoleAndSource(chat.getId(), Role.USER, "telegram")).thenReturn(response);

        update5.setCallbackQuery(callbackQuery);
        message.setText("Новая рассылка");
        update5.setMessage(message);
        botApiMethod = sendMessageToUsersHandler.handle(update5);

        sendMessage = (SendMessage) (botApiMethod.get(0));
        Assertions.assertEquals("Новая рассылка", sendMessage.getText());
        Assertions.assertInstanceOf(SendMessage.class, sendMessage);
        sendMessage = (SendMessage) (botApiMethod.get(botApiMethod.size() - 1));
        Assertions.assertEquals("Рассылка завершена", sendMessage.getText());
    }
}
