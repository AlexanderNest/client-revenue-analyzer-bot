package ru.nesterov.bot.statemachine;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.service.ButtonCallbackService;
import ru.nesterov.bot.statemachine.dto.Action;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ActionService {

    private final List<String> trueAliases = List.of("да", "true", "yes");
    private final List<String> falseAliases = List.of("нет", "false", "no");

    @Autowired
    @Lazy
    private ButtonCallbackService buttonCallbackService;

    public Action defineTheAction(String command, Update update, List<Action> expectedActions) {
        Action action = null;

        if (isCallbackInput(update)) {
            action = handleCallbackInput(command, update.getCallbackQuery().getData(), expectedActions);
        } else if (isMessageInput(update)) {
            action = handleTextInput(command, update.getMessage().getText(), expectedActions);
        }

        if (action == null) {
            throw new IllegalArgumentException("Cannot define the action");
        }

        return action;
    }

    private boolean isMessageInput(Update update) {
        return update.getMessage() != null && update.getMessage().getText() != null;
    }

    private boolean isCallbackInput(Update update) {
        return update.getCallbackQuery() != null && update.getCallbackQuery().getData() != null;
    }

    @Nullable
    private Action handleTextInput(String command, String text, List<Action> expectedActions) {
        if (text.equals(command) && expectedActions.contains(Action.COMMAND_INPUT)) {
            return Action.COMMAND_INPUT;
        }

        Boolean booleanValue = getBooleanValue(text);
        if (booleanValue != null) {
            if (expectedActions.contains(booleanValue ? Action.TRUE : Action.FALSE)) {
                return booleanValue ? Action.TRUE : Action.FALSE;
            }
            if (expectedActions.contains(Action.ANY_BOOLEAN)) {
                return Action.ANY_BOOLEAN;
            }
        }

        if (text.matches("[0-9]+") && expectedActions.contains(Action.ANY_NUMBER)) {
            return Action.ANY_NUMBER;
        }

        if (expectedActions.contains(Action.ANY_STRING)) {
            return Action.ANY_STRING;
        }

        return null;
    }

    @Nullable
    private Action handleCallbackInput(String command, String callbackData, List<Action> expectedActions) {
        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(callbackData);
        String value = buttonCallback.getValue();

        if (command.equals(value)) {
            return Action.COMMAND_INPUT;
        }

        Boolean booleanValue = getBooleanValue(value);
        if (booleanValue != null) {
            if (expectedActions.contains(booleanValue ? Action.CALLBACK_TRUE : Action.CALLBACK_FALSE)) {
                return booleanValue ? Action.CALLBACK_TRUE : Action.CALLBACK_FALSE;
            }
            if (expectedActions.contains(Action.ANY_CALLBACK_BOOLEAN)) {
                return Action.ANY_CALLBACK_BOOLEAN;
            }
        }

        if (value.equals("Prev") && expectedActions.contains(Action.CALLBACK_PREV)) {
            return Action.CALLBACK_PREV;
        }

        if (value.equals("Next") && expectedActions.contains(Action.CALLBACK_NEXT)) {
            return Action.CALLBACK_NEXT;
        }

        if (value.equals("Today") && expectedActions.contains(Action.CALLBACK_TODAY)) {
            return Action.CALLBACK_TODAY;
        }

        if (isValidDate(value)) {
            if (expectedActions.contains(Action.CALLBACK_DATE)) {
                return Action.CALLBACK_DATE;
            }
        }

        if (expectedActions.contains(Action.ANY_CALLBACK_INPUT)) {
            return Action.ANY_CALLBACK_INPUT;
        }

        return null;
    }

    private boolean isValidDate(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            LocalDate.parse(dateStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Nullable
    private Boolean getBooleanValue(String input) {
        input = input.toLowerCase();
        if (trueAliases.contains(input)) {
            return true;
        } else if (falseAliases.contains(input)) {
            return false;
        }

        return null;
    }
}