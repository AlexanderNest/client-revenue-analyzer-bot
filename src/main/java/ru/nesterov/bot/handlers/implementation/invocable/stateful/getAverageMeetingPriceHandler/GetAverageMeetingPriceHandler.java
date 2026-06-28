package ru.nesterov.bot.handlers.implementation.invocable.stateful.getAverageMeetingPriceHandler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetClientScheduleRequest;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule.InlineCalendarBuilder;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Component
public class GetAverageMeetingPriceHandler extends StatefulCommandHandler<State, GetClientScheduleRequest> {
    private static final String START_TEXT = "Выберите дату *НАЧАЛА* периода:";
    private static final String END_TEXT = "Выберите дату *КОНЦА* периода:";
    private final InlineCalendarBuilder calendarBuilder;

    public GetAverageMeetingPriceHandler(InlineCalendarBuilder calendarBuilder) {
        super(State.STARTED, GetClientScheduleRequest.class);
        this.calendarBuilder = calendarBuilder;
    }

    @Override
    public String getCommand() {
        return "Расчет среднего чека";
    }

    @Override
    protected void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.COMMAND_INPUT, State.SELECT_FIRST_DATE, this::askFirstDate)
                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_DATE, State.SELECT_SECOND_DATE, this::handleFirstDate)
                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_PREV, State.SELECT_FIRST_DATE, this::handlePrevMonth)
                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_NEXT, State.SELECT_FIRST_DATE, this::handleNextMonth)
                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_TODAY, State.SELECT_FIRST_DATE, this::handleToday)
                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_DATE, State.FINISH, this::calculateAndShow)
                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_PREV, State.SELECT_SECOND_DATE, this::handlePrevMonth)
                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_NEXT, State.SELECT_SECOND_DATE, this::handleNextMonth)
                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_TODAY, State.SELECT_SECOND_DATE, this::handleToday);
    }

    private List<BotApiMethod<?>> askFirstDate(Update update) {
        LocalDate now = LocalDate.now();
        getStateMachine(update).getMemory().setDisplayedMonth(now.withDayOfMonth(1));

        return List.of(SendMessage.builder()
                .chatId(TelegramUpdateUtils.getChatId(update))
                .text(START_TEXT)
                .parseMode("Markdown")
                .replyMarkup(calendarBuilder.createCalendarMarkup(now, getCommand(), buttonCallbackService))
                .build());
    }

    private List<BotApiMethod<?>> handleFirstDate(Update update) {
        ButtonCallback callback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());
        LocalDate startDate = LocalDate.parse(callback.getValue());

        getStateMachine(update).getMemory().setFirstDate(startDate);
        getStateMachine(update).getMemory().setDisplayedMonth(LocalDate.now
                ().withDayOfMonth(1));

        return renderCalendar(update, "Выбрано начало: `" + startDate + "`\n" + END_TEXT);
    }

    private List<BotApiMethod<?>> calculateAndShow(Update update) {
        ButtonCallback callback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());
        LocalDate endDate = LocalDate.parse(callback.getValue());
        LocalDate startDate = getStateMachine(update).getMemory().getFirstDate();

        Double avg = client.getAverageMeetingPrice(TelegramUpdateUtils.getUserId(update),
                startDate.atStartOfDay(), endDate.atTime(23, 59));

        String text = String.format(Locale.of("ru"),
                """
                        *Результат анализа*
                        
                        Период: `%s` — `%s`
                        Средний чек: *%.2f руб.*
                        """,
                startDate, endDate, avg);

        return List.of(EditMessageText.builder()
                .chatId(TelegramUpdateUtils.getChatId(update))
                .messageId(TelegramUpdateUtils.getMessageId(update))
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(null)
                .build());
    }

    private List<BotApiMethod<?>> handlePrevMonth(Update update) {
        LocalDate current = getStateMachine(update).getMemory().getDisplayedMonth();
        getStateMachine(update).getMemory().setDisplayedMonth(current.minusMonths(1));
        return refreshCalendar(update);
    }

    private List<BotApiMethod<?>> handleNextMonth(Update update) {
        LocalDate current = getStateMachine(update).getMemory().getDisplayedMonth();
        getStateMachine(update).getMemory().setDisplayedMonth(current.plusMonths(1));
        return refreshCalendar(update);
    }

    private List<BotApiMethod<?>> handleToday(Update update) {
        getStateMachine(update).getMemory().setDisplayedMonth(LocalDate.now
                ().withDayOfMonth(1));
        return refreshCalendar(update);
    }

    private List<BotApiMethod<?>> refreshCalendar(Update update) {
        String prompt = (getStateMachine(update).getMemory().getFirstDate() == null) ? START_TEXT : END_TEXT;
        return renderCalendar(update, prompt);
    }

    private List<BotApiMethod<?>> renderCalendar(Update update, String text) {
        LocalDate monthToDisplay = getStateMachine(update).getMemory().getDisplayedMonth();

        return List.of(EditMessageText.builder()
                .chatId(TelegramUpdateUtils.getChatId(update))
                .messageId(TelegramUpdateUtils.getMessageId(update))
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(calendarBuilder.createCalendarMarkup(monthToDisplay, getCommand(), buttonCallbackService))
                .build());
    }
}

