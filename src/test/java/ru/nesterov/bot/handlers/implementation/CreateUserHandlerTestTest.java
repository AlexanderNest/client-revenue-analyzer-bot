package ru.nesterov.bot.handlers.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import ru.nesterov.bot.dto.CreateUserRequest;
import ru.nesterov.bot.dto.CreateUserResponse;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.dto.GetUserResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createUser.CreateUserHandler;
import ru.nesterov.bot.handlers.service.ButtonCallbackService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        CreateUserHandler.class
})
public class CreateUserHandlerTestTest extends RegisteredUserHandlerTest {
    @Autowired
    private CreateUserHandler createUserHandler;
    @Autowired
    private ButtonCallbackService buttonCallbackService;

    private String COMMAND;

    @BeforeEach
    public void setUp() {
        COMMAND = createUserHandler.getCommand();
    }

    @Test
    void createUserWithEnabledCancelledCalendar() {
        Chat chat = new Chat();
        chat.setId(1L);

        User user = new User();
        user.setId(1L);

        CreateUserRequest request = CreateUserRequest.builder()
                .userIdentifier(user.getId().toString())
                .mainCalendarId("12345mc")
                .isCancelledCalendarEnabled(true)
                .cancelledCalendarId("12345cc")
                .build();

        CreateUserResponse createUserResponse = CreateUserResponse.builder()
                .userIdentifier(request.getUserIdentifier())
                .mainCalendarId(request.getMainCalendarId())
                .isCancelledCalendarEnabled(request.isCancelledCalendarEnabled())
                .cancelledCalendarId(request.getCancelledCalendarId())
                .build();

        when(client.createUser(any())).thenReturn(createUserResponse);

        Message message = new Message();
        message.setText(COMMAND);
        message.setFrom(user);
        message.setChat(chat);

        Update update = new Update();
        update.setMessage(message);

        when(client.getUserByUsername(any(GetUserRequest.class))).thenReturn(null);
        List<PartialBotApiMethod<?>> botApiMethod = createUserHandler.handle(update);

        assertInstanceOf(SendMessage.class, botApiMethod.get(0));


        SendMessage sendMessage0 = (SendMessage) botApiMethod.get(0);
        assertEquals("Для начала работы необходимо зарегистрироваться. Следуйте инструкциям ниже.", sendMessage0.getText());

        assertInstanceOf(SendMessage.class, botApiMethod.get(1));
        SendMessage sendMessage1 = (SendMessage) botApiMethod.get(1);
        assertEquals("Введите ID основного календаря:", sendMessage1.getText());

        message.setText(request.getMainCalendarId());
        update.setMessage(message);

        botApiMethod = createUserHandler.handle(update);
        sendMessage1 = (SendMessage) botApiMethod.get(0);
        assertEquals("Вы хотите сохранять информацию об отмененных мероприятиях с использованием второго календаря?", sendMessage1.getText());

        ReplyKeyboard markup = sendMessage1.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;

        List<List<InlineKeyboardButton>> keyboard = inlineKeyboardMarkup.getKeyboard();

        assertEquals(1, keyboard.size());

        String firstButtonText = keyboard.get(0).get(0).getText();
        String secondButtonText = keyboard.get(0).get(1).getText();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        message.setText("Да");

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId(String.valueOf(1));
        ButtonCallback callback = new ButtonCallback();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callback.setCommand(COMMAND);
        callback.setValue(String.valueOf(request.isCancelledCalendarEnabled()));
        String callbackData = buttonCallbackService.getTelegramButtonCallbackString(callback);
        callbackQuery.setData(callbackData);

        update.setCallbackQuery(callbackQuery);
        update.setMessage(null);

        botApiMethod = createUserHandler.handle(update);
        sendMessage1 = (SendMessage) botApiMethod.get(0);

        assertEquals("Введите ID календаря с отмененными мероприятиями:", sendMessage1.getText());

        message.setText(request.getCancelledCalendarId());
        update.setMessage(message);
        update.setCallbackQuery(null);

        botApiMethod = createUserHandler.handle(update);
        sendMessage1 = (SendMessage) botApiMethod.get(0);

        assertEquals(String.join(System.lineSeparator(),
                        "Вы успешно зарегистрированы! ",
                        "ID пользователя: " + createUserResponse.getUserIdentifier(),
                        "ID основного календаря: " + createUserResponse.getMainCalendarId(),
                        "ID календаря с отмененными мероприятиями: " + createUserResponse.getCancelledCalendarId()),
                sendMessage1.getText());
    }

