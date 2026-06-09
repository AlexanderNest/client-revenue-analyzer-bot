package ru.nesterov.bot.handlers.implementation.grouping;

import org.springframework.stereotype.Component;
import ru.nesterov.bot.dto.Role;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.BotInstructionsCommandHandler;

import java.util.List;

@Component
public class HelpGroupHandler extends GroupingCommandHandler {
    protected HelpGroupHandler(BotInstructionsCommandHandler botInstructionsCommandHandler) {
        super(List.of(botInstructionsCommandHandler));
    }

    @Override
    protected List<Role> getApplicableRoles() {
        return List.of(Role.ADMIN, Role.USER);
    }

    @Override
    public String getCommand() {
        return "Помощь";
    }

    @Override
    public String getDescription() {
        return "Справочник по всем функциям и возможностям бота";
    }
}
