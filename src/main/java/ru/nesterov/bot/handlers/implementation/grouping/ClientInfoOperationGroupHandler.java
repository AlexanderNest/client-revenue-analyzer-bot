package ru.nesterov.bot.handlers.implementation.grouping;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.AiAnalyzerHandler;
import ru.nesterov.bot.handlers.implementation.invocable.GetActiveClientsHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getClientStatisticHandler.GetClientStatisticHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule.GetClientScheduleCommandHandler;

import java.util.List;

@Component
public class ClientInfoOperationGroupHandler extends GroupingCommandHandler {
    protected ClientInfoOperationGroupHandler(AiAnalyzerHandler aiAnalyzerHandler,
                                              GetActiveClientsHandler getActiveClientsHandler,
                                              GetClientStatisticHandler getClientStatisticHandler,
                                              GetClientScheduleCommandHandler getClientScheduleCommandHandler) {
        super(List.of(
                        aiAnalyzerHandler,
                        getActiveClientsHandler,
                        getClientStatisticHandler,
                        getClientScheduleCommandHandler
                )
        );
    }

    @Override
    public String getCommand() {
        return "Информация о клиентах";
    }
}
