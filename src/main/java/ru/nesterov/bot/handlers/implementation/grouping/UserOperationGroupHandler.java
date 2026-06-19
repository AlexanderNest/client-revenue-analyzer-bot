package ru.nesterov.bot.handlers.implementation.grouping;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.GetUserInfoHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getYearBusynessStatistics.GetYearBusynessStatisticsHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.makeEventsBackupHandler.MakeEventsBackupHandler;

import java.util.List;

@Component
public class UserOperationGroupHandler extends GroupingCommandHandler {
    protected UserOperationGroupHandler(GetYearBusynessStatisticsHandler getYearBusynessStatisticsHandler,
                                        MakeEventsBackupHandler makeEventsBackupHandler, GetUserInfoHandler getUserInfoHandler) {
        super(List.of(getYearBusynessStatisticsHandler, makeEventsBackupHandler, getUserInfoHandler));
    }

    @Override
    public String getCommand() {
        return "Действия пользователя";
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
