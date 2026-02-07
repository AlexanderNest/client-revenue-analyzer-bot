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
import ru.nesterov.bot.dto.GetClientScheduleResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule.GetClientScheduleCommandHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        GetClientScheduleCommandHandler.class
})
public class GetClientScheduleCommandHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private GetClientScheduleCommandHandler handler;

    private static final String COMMAND = "Узнать расписание клиента";
    private static final String ENTER_FIRST_DATE = "Введите первую дату";
    private static final String ENTER_SECOND_DATE = "Введите вторую дату";

    @AfterEach
    public void resetHandler() {
        handler.resetState(1);
    }

    @Test
    void handleTodayButtonInput() {
        Update updateWithCommand = createUpdateWithCommand();
        handler.handle(updateWithCommand);

        ButtonCallback clientCallback = new ButtonCallback();
        clientCallback.setCommand("Узнать расписание клиента");
        clientCallback.setValue("Клиент 1");
        Update updateWithClientName = createUpdateWithCallbackQuery(clientCallback.getValue());
        handler.handle(updateWithClientName);

        ButtonCallback nextCallback = new ButtonCallback();
        nextCallback.setCommand("Узнать расписание клиента");
        nextCallback.setValue("Next");

        Update updateNext = createUpdateWithCallbackQuery(nextCallback.getValue());

        List<BotApiMethod<?>> botApiMethod = handler.handle(updateNext);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText text = (EditMessageText) botApiMethod.get(0);
        InlineKeyboardMarkup markupNext = text.getReplyMarkup();
        assertNotNull(markupNext);
        LocalDate expectedNextDate = LocalDate.now().plusMonths(1);
        assertCalendar(markupNext.getKeyboard(), expectedNextDate);

        ButtonCallback todayCallback = new ButtonCallback();
        todayCallback.setCommand("Узнать расписание клиента");
        todayCallback.setValue("Today");

        Update updateToday = createUpdateWithCallbackQuery(todayCallback.getValue());

        List<BotApiMethod<?>> botApiMethodToday = handler.handle(updateToday);
        assertInstanceOf(EditMessageText.class, botApiMethodToday.get(0));

        EditMessageText text1 = (EditMessageText) botApiMethodToday.get(0);
        InlineKeyboardMarkup markupToday = text1.getReplyMarkup();
        assertNotNull(markupToday);
        LocalDate expectedTodayDate = LocalDate.now();
        assertCalendar(markupToday.getKeyboard(), expectedTodayDate);
    }

    @Test
    void handleCommandWhenMessageContainsText() {
        List<GetActiveClientResponse> clients = createActiveClients();
        when(client.getActiveClients(anyLong())).thenReturn(clients);

        Update update = createUpdateWithCommand();

        List<BotApiMethod<?>> botApiMethod = handler.handle(update);
        assertInstanceOf(SendMessage.class, botApiMethod.get(0));

        SendMessage sendMessage = (SendMessage) botApiMethod.get(0);
        assertEquals("Выберите клиента, для которого хотите получить расписание:", sendMessage.getText());

        ReplyKeyboard markup = sendMessage.getReplyMarkup();
        assertInstanceOf(InlineKeyboardMarkup.class, markup);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) markup;
        List<List<InlineKeyboardButton>> clientNamesKeyboard = inlineKeyboardMarkup.getKeyboard();

        assertEquals(4, clientNamesKeyboard.size());

        for (int i = 0; i < clientNamesKeyboard.size(); i++) {
            InlineKeyboardButton button = clientNamesKeyboard.get(i).get(0);
            assertEquals("Клиент " + (i + 1), button.getText());
        }
    }

    @SneakyThrows
    @Test
    void handleClientNameShouldReturnCalendarKeyboard() {
        Update updateWithCommand = createUpdateWithCommand();
        handler.handle(updateWithCommand);
        Update updateWithClientName = createUpdateWithCallbackQuery("Клиент 1");

        List<BotApiMethod<?>> botApiMethod = handler.handle(updateWithClientName);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText editMessage = (EditMessageText) botApiMethod.get(0);
        assertEquals(ENTER_FIRST_DATE, editMessage.getText());

        InlineKeyboardMarkup markup = editMessage.getReplyMarkup();
        assertNotNull(markup);
        assertFalse(markup.getKeyboard().isEmpty());

        List<List<InlineKeyboardButton>> calendarKeyboard = markup.getKeyboard();
        LocalDate expectedDisplayedDate = LocalDate.now();
        assertCalendar(calendarKeyboard, expectedDisplayedDate);
    }

    @SneakyThrows
    @Test
    void handleFirstDateShouldReturnCalendarKeyboard() {
        Update updateWithCommand = createUpdateWithCommand();
        handler.handle(updateWithCommand);
        Update updateWithClientName = createUpdateWithCallbackQuery("Клиент 1");
        handler.handle(updateWithClientName);
        LocalDate firstDate = LocalDate.now();
        Update updateWithFirstDate = createUpdateWithCallbackQuery(String.valueOf(firstDate));

        List<BotApiMethod<?>> botApiMethod = handler.handle(updateWithFirstDate);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText editMessage = (EditMessageText) botApiMethod.get(0);
        assertEquals(ENTER_SECOND_DATE, editMessage.getText());

        InlineKeyboardMarkup markup = editMessage.getReplyMarkup();
        assertNotNull(markup);
        assertFalse(markup.getKeyboard().isEmpty());

        List<List<InlineKeyboardButton>> calendarKeyboard = markup.getKeyboard();
        assertCalendar(calendarKeyboard, firstDate);
    }

    @Test
    void handleSecondDateShouldReturnClientSchedule() {
        Update updateWithCommand = createUpdateWithCommand();
        handler.handle(updateWithCommand);
        Update updateWithClientName = createUpdateWithCallbackQuery("Клиент 1");
        handler.handle(updateWithClientName);
        LocalDate firstDate = LocalDate.now();
        Update updateWithFirstDate = createUpdateWithCallbackQuery(String.valueOf(firstDate));
        handler.handle(updateWithFirstDate);

        List<GetClientScheduleResponse> clientSchedule = new ArrayList<>();

        GetClientScheduleResponse schedule1 = new GetClientScheduleResponse();
        LocalDateTime eventStart1 = LocalDateTime.now();
        schedule1.setEventStart(eventStart1);
        schedule1.setEventEnd(eventStart1.plusHours(1L));
        schedule1.setRequiresShift(true);
        clientSchedule.add(schedule1);

        GetClientScheduleResponse schedule2 = new GetClientScheduleResponse();
        LocalDateTime eventStart2 = LocalDateTime.now().plusDays(1L);
        schedule2.setEventStart(eventStart2);
        schedule2.setEventEnd(eventStart2.plusHours(1L));
        clientSchedule.add(schedule2);

        GetClientScheduleResponse schedule3 = new GetClientScheduleResponse();
        LocalDateTime eventStart3 = LocalDateTime.now().plusDays(2L);
        schedule3.setEventStart(eventStart3);
        schedule3.setEventEnd(eventStart3.plusHours(1L));
        clientSchedule.add(schedule3);

        LocalDate secondDate = firstDate.plusMonths(1L);

        when(client.getClientSchedule(
                1L,
                "Клиент 1",
                firstDate.atStartOfDay(),
                secondDate.atStartOfDay().plusDays(1)
        )).thenReturn(clientSchedule);

        Update updateWithSecondDate = createUpdateWithCallbackQuery(String.valueOf(secondDate));
        List<BotApiMethod<?>> botApiMethod = handler.handle(updateWithSecondDate);

        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));
        EditMessageText editMessage = (EditMessageText) botApiMethod.get(0);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("ru"));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("ru"));

        String expectedText = String.join("\n\n",
                String.format(
                        "📅 Дата: %s\n⏰ Время: %s - %s\n%s",
                        LocalDateTime.now().format(dateFormatter),
                        LocalDateTime.now().format(timeFormatter),
                        LocalDateTime.now().plusHours(1).format(timeFormatter),
                        "⚠️ Требуется перенос"
                ),
                String.format(
                        "📅 Дата: %s\n⏰ Время: %s - %s",
                        LocalDateTime.now().plusDays(1).format(dateFormatter),
                        LocalDateTime.now().plusDays(1).format(timeFormatter),
                        LocalDateTime.now().plusDays(1).plusHours(1).format(timeFormatter)
                ),
                String.format(
                        "📅 Дата: %s\n⏰ Время: %s - %s",
                        LocalDateTime.now().plusDays(2).format(dateFormatter),
                        LocalDateTime.now().plusDays(2).format(timeFormatter),
                        LocalDateTime.now().plusDays(2).plusHours(1).format(timeFormatter)
                )
        );
        assertEquals(expectedText, editMessage.getText());
    }

    @Test
    void handleSwitchMonthWhenSelectedFirstDate1() {
        Update updateWithCommand = createUpdateWithCommand();
        handler.handle(updateWithCommand);
        Update updateWithClientName = createUpdateWithCallbackQuery("Клиент 1");
        handler.handle(updateWithClientName);

        Update update = createUpdateWithCallbackQuery("Prev");

        List<BotApiMethod<?>> botApiMethod = handler.handle(update);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText editMessage = (EditMessageText) botApiMethod.get(0);
        assertEquals(ENTER_FIRST_DATE, editMessage.getText());

        InlineKeyboardMarkup markup = editMessage.getReplyMarkup();
        assertNotNull(markup);
        assertFalse(markup.getKeyboard().isEmpty());

        LocalDate expectedDisplayedDate = LocalDate.now().minusMonths(1L);
        List<List<InlineKeyboardButton>> calendarKeyboard = markup.getKeyboard();
        assertCalendar(calendarKeyboard, expectedDisplayedDate);
    }

    @Test
    void handleSwitchMonthWhenSelectedFirstDate2() {
        Update updateWithCommand = createUpdateWithCommand();
        handler.handle(updateWithCommand);
        Update updateWithClientName = createUpdateWithCallbackQuery("Клиент 1");
        handler.handle(updateWithClientName);

        Update update = createUpdateWithCallbackQuery("Next");

        List<BotApiMethod<?>> botApiMethod = handler.handle(update);
        assertInstanceOf(EditMessageText.class, botApiMethod.get(0));

        EditMessageText editMessage = (EditMessageText) botApiMethod.get(0);
        assertEquals(ENTER_FIRST_DATE, editMessage.getText());

        InlineKeyboardMarkup markup = editMessage.getReplyMarkup();
        assertNotNull(markup);
        assertFalse(markup.getKeyboard().isEmpty());

        LocalDate expectedDisplayedDate = LocalDate.now().plusMonths(1L);
        List<List<InlineKeyboardButton>> calendarKeyboard = markup.getKeyboard();
        assertCalendar(calendarKeyboard, expectedDisplayedDate);
    }

    private void assertCalendar(List<List<InlineKeyboardButton>> calendarKeyboard, LocalDate displayedDate) {
        String month = displayedDate.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru")).toUpperCase();
        String year = String.valueOf(displayedDate.getYear());

        int daysInMonth = displayedDate.lengthOfMonth();

        List<InlineKeyboardButton> headerRow = calendarKeyboard.get(0);
        assertEquals(3, headerRow.size());
        assertEquals("◀", headerRow.get(0).getText());
        assertEquals(month + " " + year, headerRow.get(1).getText());
        assertEquals("▶", headerRow.get(2).getText());

        List<InlineKeyboardButton> daysOfWeekRow = calendarKeyboard.get(1);
        assertDaysOfWeek(daysOfWeekRow);

        for (int i = 2; i < calendarKeyboard.size(); i++) {
            for (InlineKeyboardButton dayButton : calendarKeyboard.get(i)) {
                if (!dayButton.getText().equals(" ")) {
                    int day = Integer.parseInt(dayButton.getText());
                    assertTrue(day >= 1 && day <= daysInMonth);
                }
            }
        }
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
    void handleCommandInputAndSendClientsShouldReturnSortedClients() {

        GetActiveClientResponse client1 = new GetActiveClientResponse();
        client1.setName("алексей");

        GetActiveClientResponse client2 = new GetActiveClientResponse();
        client2.setName("Яна");

        GetActiveClientResponse client3 = new GetActiveClientResponse();
        client3.setName("Андрей");

        GetActiveClientResponse client4 = new GetActiveClientResponse();
        client4.setName("борис");

        GetActiveClientResponse client5 = new GetActiveClientResponse();
        client5.setName("Борис");

        List<GetActiveClientResponse> unsortedClients = new ArrayList<>(List.of(client1, client2, client3, client4, client5));

        when(client.getActiveClients(anyLong())).thenReturn(unsortedClients);

        Update update = createUpdateWithCommand();
        List<BotApiMethod<?>> result = handler.handle(update);

        assertInstanceOf(SendMessage.class, result.get(0));
        SendMessage sendMessage = (SendMessage) result.get(0);

        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) sendMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();

        assertEquals(5, keyboard.size());

        assertEquals("алексей", keyboard.get(0).get(0).getText());
        assertEquals("Андрей", keyboard.get(1).get(0).getText());
        assertEquals("борис", keyboard.get(2).get(0).getText());
        assertEquals("Борис", keyboard.get(3).get(0).getText());
        assertEquals("Яна", keyboard.get(4).get(0).getText());
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

    private void assertDaysOfWeek(List<InlineKeyboardButton> daysOfWeekRow) {
        assertEquals(7, daysOfWeekRow.size());
        assertEquals("ПН", daysOfWeekRow.get(0).getText());
        assertEquals("ВТ", daysOfWeekRow.get(1).getText());
        assertEquals("СР", daysOfWeekRow.get(2).getText());
        assertEquals("ЧТ", daysOfWeekRow.get(3).getText());
        assertEquals("ПТ", daysOfWeekRow.get(4).getText());
        assertEquals("СБ", daysOfWeekRow.get(5).getText());
        assertEquals("ВС", daysOfWeekRow.get(6).getText());
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
