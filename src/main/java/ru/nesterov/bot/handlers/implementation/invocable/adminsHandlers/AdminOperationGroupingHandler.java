package ru.nesterov.bot.handlers.implementation.invocable.adminsHandlers;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.dto.Role;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;

import java.util.List;

@Component
public class AdminOperationGroupingHandler extends GroupingCommandHandler {

    protected AdminOperationGroupingHandler(SendMessageToUsersHandler sendMessageToUsersHandler) {
        super(List.of(sendMessageToUsersHandler));
    }

    @Override
    protected List<Role> getApplicableRoles() {
        return List.of(Role.ADMIN);
    }

    @Override
    public String getCommand() {
        return "Администрирование";
    }
}
