package ru.nesterov.bot.handlers.statemachine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.statemachine.StateMachine;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.statemachine.dto.NextStateFunction;

import java.util.List;
import java.util.function.Function;

public class StateMachineTest {

    private StateMachine<StateTest, Action, MemoryTest> stateMachine;

    @BeforeEach
    void start() {
        stateMachine = new StateMachine<>(StateTest.STARTED, new MemoryTest());
    }

    @Test
    void test() {
        Function<Update, List<BotApiMethod<?>>> functionForTransition = update -> List.of(new SendMessage("1", "Текст"));
        stateMachine.addTransition(StateTest.STARTED, Action.COMMAND_INPUT, StateTest.WAITING_INPUT, functionForTransition);
        Assertions.assertEquals(StateTest.STARTED, stateMachine.getCurrentState());

        NextStateFunction<StateTest> nextStateFunction = stateMachine.getNextStateFunction(Action.COMMAND_INPUT);
        Assertions.assertNotNull(nextStateFunction);
        Assertions.assertEquals(StateTest.WAITING_INPUT, nextStateFunction.getState());

        List<BotApiMethod<?>> response = nextStateFunction.getFunctionForTransition().apply(null);
        Assertions.assertInstanceOf(SendMessage.class, response.get(0));
        Assertions.assertEquals("Текст", ((SendMessage) response.get(0)).getText());

        stateMachine.applyNextState(Action.COMMAND_INPUT);
        Assertions.assertEquals(StateTest.WAITING_INPUT, stateMachine.getCurrentState());
    }
}
