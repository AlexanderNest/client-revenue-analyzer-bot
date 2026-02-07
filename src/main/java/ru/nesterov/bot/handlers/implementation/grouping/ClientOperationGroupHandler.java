package ru.nesterov.bot.handlers.implementation.grouping;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createClient.CreateClientHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.deleteClient.DeleteClientHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient.UpdateClientHandler;

import java.util.List;

@Component
public class ClientOperationGroupHandler extends GroupingCommandHandler {

    protected ClientOperationGroupHandler(CreateClientHandler createClientHandler,
                                          DeleteClientHandler deleteClientHandler,
                                          UpdateClientHandler updateClientHandler) {

        super(List.of(createClientHandler, deleteClientHandler, updateClientHandler));
    }

    @Override
    public String getCommand() {
        return "Изменение списка клиентов";
    }
}
