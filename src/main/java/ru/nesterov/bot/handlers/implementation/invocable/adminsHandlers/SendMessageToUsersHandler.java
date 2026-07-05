package ru.nesterov.bot.handlers.implementation.invocable.adminsHandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetAllUsersByRoleAndSourceResponse;
import ru.nesterov.bot.dto.Role;
import ru.nesterov.bot.dto.SendMessageToUserRequest;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Запуск рассылки админом
 */
@Component
@Slf4j
public class SendMessageToUsersHandler extends StatefulCommandHandler<State, SendMessageToUserRequest> {

    public SendMessageToUsersHandler() {
        super(State.STARTED, SendMessageToUserRequest.class);
    }

    @Override
    public String getCommand() {
        return "Запустить рассылку";
    }

    @Override
    protected List<Role> getApplicableRoles() {
        return List.of(Role.ADMIN);
    }

    @Override
    public void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.COMMAND_INPUT, State.TEXT_INPUT, this::handleStartMessage)

                .addTransition(State.TEXT_INPUT, Action.ANY_STRING, State.WAITING_FOR_CONFIRMATION, this::handleTextInput)
                .addTransition(State.TEXT_INPUT, Action.ANY_STRING, State.WAITING_FOR_CONFIRMATION, this::handleTextInput)

                .addTransition(State.WAITING_FOR_CONFIRMATION, Action.CALLBACK_TRUE, State.TEXT_INPUT, this::handleUpdatedMessage)
                .addTransition(State.WAITING_FOR_CONFIRMATION, Action.CALLBACK_FALSE, State.FINISH, this::sendMessageToUsers);
    }

    public List<PartialBotApiMethod<?>> handleStartMessage(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите текст рассылки");
    }

    public List<PartialBotApiMethod<?>> handleUpdatedMessage(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите исправленный текст рассылки");
    }

    public List<PartialBotApiMethod<?>> handleTextInput(Update update) {
        getStateMachine(update).getMemory().setMessage(update.getMessage().getText());
        return getApproveKeyBoardMessage(update, "Редактировать сообщение?");
    }

    public List<PartialBotApiMethod<?>> sendMessageToUsers(Update update) {
        GetAllUsersByRoleAndSourceResponse response = client.getUsersIdByRoleAndSource(TelegramUpdateUtils.getChatId(update),
                Role.USER, "telegram");
        List<PartialBotApiMethod<?>> messages = new ArrayList<>();

        for (String user : response.getUserIds()) {
            try {
                messages.addAll(getPlainSendMessage(Long.parseLong(user), getStateMachine(update).getMemory().getMessage()));
            } catch (Exception exception) {
                log.error("Ошибка отправки сообщения");
            }
        }
        messages.addAll(getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Рассылка завершена"));
        return messages;
    }
}
