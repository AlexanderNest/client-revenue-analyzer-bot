package ru.nesterov.bot.handlers.implementation.invocable.stateful.GetPdfReport;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.TelegramDocumentBuffer;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule.InlineCalendarBuilder;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@Component
public class GetPdfReportHandler extends StatefulCommandHandler<State, GetPdfReportRequest> {

    private static final String ENTER_FIRST_DATE = "Введите первую дату";
    private static final String ENTER_SECOND_DATE = "Введите вторую дату";
    private final InlineCalendarBuilder inlineCalendarBuilder;
    private final TelegramDocumentBuffer buffer;

    public GetPdfReportHandler(InlineCalendarBuilder inlineCalendarBuilder,
                               TelegramDocumentBuffer buffer) {
        super(State.STARTED, GetPdfReportRequest.class);
        this.inlineCalendarBuilder = inlineCalendarBuilder;
        this.buffer = buffer;
    }

    @Override
    protected void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.COMMAND_INPUT, State.SELECT_CLIENT, this::handleCommandInputAndSendClients)

                .addTransition(State.SELECT_CLIENT, Action.ANY_CALLBACK_INPUT, State.SELECT_FIRST_DATE, this::handleClientName)

                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_DATE, State.SELECT_SECOND_DATE, this::handleFirstDate)
                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_PREV, State.SELECT_FIRST_DATE, this::handleCallbackPrev)
                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_NEXT, State.SELECT_FIRST_DATE, this::handleCallbackNext)
                .addTransition(State.SELECT_FIRST_DATE, Action.CALLBACK_TODAY, State.SELECT_FIRST_DATE, this::handleCallbackToday)

                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_DATE, State.FINISH, this::handleSecondDate)
                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_PREV, State.SELECT_SECOND_DATE, this::handleCallbackPrev)
                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_NEXT, State.SELECT_SECOND_DATE, this::handleCallbackNext)
                .addTransition(State.SELECT_SECOND_DATE, Action.CALLBACK_TODAY, State.SELECT_SECOND_DATE, this::handleCallbackToday);
    }

    @Override
    public String getCommand() {
        return "Сформировать PDF-отчет";
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleCallbackPrev(Update update) {
        LocalDate displayedMonth = getStateMachine(update).getMemory().getDisplayedMonth().minusMonths(1);
        getStateMachine(update).getMemory().setDisplayedMonth(displayedMonth);
        return handleMonthSwitch(update);
    }

    private List<BotApiMethod<?>> handleClientName(Update update) {
        if (getStateMachine(update).getMemory().getClientName() == null) {
            ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());
            getStateMachine(update).getMemory().setClientName(buttonCallback.getValue());
        }
        getStateMachine(update).getMemory().setDisplayedMonth(LocalDate.now().withDayOfMonth(1));
        return sendCalendarKeyBoard(update, ENTER_FIRST_DATE, getStateMachine(update).getMemory().getDisplayedMonth());
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleFirstDate(Update update) {
        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());

        getStateMachine(update).getMemory().setFirstDate(LocalDate.parse(buttonCallback.getValue()));
        return sendCalendarKeyBoard(update, ENTER_SECOND_DATE, getStateMachine(update).getMemory().getFirstDate());
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleSecondDate(Update update) {
        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());

        getStateMachine(update).getMemory().setSecondDate(LocalDate.parse(buttonCallback.getValue()).plusDays(1));

        byte[] pdf = client.getClientPdfReport(
                TelegramUpdateUtils.getUserId(update),
                getStateMachine(update).getMemory().getClientName(),
                getStateMachine(update).getMemory().getFirstDate().atStartOfDay(),
                getStateMachine(update).getMemory().getSecondDate().atStartOfDay()
        );

        InputFile inputFile = new InputFile(
                new ByteArrayInputStream(pdf),
                "report_" + getStateMachine(update).getMemory().getClientName() + ".pdf"
        );

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(TelegramUpdateUtils.getChatId(update)));
        sendDocument.setDocument(inputFile);

        buffer.set(sendDocument);

        return getPlainSendMessage(
                TelegramUpdateUtils.getChatId(update),
                "PDF-отчёт отправлен"
        );
    }

    @SneakyThrows
    private List<BotApiMethod<?>> sendCalendarKeyBoard(Update update, String text, LocalDate date) {
        return editMessage(TelegramUpdateUtils.getChatId(update),
                TelegramUpdateUtils.getMessageId(update),
                text,
                inlineCalendarBuilder.createCalendarMarkup(date, getCommand(), buttonCallbackService)
        );
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleCommandInputAndSendClients(Update update) {
        return getClientNamesKeyboard(update, "Выберите клиента, для которого хотите получить PDF-отчет:");
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleCallbackToday(Update update) {
        LocalDate today = LocalDate.now();
        getStateMachine(update).getMemory().setDisplayedMonth(today.withDayOfMonth(1));
        return handleMonthSwitch(update);
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleCallbackNext(Update update) {
        LocalDate displayedMonth = getStateMachine(update).getMemory().getDisplayedMonth().plusMonths(1);
        getStateMachine(update).getMemory().setDisplayedMonth(displayedMonth);
        return handleMonthSwitch(update);
    }

    private List<BotApiMethod<?>> handleMonthSwitch(Update update) {
        String calendarMessage = "";
        if (getStateMachine(update).getMemory().getFirstDate() == null) {
            calendarMessage = ENTER_FIRST_DATE;
        } else if (getStateMachine(update).getMemory().getSecondDate() == null) {
            calendarMessage = ENTER_SECOND_DATE;
        }
        LocalDate firstDayOfMonth = getStateMachine(update).getMemory().getDisplayedMonth().withDayOfMonth(1);
        return sendCalendarKeyBoard(update, calendarMessage, firstDayOfMonth);
    }
}
