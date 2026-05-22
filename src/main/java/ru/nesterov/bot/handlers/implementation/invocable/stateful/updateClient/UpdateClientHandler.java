package ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

import static ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient.State.STARTED;

@Component
public class UpdateClientHandler extends StatefulCommandHandler<State, UpdateClientRequest> {
    public UpdateClientHandler() {
        super(STARTED, UpdateClientRequest.class);
    }

    @Override
    public String getCommand() {
        return "Обновить клиента";
    }

    @Override
    protected void initTransitions() {
        stateMachineProvider
                .addTransition(STARTED, Action.COMMAND_INPUT, State.SELECT_CLIENT, this::handleCommandInputAndSendClientNamesKeyboard)
                .addTransition(State.SELECT_CLIENT, Action.ANY_CALLBACK_INPUT, State.APPROVE_NAME_CHANGE, this::handleClientNameAndSendNameChangeApprove)

                .addTransition(State.APPROVE_NAME_CHANGE, Action.CALLBACK_FALSE, State.APPROVE_PRICE_CHANGE, this::handleChangeNameFalseAndSendPriceChangeApprove)
                .addTransition(State.APPROVE_NAME_CHANGE, Action.CALLBACK_TRUE, State.CHANGE_NAME, this::handleChangeNameTrueAndAskForNewName)
                .addTransition(State.CHANGE_NAME, Action.ANY_STRING, State.APPROVE_PRICE_CHANGE, this::handleNewClientNameInputAndSendPriceChangeApprove)

                .addTransition(State.APPROVE_PRICE_CHANGE, Action.CALLBACK_FALSE, State.APPROVE_DESCRIPTION_CHANGE, this::handleFalsePriceChangingAndAskForChangeDescription)
                .addTransition(State.APPROVE_PRICE_CHANGE, Action.CALLBACK_TRUE, State.CHANGE_PRICE, this::handleTruePriceChangingAndAskForNewPrice)
                .addTransition(State.CHANGE_PRICE, Action.ANY_STRING, State.APPROVE_DESCRIPTION_CHANGE, this::handleNewPriceInputAndSendDescriptionChangeApprove)

                .addTransition(State.APPROVE_DESCRIPTION_CHANGE, Action.CALLBACK_FALSE, State.APPROVE_PHONE_CHANGE, this::handleFalseDescriptionChangingAndSendUpdatePhoneApprove)
                .addTransition(State.APPROVE_DESCRIPTION_CHANGE, Action.CALLBACK_TRUE, State.CHANGE_DESCRIPTION, this::handleTrueDescriptionChangingAndAskForChangeDescription)
                .addTransition(State.CHANGE_DESCRIPTION, Action.ANY_STRING, State.APPROVE_PHONE_CHANGE, this::handleDescriptionInputAndAskForChangePhone)

                .addTransition(State.APPROVE_PHONE_CHANGE, Action.CALLBACK_FALSE, State.FINISH, this::handleFalsePhoneChangingAndAndUpdateClient)
                .addTransition(State.APPROVE_PHONE_CHANGE, Action.CALLBACK_TRUE, State.CHANGE_PHONE, this::handleTruePhoneChangingAndAskForNewPhone)
                .addTransition(State.CHANGE_PHONE, Action.ANY_STRING, State.FINISH, this::handleInputPhoneAndUpdateClient);
    }

    public List<BotApiMethod<?>> handleCommandInputAndSendClientNamesKeyboard(Update update) {
        return getClientNamesKeyboard(update, "Выберите клиента для обновления данных:");
    }

    private List<BotApiMethod<?>> handleClientNameAndSendNameChangeApprove(Update update) {
        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());
        getStateMachine(update).getMemory().setClientName(buttonCallback.getValue());
        return getApproveKeyBoardMessage(update, "Обновить имя клиента?");
    }

    private List<BotApiMethod<?>> handleChangeNameTrueAndAskForNewName(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите новое имя:");
    }

    private List<BotApiMethod<?>> handleNewClientNameInputAndSendPriceChangeApprove(Update update) {
        getStateMachine(update).getMemory().setNewName(update.getMessage().getText());
        return getApproveKeyBoardMessage(update, "Обновить стоимость за час клиента?");
    }

    private List<BotApiMethod<?>> handleChangeNameFalseAndSendPriceChangeApprove(Update update) {
        return editCurrentApproveKeyboardMessage(update, "Обновить стоимость за час клиента?");
    }

    private List<BotApiMethod<?>> handleTruePriceChangingAndAskForNewPrice(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите новую стоимость клиента за час: ");
    }

    private List<BotApiMethod<?>> handleNewPriceInputAndSendDescriptionChangeApprove(Update update) {
        getStateMachine(update).getMemory().setPricePerHour(Integer.parseInt(update.getMessage().getText()));
        return getApproveKeyBoardMessage(update, "Обновить описание клиента?");
    }

    private List<BotApiMethod<?>> handleFalsePriceChangingAndAskForChangeDescription(Update update) {
        return editCurrentApproveKeyboardMessage(update, "Обновить описание клиента?");
    }

    private List<BotApiMethod<?>> handleFalseDescriptionChangingAndSendUpdatePhoneApprove(Update update) {
        return editCurrentApproveKeyboardMessage(update, "Обновить номер телефона клиента?");
    }

    private List<BotApiMethod<?>> handleTrueDescriptionChangingAndAskForChangeDescription(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите новое описание клиента: ");
    }

    private List<BotApiMethod<?>> handleDescriptionInputAndAskForChangePhone(Update update) {
        getStateMachine(update).getMemory().setDescription(update.getMessage().getText());
        return getApproveKeyBoardMessage(update, "Обновить номер телефона клиента?");
    }

    private List<BotApiMethod<?>> handleFalsePhoneChangingAndAndUpdateClient(Update update) {
        UpdateClientResponse updateClientResponse = client.updateClient(TelegramUpdateUtils.getChatId(update), getStateMachine(update).getMemory());
        return editMessage(TelegramUpdateUtils.getChatId(update), TelegramUpdateUtils.getMessageId(update), formatUpdateClients(updateClientResponse), null);
    }

    private List<BotApiMethod<?>> handleTruePhoneChangingAndAskForNewPhone(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите новый номер телефона: ");
    }

    private List<BotApiMethod<?>> handleInputPhoneAndUpdateClient(Update update) {
        getStateMachine(update).getMemory().setPhone(update.getMessage().getText());
        UpdateClientResponse updateClientResponse = client.updateClient(TelegramUpdateUtils.getChatId(update), getStateMachine(update).getMemory());
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), formatUpdateClients(updateClientResponse));
    }

    private String formatUpdateClients(UpdateClientResponse response) {
        return String.join(
                System.lineSeparator(),
                "Клиент успешно обновлен!",
                "Имя: " + response.getName(),
                "Стоимость за час: " + response.getPricePerHour(),
                "Описание: " + response.getDescription(),
                "Номер телефона: " + response.getPhone()
        );
    }

    @Override
    public String getDescription() {
        return "Изменить данные существующего клиента";
    }
}
