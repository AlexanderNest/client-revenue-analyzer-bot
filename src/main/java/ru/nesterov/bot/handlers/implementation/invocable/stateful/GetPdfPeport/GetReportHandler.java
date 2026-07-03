package ru.nesterov.bot.handlers.implementation.invocable.stateful.GetPdfPeport;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule.InlineCalendarBuilder;

@Component
public class GetReportHandler extends StatefulCommandHandler<State, GetPdfReportRequest> {

    private final InlineCalendarBuilder inlineCalendarBuilder;

    public GetReportHandler(InlineCalendarBuilder inlineCalendarBuilder) {
        super(State.STARTED, GetPdfReportRequest.class);
        this.inlineCalendarBuilder = inlineCalendarBuilder;
    }

    @Override
    protected void initTransitions() {

    }

    @Override
    public String getCommand() {
        return "";
    }
}
