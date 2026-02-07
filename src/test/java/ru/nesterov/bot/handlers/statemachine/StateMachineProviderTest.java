package ru.nesterov.bot.handlers.statemachine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.nesterov.bot.statemachine.StateMachine;
import ru.nesterov.bot.statemachine.StateMachineProvider;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.statemachine.dto.NextStateFunction;

import java.util.List;

public class StateMachineProviderTest {

    private StateMachineProvider<StateTest, MemoryTest> stateMachineProvider;

    @BeforeEach
    void start() {
        stateMachineProvider = new StateMachineProvider<>(StateTest.STARTED, MemoryTest.class);
    }

    //TODO добавить тест если машины нет

    @Test
    void createAndGetMachineTest() {
        stateMachineProvider.addTransition(StateTest.STARTED, Action.COMMAND_INPUT, StateTest.WAITING_INPUT, update -> List.of(new SendMessage("1", "Текст")));

        StateMachine<StateTest, Action, MemoryTest> stateMachine = stateMachineProvider.createMachine(1L, new MemoryTest(), stateMachineProvider.getTransitions());

        Assertions.assertNotNull(stateMachine);
        Assertions.assertEquals(StateTest.STARTED, stateMachine.getCurrentState());

        StateMachine<StateTest, Action, MemoryTest> savedMachine = stateMachineProvider.getMachine(1L);
        Assertions.assertSame(stateMachine, savedMachine);

        NextStateFunction<StateTest> nextStateFunction = stateMachine.getNextStateFunction(Action.COMMAND_INPUT);
        Assertions.assertNotNull(nextStateFunction);
        Assertions.assertEquals(StateTest.WAITING_INPUT, nextStateFunction.getState());

        List<BotApiMethod<?>> response = nextStateFunction.getFunctionForTransition().apply(null);
        Assertions.assertInstanceOf(SendMessage.class, response.get(0));
        Assertions.assertEquals("Текст", ((SendMessage) response.get(0)).getText());
    }

    @Test
    void removeMachineTest() {
        stateMachineProvider.createMachine(1L, new MemoryTest(), stateMachineProvider.getTransitions());
        stateMachineProvider.removeMachine(1L);
        Assertions.assertNull(stateMachineProvider.getMachine(1L));
    }
}
