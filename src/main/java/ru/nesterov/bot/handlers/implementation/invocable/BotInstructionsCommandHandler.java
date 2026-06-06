package ru.nesterov.bot.handlers.implementation.invocable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.config.BotInfoProperties;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.implementation.grouping.HelpGroupHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BotInstructionsCommandHandler extends InvocableCommandHandler {
    private static final String CATEGORY_PREFIX = "category_";

    private final List<InvocableCommandHandler> allHandlers;
    private final List<GroupingCommandHandler> allGroups;
    private final BotInfoProperties botInfoProperties;

    public BotInstructionsCommandHandler(@Lazy List<InvocableCommandHandler> allHandlers,
                                         @Lazy List<GroupingCommandHandler> allGroups,
                                         BotInfoProperties botInfoProperties) {
        this.allHandlers = allHandlers;
        this.allGroups = allGroups;
        this.botInfoProperties = botInfoProperties;
    }

    @Override
    public String getCommand() {
        return "Инструкция по работе с ботом";
    }

    @Override
    public String getDescription() {
        return "Показать справку по всем доступным функциям бота";
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update){
        long chatId = TelegramUpdateUtils.getChatId(update);
        if (update.hasCallbackQuery()) {
            String callbackValue = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData()).getValue();

            if ("back".equals(callbackValue)) {
                return editMessage(chatId,
                        TelegramUpdateUtils.getMessageId(update),
                        "Выберите интересующий Вас раздел:",
                        getGroupsKeyboard(update));
            }
            if (callbackValue.startsWith(CATEGORY_PREFIX)) {
                String groupName = callbackValue.substring(CATEGORY_PREFIX.length());
                return editMessage(chatId,
                        TelegramUpdateUtils.getMessageId(update),
                        getCategoryDescription(groupName),
                        getBackKeyboard());
            }
        }
        return getReplyKeyboard(chatId, "Привет! Я помогу тебе освоиться! Выбери интересующий раздел:", getGroupsKeyboard(update));
    }

    private InlineKeyboardMarkup getGroupsKeyboard(Update update) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        allGroups.stream()
                .filter(group -> group.isDisplayed(update))
                .filter(group -> !(group instanceof HelpGroupHandler))
                .filter(group -> !group.getCommand().startsWith("/"))
                .sorted(InvocableCommandHandler.DEFAULT_COMPARATOR)
                .forEach(group -> {
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText(group.getCommand());
                    ButtonCallback callback = new ButtonCallback();
                    callback.setCommand(getCommand());
                    callback.setValue(CATEGORY_PREFIX + group.getCommand());
                    inlineKeyboardButton.setCallbackData(buttonCallbackService.getTelegramButtonCallbackString(callback));

                    rows.add(List.of(inlineKeyboardButton));
                });
        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    private String getCategoryDescription(String groupName) {
        Map<String, InvocableCommandHandler> handlerMap = allHandlers.stream()
                .collect(Collectors.toMap(InvocableCommandHandler::getCommand, h -> h, (a, b) -> b));
        Optional<GroupingCommandHandler> groupOpt = allGroups.stream()
                .filter(g -> g.getCommand().equals(groupName))
                .findFirst();

        if (groupOpt.isEmpty()) {
            return "Раздел не найден";
        }

        GroupingCommandHandler group = groupOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(group.getCommand().toUpperCase()).append("*\n\n");

        group.getGroupedCommandHandlersNames().forEach(cmdName -> {
            InvocableCommandHandler h = handlerMap.get(cmdName);
            if (h != null && !h.getDescription().isBlank()) {
                sb.append("• `").append(h.getCommand()).append("` - ").append(h.getDescription()).append("\n\n");
            }
        });
        sb.append("\nЕсли возникнут вопросы, обращайтесь к ").append(botInfoProperties.getCreatorContact());
        return sb.toString();
    }

    private InlineKeyboardMarkup getBackKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад к разделам");

        ButtonCallback callback = new ButtonCallback();
        callback.setCommand(getCommand());
        callback.setValue("back");
        backButton.setCallbackData(buttonCallbackService.getTelegramButtonCallbackString(callback));

        markup.setKeyboard(List.of(List.of(backButton)));
        return markup;
    }
}
