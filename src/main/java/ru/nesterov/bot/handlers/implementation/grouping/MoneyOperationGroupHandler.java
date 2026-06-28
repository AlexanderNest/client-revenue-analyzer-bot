package ru.nesterov.bot.handlers.implementation.grouping;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.GetUnpaidEventsHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getAverageMeetingPriceHandler.GetAverageMeetingPriceHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getMonthStatistics.GetMonthStatisticsCommandHandler;

import java.util.List;

@Component
public class MoneyOperationGroupHandler extends GroupingCommandHandler {
    protected MoneyOperationGroupHandler(GetUnpaidEventsHandler getUnpaidEventsHandler,
                                         GetMonthStatisticsCommandHandler getMonthStatisticsCommandHandler,
                                         GetAverageMeetingPriceHandler averageMeetingPriceHandler) {
        super(List.of(getMonthStatisticsCommandHandler, getUnpaidEventsHandler, averageMeetingPriceHandler));
    }

    @Override
    public String getCommand() {
        return "Денежные операции";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
