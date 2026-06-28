package ru.nesterov.bot.handlers.abstractions;

import org.springframework.core.Ordered;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.dto.GetActiveClientResponse;
import ru.nesterov.bot.dto.Role;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Обработчик, который вызывается по отправленной команде
 */
public abstract class InvocableCommandHandler extends SendingMessageCommandHandler implements Ordered {
    public static final Comparator<InvocableCommandHandler> DEFAULT_COMPARATOR =
            Comparator.comparingInt(InvocableCommandHandler::getOrder)
                    .thenComparing(InvocableCommandHandler::getCommand);
    /**
     * Команда, которая вызовет обработчик
     */
    public abstract String getCommand();

    /**
     * краткое описание функционала обработчика для формирования справки пользователю
     */
    public abstract String getDescription();

    protected List<Role> getApplicableRoles() {
        return List.of(Role.USER);
    }

    public List<BotApiMethod<?>> getClientNamesKeyboard(Update update, String text) {
        List<GetActiveClientResponse> clients = client.getActiveClients(TelegramUpdateUtils.getUserId(update));
        if (clients.isEmpty()) {
            return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Нет доступных клиентов");
        }

        List<String> clientNamesKeyboard = clients.stream()
                .map(GetActiveClientResponse::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return getOneColumnInlineKeyboard(clientNamesKeyboard, update, text);
    }

    /**
     * Определяет порядок вывода обработчика
     */
    @Override
    public int getOrder() {
        return 5;
    }

    /**
     * @param text   - текст, который будет отображаться над списком
     * @param values - названия кнопок по порядку
     * @return - список из клиентов в виде inline keyboard
     */
    public List<BotApiMethod<?>> getOneColumnInlineKeyboard(List<String> values, Update update, String text) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (String value : values) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(value);
            ButtonCallback callback = new ButtonCallback();
            callback.setCommand(getCommand());
            callback.setValue(value);
            button.setCallbackData(buttonCallbackService.getTelegramButtonCallbackString(callback));

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            keyboard.add(rowInline);
        }
        keyboardMarkup.setKeyboard(keyboard);

        return getReplyKeyboard(TelegramUpdateUtils.getChatId(update), text, keyboardMarkup);
    }

    public List<BotApiMethod<?>> editCurrentApproveKeyboardMessage(Update update, String message) {
        return editMessage(
                TelegramUpdateUtils.getChatId(update),
                TelegramUpdateUtils.getMessageId(update),
                message,
                getApproveKeyboard()
        );
    }

    public List<BotApiMethod<?>> getApproveKeyBoardMessage(Update update, String message) {
        return getReplyKeyboard(TelegramUpdateUtils.getChatId(update), message, getApproveKeyboard());
    }

    public InlineKeyboardMarkup getApproveKeyboard() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(buildButton("Да", "true", getCommand()));
        rowInline.add(buildButton("Нет", "false", getCommand()));
        keyboard.add(rowInline);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    @Override
    public boolean isApplicable(Update update) {
        Message message = update.getMessage();

        boolean isCurrentHandlerCommand = message != null && getCommand().equals(message.getText());
        if (isCurrentHandlerCommand) {
            return true;
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();

        if (callbackQuery == null) {
            return false;
        }

        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(callbackQuery.getData());

        if (getCommand().equals(buttonCallback.getValue())) {
            return true;
        }

        boolean isShortButtonCallback = getCommand().equals(buttonCallback.getCommand());
        boolean isJsonButtonCallback = false;
        try {
            isJsonButtonCallback = getCommand().equals(objectMapper.readValue(callbackQuery.getData(), ButtonCallback.class).getCommand());
        } catch (Exception ignored) {

        }

        return isShortButtonCallback || isJsonButtonCallback;
    }
}
