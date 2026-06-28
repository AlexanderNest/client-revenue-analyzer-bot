package ru.nesterov.bot.handlers.implementation.invocable.stateful.createUser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.dto.CreateUserRequest;
import ru.nesterov.bot.dto.CreateUserResponse;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Процесс создания нового клиента для зарегистрированного пользователя
 */

@Component
@Slf4j
public class CreateUserHandler extends StatefulCommandHandler<State, CreateUserRequest> {

    public CreateUserHandler() {
        super(State.STARTED, CreateUserRequest.class);
    }

    @Override
    public void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.ANY_STRING, State.MAIN_CALENDAR_INPUT, this::handleRegisterCommand)
                .addTransition(State.MAIN_CALENDAR_INPUT, Action.ANY_STRING, State.CANCELLED_CALENDAR_ENABLED_QUESTION, this::handleMainCalendarInput)
                .addTransition(State.CANCELLED_CALENDAR_ENABLED_QUESTION, Action.CALLBACK_TRUE, State.CANCELLED_CALENDAR_ID_INPUT, this::handleCancelledCalendarEnabledInput)
                .addTransition(State.CANCELLED_CALENDAR_ENABLED_QUESTION, Action.CALLBACK_FALSE, State.FINISH, this::registerUser)
                .addTransition(State.CANCELLED_CALENDAR_ID_INPUT, Action.ANY_STRING, State.FINISH, this::handleCancelledCalendarIdInput);
    }

    private List<BotApiMethod<?>> handleRegisterCommand(Update update) {
        long chatId = TelegramUpdateUtils.getChatId(update);
        if (isUserExists(String.valueOf(chatId))) {
            return buildFirstMessage(chatId);
        } else {
            return getPlainSendMessage(chatId, "Вы уже зарегистрированы и можете пользоваться функциями бота");
        }
    }

    private List<BotApiMethod<?>> buildFirstMessage(long chatId) {
        return getPlainSendMessage(chatId,
                "Для начала работы необходимо зарегистрироваться. Следуйте инструкциям ниже.",
                "Введите ID основного календаря:"
        );
    }

    private List<BotApiMethod<?>> handleMainCalendarInput(Update update) {
        getStateMachine(update).getMemory().setUserIdentifier(String.valueOf(TelegramUpdateUtils.getChatId(update)));
        getStateMachine(update).getMemory().setMainCalendarId(update.getMessage().getText());

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(buildButton("Да", "true", getCommand()));
        rowInline.add(buildButton("Нет", "false", getCommand()));
        keyboard.add(rowInline);
        keyboardMarkup.setKeyboard(keyboard);

        return getReplyKeyboard(TelegramUpdateUtils.getChatId(update), "Вы хотите сохранять информацию " +
                "об отмененных мероприятиях с использованием второго календаря?", keyboardMarkup);
    }

    private List<BotApiMethod<?>> handleCancelledCalendarEnabledInput(Update update) {
        getStateMachine(update).getMemory().setCancelledCalendarEnabled(Boolean.parseBoolean(getButtonCallbackValue(update)));
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите ID календаря с отмененными мероприятиями:");
    }

    private List<BotApiMethod<?>> handleCancelledCalendarIdInput(Update update) {
        getStateMachine(update).getMemory().setCancelledCalendarId(update.getMessage().getText());

        return registerUser(update);
    }

    private boolean isUserExists(String userIdentifier) {
        GetUserRequest request = new GetUserRequest();
        request.setUsername(userIdentifier);

        return client.getUserByUsername(request) == null;
    }

    private List<BotApiMethod<?>> registerUser(Update update) {
        CreateUserRequest request = getStateMachine(update).getMemory();
        request.setSource("telegram");
        CreateUserResponse response = client.createUser(request);
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), formatCreateUserResponse(response));
    }

    private String formatCreateUserResponse(CreateUserResponse createUserResponse) {
        return String.join(System.lineSeparator(),
                "Вы успешно зарегистрированы! ",
                "ID пользователя: " + createUserResponse.getUserIdentifier(),
                "ID основного календаря: " + createUserResponse.getMainCalendarId(),
                "ID календаря с отмененными мероприятиями: " + createUserResponse.getCancelledCalendarId());
    }

    private String getButtonCallbackValue(Update update) {
        String telegramCallbackString = update.getCallbackQuery().getData();
        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(telegramCallbackString);

        return buttonCallback.getValue();
    }

    @Override
    public String getCommand() {
        return "Зарегистрироваться в боте";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public String getDescription() {
        return "Начальная настройка: привязка Google календарей для сбора статистики.";
    }
}
