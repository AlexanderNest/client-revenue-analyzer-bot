package ru.nesterov.bot.handlers.implementation.invocable.stateful.makeEventsBackupHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.dto.MakeEventsBackupRequest;
import ru.nesterov.bot.dto.MakeEventsBackupResponse;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MakeEventsBackupHandler extends StatefulCommandHandler<State, MakeEventsBackupRequest> {

    public MakeEventsBackupHandler() {
        super(State.STARTED, MakeEventsBackupRequest.class);
    }

    @Override
    public void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.COMMAND_INPUT, State.WAITING_FOR_CONFIRMATION, this::requestConfirmation)
                .addTransition(State.WAITING_FOR_CONFIRMATION, Action.CALLBACK_TRUE, State.FINISH, this::makeEventsBackup)
                .addTransition(State.WAITING_FOR_CONFIRMATION, Action.CALLBACK_FALSE, State.FINISH, this::getFinishMessageWithoutBackup);
    }

    private List<BotApiMethod<?>> requestConfirmation(Update update) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(buildButton("Да", "true", getCommand()));
        rowInline.add(buildButton("Нет", "false", getCommand()));
        keyboard.add(rowInline);
        keyboardMarkup.setKeyboard(keyboard);

        return getReplyKeyboard(
                TelegramUpdateUtils.getChatId(update),
                "Выполнить резервное копирование событий?",
                keyboardMarkup
        );
    }

    private List<BotApiMethod<?>> getFinishMessageWithoutBackup(Update update) {
        return editMessage(
                TelegramUpdateUtils.getChatId(update),
                TelegramUpdateUtils.getMessageId(update),
                "Вы отказались от выполнения резервного копирования событий",
                null);
    }

    private List<BotApiMethod<?>> makeEventsBackup(Update update) {

        MakeEventsBackupResponse response = client.makeEventsBackup(TelegramUpdateUtils.getChatId(update));

        String message;

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        if (response.getIsBackupMade()) {
            message = String.format(
                    "Резервная копия событий (%d шт.) в период с %s по %s сохранена",
                    response.getSavedEventsCount(),
                    response.getFrom().format(dateTimeFormatter),
                    response.getTo().format(dateTimeFormatter)
            );
        } else {
            message = String.format(
                    "Выполнить резервное копирование событий возможно по прошествии %d минут(ы)",
                    response.getCooldownMinutes()
            );
        }

        return editMessage(
                TelegramUpdateUtils.getChatId(update),
                TelegramUpdateUtils.getMessageId(update),
                message,
                null);
    }

    @Override
    public String getCommand() {
        return "Создать бэкап событий";
    }
}




