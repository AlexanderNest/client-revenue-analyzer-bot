package ru.nesterov.bot.handlers.abstractions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.exception.UserFriendlyException;
import ru.nesterov.bot.statemachine.ActionService;
import ru.nesterov.bot.statemachine.StateMachine;
import ru.nesterov.bot.statemachine.StateMachineProvider;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.statemachine.dto.NextStateFunction;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Slf4j
public abstract class StatefulCommandHandler<STATE extends Enum<STATE>, MEMORY> extends InvocableCommandHandler {
    @Autowired
    protected ActionService actionService;

    protected final StateMachineProvider<STATE, MEMORY> stateMachineProvider;
    private final Class<MEMORY> memoryType;

    public StatefulCommandHandler(STATE state, Class<MEMORY> memoryType) {
        this.memoryType = memoryType;
        stateMachineProvider = new StateMachineProvider<>(state, memoryType);
        initTransitions();
    }

    /**
     * Инициализирует переходы между состояниями. Метод должен быть вызван в конструкторе.
     */
    protected abstract void initTransitions();

    public void resetState(long userId) {
        stateMachineProvider.removeMachine(userId);
    }

    /**
     * Определяет нужно ли сбрасывать обработчик для указанного пользователя.
     * Это полезно для тех случаев, когда один и тот же обработчик должен получить несколько сообщений подряд.
     *
     * @param userId
     * @return true - если надо сбросить обработчики для пользователя.
     * false - если надо, чтобы при следующем обновлении в чате вызвался тот же обработчик
     */
    public boolean isFinishedOrNotStarted(Long userId) {
        StateMachine<STATE, Action, MEMORY> stateMachine = stateMachineProvider.getMachine(userId);
        return stateMachine == null || "FINISH".equals(stateMachine.getCurrentState().name());
    }

    /**
     * Возвращает машину для указанного пользователя. Если машина для пользователя не существует, создает ее.
     */
    protected StateMachine<STATE, Action, MEMORY> getStateMachine(Update update) {
        long chatId = TelegramUpdateUtils.getChatId(update);
        StateMachine<STATE, Action, MEMORY> stateMachine = stateMachineProvider.getMachine(chatId);
        if (stateMachine == null) {
            try {
                stateMachine = stateMachineProvider.createMachine(
                        chatId,
                        memoryType.getDeclaredConstructor().newInstance(),
                        stateMachineProvider.getTransitions()
                );
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return stateMachine;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        StateMachine<STATE, Action, MEMORY> stateMachine = getStateMachine(update);
        List<Action> expectedActions = stateMachine.getExpectedActions();
        Action action = actionService.defineTheAction(getCommand(), update, expectedActions);

        NextStateFunction<STATE> nextStateFunction = stateMachine.getNextStateFunction(action);

        if (nextStateFunction == null) {
            throw new RuntimeException(
                    "Не найдена функция для перехода из состояния '%s' через action = '%s', из данных update = %s"
                            .formatted(stateMachine.getCurrentState(), action, update)
            );
        }

        List<BotApiMethod<?>> botApiMethod;
        try {
            botApiMethod = nextStateFunction.getFunctionForTransition().apply(update);
        } catch (UserFriendlyException ex) {
            return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), ex.getMessage());
        } catch (Exception ex) {
            log.error("Ошибка во время выполнения функции перехода", ex);
            return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Возникла ошибка. Обратитесь к разработчику");
        }

        stateMachine.applyNextState(action);
        return botApiMethod;
    }

    @Override
    public boolean isApplicable(Update update) {
        Message message = update.getMessage();
        boolean isPlainText = message != null && message.getText() != null;
        if (isPlainText && !isFinishedOrNotStarted(TelegramUpdateUtils.getChatId(update))) {
            return true;
        }

        return super.isApplicable(update);
    }
}
