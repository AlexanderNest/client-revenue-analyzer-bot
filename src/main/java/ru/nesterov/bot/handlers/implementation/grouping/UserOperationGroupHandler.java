package ru.nesterov.bot.handlers.implementation.grouping;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getYearBusynessStatistics.GetYearBusynessStatisticsHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.makeEventsBackupHandler.MakeEventsBackupHandler;

import java.util.List;

@Component
public class UserOperationGroupHandler extends GroupingCommandHandler {
    protected UserOperationGroupHandler(GetYearBusynessStatisticsHandler getYearBusynessStatisticsHandler,
                                        MakeEventsBackupHandler makeEventsBackupHandler) {
        super(List.of(getYearBusynessStatisticsHandler, makeEventsBackupHandler));
    }

    @Override
    public String getCommand() {
        return "Действия пользователя";
    }


    @Override
    public String getDescription() {
        return "Сервисные действия пользователя и бизнес статистика за год";
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
