package ru.nesterov.bot.handlers.implementation.invocable.stateful.deleteClient;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

@Component
public class DeleteClientHandler extends StatefulCommandHandler<State, DeleteClientDto> {

    public DeleteClientHandler() {
        super(State.STARTED, DeleteClientDto.class);
    }

    @Override
    public String getCommand() {
        return "Удалить клиента";
    }

    @Override
    protected void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.COMMAND_INPUT, State.SELECT_CLIENT, this::handleCommandInputAndSendClientNamesKeyboard)

                .addTransition(State.SELECT_CLIENT, Action.ANY_CALLBACK_INPUT, State.APPROVE_DELETING, this::handleClientNameAndRequestApprove)

                .addTransition(State.APPROVE_DELETING, Action.CALLBACK_TRUE, State.FINISH, this::handleDeleteClient)
                .addTransition(State.APPROVE_DELETING, Action.CALLBACK_FALSE, State.FINISH, this::handleDeleteCanceling);
    }

    @SneakyThrows
    public List<BotApiMethod<?>> handleCommandInputAndSendClientNamesKeyboard(Update update) {
        return getClientNamesKeyboard(update, "Выберите клиента для удаления:");
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleClientNameAndRequestApprove(Update update) {
        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());
        getStateMachine(update).getMemory().setClientName(buttonCallback.getValue());
        return getApproveKeyBoardMessage(update, "Подтвердите удаление");
    }

    private List<BotApiMethod<?>> handleDeleteClient(Update update) {
        client.deleteClient(TelegramUpdateUtils.getUserId(update), getStateMachine(update).getMemory().getClientName());
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), formatDeleteUserResponse(getStateMachine(update).getMemory().getClientName()));
    }

    private String formatDeleteUserResponse(String clientName) {
        return String.join(System.lineSeparator(),
                "Пользователь " + clientName + " успешно удален.");
    }

    private List<BotApiMethod<?>> handleDeleteCanceling(Update update) {
        return editMessage(TelegramUpdateUtils.getChatId(update), TelegramUpdateUtils.getMessageId(update), "Пользователь не удален.", null);
    }
}
