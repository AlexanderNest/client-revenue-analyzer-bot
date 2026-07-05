package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.nesterov.bot.config.BotProperties;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.implementation.grouping.ClientOperationGroupHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createClient.CreateClientHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.deleteClient.DeleteClientHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient.UpdateClientHandler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        ClientOperationGroupHandler.class,
        CreateClientHandler.class,
        DeleteClientHandler.class,
        UpdateClientHandler.class,
        BotProperties.class
})
@EnableConfigurationProperties(BotProperties.class)
@TestPropertySource(properties = {
        "bot.menu-buttons-per-line=1"
})
public class UpdateUserControlButtonsHandlerTest extends RegisteredUserHandlerTest {

    private static final String UPDATE_MESSAGE = "Меню было автоматически обновлено. Можно игнорировать это сообщение и продолжить ввод информации";

    @Test
    public void testForRegisteredUser() {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(111L);
        User user = new User();
        user.setId(1L);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/start");
        update.setMessage(message);

        ReplyKeyboardMarkup replyKeyboardMarkupTest = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton("Изменение списка клиентов"));
        keyboardRows.add(keyboardRow1);

        replyKeyboardMarkupTest.setKeyboard(keyboardRows);


        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setUsername(user.getUserName());


        List<PartialBotApiMethod<?>> result = updateUserControlButtonsHandler.handle(update);

        assertNotNull(result);
        assertEquals(SendMessage.class, result.get(0).getClass());

        SendMessage sendMessage = (SendMessage) result.get(0);
        assertEquals("111", sendMessage.getChatId());
        assertEquals(UPDATE_MESSAGE, sendMessage.getText());

        ReplyKeyboardMarkup replyKeyboardMarkup = (ReplyKeyboardMarkup) sendMessage.getReplyMarkup();
        assertNotNull(replyKeyboardMarkup);
        assertNotNull(replyKeyboardMarkup.getKeyboard());
        assertEquals(keyboardRows, replyKeyboardMarkup.getKeyboard());


        replyKeyboardMarkup.getKeyboard().stream()
                .map(ArrayList::size)
                .forEach(size -> assertEquals(1, size));
    }

    @Test
    @Disabled("Нужно переписать тест. Сейчас тест не работает, потому что /start как будто и не должен подбирать, т.к. у незареганного нет кнопок. А определение зареганного делает сервис")
    public void testForUnregisteredUser(){
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(111L);
        User user = new User();
        user.setId(1L);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/start");
        update.setMessage(message);

        ReplyKeyboardMarkup replyKeyboardMarkupTest = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();

        keyboardRow.add(new KeyboardButton("Зарегистрироваться в боте"));
        keyboardRows.add(keyboardRow);
        replyKeyboardMarkupTest.setKeyboard(keyboardRows);

        when(client.getUserByUsername(any())).thenReturn(null);

        List<PartialBotApiMethod<?>> result = updateUserControlButtonsHandler.handle(update);

        assertNotNull(result);
        assertEquals(SendMessage.class, result.get(0).getClass());

        SendMessage sendMessage = (SendMessage) result.get(0);
        assertEquals("111", sendMessage.getChatId());
        assertEquals(UPDATE_MESSAGE, sendMessage.getText());

        ReplyKeyboardMarkup replyKeyboardMarkup = (ReplyKeyboardMarkup) sendMessage.getReplyMarkup();
        assertNotNull(replyKeyboardMarkup);
        assertNotNull(replyKeyboardMarkup.getKeyboard());
        assertEquals(keyboardRows, replyKeyboardMarkup.getKeyboard());

        replyKeyboardMarkup.getKeyboard().stream()
                .map(ArrayList::size)
                .forEach(size -> assertEquals(1, size));
    }
}
