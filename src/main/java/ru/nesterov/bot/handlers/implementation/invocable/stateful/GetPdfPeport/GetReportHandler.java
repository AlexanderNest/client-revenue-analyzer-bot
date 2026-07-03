package ru.nesterov.bot.handlers.implementation.invocable.stateful.GetPdfPeport;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule.InlineCalendarBuilder;
import ru.nesterov.bot.statemachine.dto.Action;

@Component
public class GetReportHandler extends StatefulCommandHandler<State, GetPdfReportRequest> {

    private final InlineCalendarBuilder inlineCalendarBuilder;

    public GetReportHandler(InlineCalendarBuilder inlineCalendarBuilder) {
        super(State.STARTED, GetPdfReportRequest.class);
        this.inlineCalendarBuilder = inlineCalendarBuilder;
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
        return "";
    }
}
