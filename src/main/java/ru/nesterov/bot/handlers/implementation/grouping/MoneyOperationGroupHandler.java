package ru.nesterov.bot.handlers.implementation.grouping;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.GetUnpaidEventsHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getMonthStatistics.GetMonthStatisticsCommandHandler;

import java.util.List;

@Component
public class MoneyOperationGroupHandler extends GroupingCommandHandler {
    protected MoneyOperationGroupHandler(GetUnpaidEventsHandler getUnpaidEventsHandler,
                                         GetMonthStatisticsCommandHandler getMonthStatisticsCommandHandler) {
        super(List.of(getMonthStatisticsCommandHandler, getUnpaidEventsHandler));
    }

    @Override
    public String getCommand() {
        return "Финансовые отчеты и работа с оплатами";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
