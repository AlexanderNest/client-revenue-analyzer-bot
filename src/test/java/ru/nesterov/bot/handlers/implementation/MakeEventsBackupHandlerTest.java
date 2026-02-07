package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
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
import ru.nesterov.bot.dto.MakeEventsBackupRequest;
import ru.nesterov.bot.dto.MakeEventsBackupResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.makeEventsBackupHandler.MakeEventsBackupHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        MakeEventsBackupHandler.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MakeEventsBackupHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private MakeEventsBackupHandler handler;
    
    private String command;
    private static final long CHAT_ID = 1L;
    private static final long USER_ID = 1L;
    
    @Value("${app.calendar.events.backup.dates-range-for-backup}")
    private int datesRangeForBackup;
    
    @Value("${app.calendar.events.backup.delay-between-manual-backups}")
    private int delayBetweenManualBackups;
    
    @BeforeEach
    public void setUp() {
        command = handler.getCommand();
    }

    @Test
    void handle() {
        int datesRangeForBackup = 7;

        MakeEventsBackupRequest request = MakeEventsBackupRequest.builder()
                .isEventsBackupMade(true)
                .build();

        LocalDateTime from = LocalDateTime.now().minusDays(datesRangeForBackup);
        LocalDateTime to = LocalDateTime.now().plusDays(datesRangeForBackup);

        MakeEventsBackupResponse response = MakeEventsBackupResponse.builder()
                .savedEventsCount(2)
                .isBackupMade(true)
                .from(from)
                .to(to)
                .build();

        when(client.makeEventsBackup(anyLong())).thenReturn(response);

        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        User user = new User();
        user.setId(USER_ID);

        Message message = new Message();
        message.setText(command);
        message.setChat(chat);
        message.setFrom(user);
        message.setMessageId(1);

        Update firstUpdate = new Update();
        firstUpdate.setMessage(message);

        List<BotApiMethod<?>> botApiMethod = handler.handle(firstUpdate);
        assertInstanceOf(SendMessage.class, botApiMethod.get(0));

        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);
        assertEquals("Выполнить резервное копирование событий?", sendMessage.getText());

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        List<List<InlineKeyboardButton>> yesNoKeyboard = inlineKeyboardMarkup.getKeyboard();
        assertEquals(1, yesNoKeyboard.size());

        String firstButtonText = yesNoKeyboard.get(0).get(0).getText();
        String secondButtonText = yesNoKeyboard.get(0).get(1).getText    ();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("1");

        Message callbackMessage = new Message();
        callbackMessage.setMessageId(1);
        callbackMessage.setChat(chat);
        callbackMessage.setFrom(user);

        ButtonCallback callback = new ButtonCallback();
        callback.setCommand(command);
        callback.setValue(request.getIsEventsBackupMade().toString());
        String callbackData = buttonCallbackService.getTelegramButtonCallbackString(callback);
        callbackQuery.setData(callbackData);
        callbackQuery.setMessage(callbackMessage);
        callbackQuery.setFrom(user);

        Update secondUpdate = new Update();
        secondUpdate.setCallbackQuery(callbackQuery);

        botApiMethod = handler.handle(secondUpdate);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText editMessageText = (EditMessageText) botApiMethod.get(0);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        assertEquals(
                String.format("Резервная копия событий (%d шт.) в период с %s по %s сохранена",
                        response.getSavedEventsCount(),
                        from.format(dateTimeFormatter),
                        to.format(dateTimeFormatter)),
                editMessageText.getText()
        );
    }

    @Test
    void handleAlreadyMadeBackup() {
        MakeEventsBackupRequest request = MakeEventsBackupRequest.builder()
                .isEventsBackupMade(true)
                .build();

        MakeEventsBackupResponse response = MakeEventsBackupResponse.builder()
                .isBackupMade(false)
                .cooldownMinutes(delayBetweenManualBackups)
                .build();

        when(client.makeEventsBackup(anyLong())).thenReturn(response);

        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        User user = new User();
        user.setId(USER_ID);

        Message message = new Message();
        message.setText(command);
        message.setChat(chat);
        message.setFrom(user);
        message.setMessageId(1);

        Update firstUpdate = new Update();
        firstUpdate.setMessage(message);

        List<BotApiMethod<?>> botApiMethod = handler.handle(firstUpdate);
        assertInstanceOf(SendMessage.class, botApiMethod.get(0));

        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);
        assertEquals("Выполнить резервное копирование событий?", sendMessage.getText());

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        List<List<InlineKeyboardButton>> yesNoKeyboard = inlineKeyboardMarkup.getKeyboard();
        assertEquals(1, yesNoKeyboard.size());

        String firstButtonText = yesNoKeyboard.get(0).get(0).getText();
        String secondButtonText = yesNoKeyboard.get(0).get(1).getText();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("1");

        Message callbackMessage = new Message();
        callbackMessage.setMessageId(1);
        callbackMessage.setChat(chat);
        callbackMessage.setFrom(user);
        callbackMessage.setText("Да");

        ButtonCallback callback = new ButtonCallback();
        callback.setCommand(command);
        callback.setValue(request.getIsEventsBackupMade().toString());
        String callbackData = buttonCallbackService.getTelegramButtonCallbackString(callback);
        callbackQuery.setData(callbackData);
        callbackQuery.setMessage(callbackMessage);
        callbackQuery.setFrom(user);

        Update secondUpdate = new Update();
        secondUpdate.setCallbackQuery(callbackQuery);

        botApiMethod = handler.handle(secondUpdate);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText editMessageText = (EditMessageText) botApiMethod.get(0);

        assertEquals(
                String.format("Выполнить резервное копирование событий возможно по прошествии %d минут(ы)", delayBetweenManualBackups),
                editMessageText.getText()
        );
    }

    @Test
    void handleWithoutBackup() {
        MakeEventsBackupRequest request = MakeEventsBackupRequest.builder()
                .isEventsBackupMade(false)
                .build();

        LocalDateTime from = LocalDateTime.now().minusDays(datesRangeForBackup);
        LocalDateTime to = LocalDateTime.now().plusDays(datesRangeForBackup);

        MakeEventsBackupResponse response = MakeEventsBackupResponse.builder()
                .savedEventsCount(2)
                .isBackupMade(request.getIsEventsBackupMade())
                .from(from)
                .to(to)
                .build();

        when(client.makeEventsBackup(anyLong())).thenReturn(response);

        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        User user = new User();
        user.setId(USER_ID);

        Message message = new Message();
        message.setText(command);
        message.setChat(chat);
        message.setFrom(user);
        message.setMessageId(1);

        Update firstUpdate = new Update();
        firstUpdate.setMessage(message);

        List<BotApiMethod<?>> botApiMethod = handler.handle(firstUpdate);
        assertInstanceOf(SendMessage.class, botApiMethod.get(0));

        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);
        assertEquals("Выполнить резервное копирование событий?", sendMessage.getText());

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        List<List<InlineKeyboardButton>> yesNoKeyboard = inlineKeyboardMarkup.getKeyboard();
        assertEquals(1, yesNoKeyboard.size());

        String firstButtonText = yesNoKeyboard.get(0).get(0).getText();
        String secondButtonText = yesNoKeyboard.get(0).get(1).getText();
        assertEquals("Да", firstButtonText);
        assertEquals("Нет", secondButtonText);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("1");

        Message callbackMessage = new Message();
        callbackMessage.setMessageId(1);
        callbackMessage.setChat(chat);
        callbackMessage.setFrom(user);
        callbackMessage.setText("Нет");

        ButtonCallback callback = new ButtonCallback();
        callback.setCommand(command);
        callback.setValue(request.getIsEventsBackupMade().toString());
        String callbackData = buttonCallbackService.getTelegramButtonCallbackString(callback);
        callbackQuery.setData(callbackData);
        callbackQuery.setMessage(callbackMessage);
        callbackQuery.setFrom(user);

        Update secondUpdate = new Update();
        secondUpdate.setCallbackQuery(callbackQuery);

        botApiMethod = handler.handle(secondUpdate);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText editMessageText = (EditMessageText) botApiMethod.get(0);

        assertEquals("Вы отказались от выполнения резервного копирования событий", editMessageText.getText());
    }
}
