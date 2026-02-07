package ru.nesterov.bot.handlers.abstractions;

import org.springframework.core.Ordered;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.dto.GetUserResponse;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

/**
 * Обработчик, который будет отображаться в списке команд для отправки на стороне пользователя. Он же отвечает за группу команд
 */
public abstract class GroupingCommandHandler extends InvocableCommandHandler {

    private final List<String> groupedCommandHandlersNames;

    protected GroupingCommandHandler(List<InvocableCommandHandler> groupedCommandHandler) {
        this.groupedCommandHandlersNames = groupedCommandHandler.stream()
                .map(InvocableCommandHandler::getCommand)
                .toList();
    }

    /**
     * Определяет, будет ли обработчик отображаться для текущего update
     */
    public boolean isDisplayed(Update update) {
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setUsername(String.valueOf(TelegramUpdateUtils.getUserId(update)));
        GetUserResponse response = client.getUserByUsername(getUserRequest);
        if (response != null) {
            return isDisplayedForRole(response);
        }

        return false;
    }

    private boolean isDisplayedForRole(GetUserResponse response) {
        return getApplicableRoles().contains(response.getRole());
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        return getOneColumnInlineKeyboard(groupedCommandHandlersNames, update, getCommand());
    }

    @Override
    public boolean isApplicable(Update update) {
        return update.hasMessage()
                && getCommand().equals(update.getMessage().getText());
    }
}