    @Test
    @Disabled
    void createUserWithoutCancelledCalendar() throws JsonProcessingException {
        Chat chat = new Chat();
        chat.setId(3L);

        User user = new User();
        user.setId(3L);

        CreateUserRequest request = CreateUserRequest.builder()
                .userIdentifier(user.getId().toString())
                .mainCalendarId("12345mc")
                .isCancelledCalendarEnabled(false)
                .build();

        Message message = new Message();
        message.setText(COMMAND);
        message.setFrom(user);
        message.setChat(chat);

        Update update = new Update();
        update.setMessage(message);

        when(client.getUserByUsername(any(GetUserRequest.class))).thenReturn(null);
        List<PartialBotApiMethod<?>> botApiMethod = createUserHandler.handle(update);

        assertInstanceOf(SendMessage.class, botApiMethod.get(0));

        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);
        assertEquals("Введите ID основного календаря:", sendMessage.getText());

        message.setText(request.getMainCalendarId());
        update.setMessage(message);

        botApiMethod = createUserHandler.handle(update);
        sendMessage = (SendMessage) botApiMethod.get(0);
        assertEquals("Вы хотите сохранять информацию об отмененных мероприятиях с использованием второго календаря?", sendMessage.getText());

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;

        List<List<InlineKeyboardButton>> keyboard = inlineKeyboardMarkup.getKeyboard();

        String firstButtonText = keyboard.get(0).get(0).getText();
        String secondButtonText = keyboard.get(0).get(1).getText();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        message.setText("Нет");

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId(String.valueOf(3L));
        ButtonCallback callback = new ButtonCallback();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callback.setCommand(COMMAND);
        callback.setValue(String.valueOf(request.isCancelledCalendarEnabled()));
        callbackQuery.setData(objectMapper.writeValueAsString(callback));

        update.setCallbackQuery(callbackQuery);

        CreateUserResponse createUserResponse = CreateUserResponse.builder()
                .userIdentifier(request.getUserIdentifier())
                .mainCalendarId(request.getMainCalendarId())
                .isCancelledCalendarEnabled(request.isCancelledCalendarEnabled())
                .cancelledCalendarId(request.getCancelledCalendarId())
                .build();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .userIdentifier("3")
                .mainCalendarId("12345mc")
                .build();

        when(client.createUser(createUserRequest)).thenReturn(createUserResponse);

        botApiMethod = createUserHandler.handle(update);
        sendMessage = (SendMessage) botApiMethod.get(0);

        assertEquals(String.join(System.lineSeparator(),
                        "Вы успешно зарегистрированы! ",
                        "ID пользователя: " + createUserResponse.getUserIdentifier(),
                        "ID основного календаря: " + createUserResponse.getMainCalendarId(),
                        "ID календаря с отмененными мероприятиями: " + createUserResponse.getCancelledCalendarId()),
                sendMessage.getText());
    }

    @Test
    void tryToCreateTheSameUser() {
        Chat chat = new Chat();
        chat.setId(2L);

        User user = new User();
        user.setId(2L);

        Message message = new Message();
        message.setText(COMMAND);
        message.setFrom(user);
        message.setChat(chat);

        Update update = new Update();
        update.setMessage(message);

        GetUserResponse response = GetUserResponse.builder()
                .userId(user.getId())
                .isCancelledCalendarEnabled(false)
                .mainCalendarId("main")
                .build();

        when(client.getUserByUsername(any(GetUserRequest.class))).thenReturn(response);

        List<PartialBotApiMethod<?>> botApiMethod = createUserHandler.handle(update);

        assertInstanceOf(SendMessage.class, botApiMethod.get(0));

        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);
        assertEquals("Вы уже зарегистрированы и можете пользоваться функциями бота", sendMessage.getText());
    }
}


