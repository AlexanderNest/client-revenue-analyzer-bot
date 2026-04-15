package ru.nesterov.bot.handlers.implementation.invocable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.nesterov.bot.config.BotProperties;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.handlers.abstractions.Priority;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createUser.CreateUserHandler;
import ru.nesterov.bot.integration.ClientRevenueAnalyzerIntegrationClient;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Отображает кнопки для управления ботом
 */
@RequiredArgsConstructor
@Component
public class UpdateUserControlButtonsHandler extends InvocableCommandHandler {
    private static final String UPDATE_MESSAGE = "Меню было автоматически обновлено. " +
            "Можно игнорировать это сообщение и продолжить ввод информации";

    private final List<GroupingCommandHandler> sendingMessageCommandHandlers;
    private final BotProperties botProperties;
    private final ClientRevenueAnalyzerIntegrationClient analyzerIntegrationClient;
    private final CreateUserHandler createUserHandler;

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public Priority getPriority() {
        return Priority.HIGHEST;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        int buttonsPerLine = botProperties.getMenuButtonsPerLine();

        List<KeyboardRow> keyboardRows = buildKeyboardRows(update, buttonsPerLine);
        keyboardMarkup.setKeyboard(keyboardRows);
        return getReplyKeyboard(TelegramUpdateUtils.getChatId(update), UPDATE_MESSAGE, keyboardMarkup);

    }

    @Override
    public boolean isApplicable(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return false;
        }

        if (getCommand().equals(update.getMessage().getText())) {
            return true;
        }

        if (createUserHandler.getCommand().equals(update.getMessage().getText())) {
            return false;
        }

        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setUsername(String.valueOf(TelegramUpdateUtils.getUserId(update)));
        boolean userExists = analyzerIntegrationClient.getUserByUsername(getUserRequest) != null;
        return !userExists;
    }

    private List<KeyboardRow> buildKeyboardRows(Update update, int buttonsPerLine) {
        List<GroupingCommandHandler> relevantHandlers = getHandlersDisplayedForCurrentUser(update);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow currentRow = new KeyboardRow();

        for (int buttonNumber = 0; buttonNumber < relevantHandlers.size(); buttonNumber++) {
            GroupingCommandHandler handler = relevantHandlers.get(buttonNumber);
            currentRow.add(new KeyboardButton(handler.getCommand()));

            if ((buttonNumber + 1) % buttonsPerLine == 0) {
                keyboardRows.add(currentRow);
                currentRow = new KeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            keyboardRows.add(currentRow);
        }

        return keyboardRows;
    }

    private List<GroupingCommandHandler> getHandlersDisplayedForCurrentUser(Update update) {
        return sendingMessageCommandHandlers.stream()
                .filter(handler -> handler.isDisplayed(update))
                .collect(Collectors.toList());
    }
}
