package ru.nesterov.bot.statemachine;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.statemachine.dto.NextStateFunction;
import ru.nesterov.bot.statemachine.dto.TransitionDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Getter
@Setter
public class StateMachineProvider<STATE extends Enum<STATE>, MEMORY> {

    private final Map<Long, StateMachine<STATE, Action, MEMORY>> userMachines = new ConcurrentHashMap<>();
    private final STATE initialState;
    private final Class<MEMORY> memory;
    private final Map<TransitionDescription<STATE>, NextStateFunction<STATE>> transitions = new HashMap<>();

    public StateMachineProvider(STATE initialState, Class<MEMORY> memory) {
        this.initialState = initialState;
        this.memory = memory;
    }

    public StateMachine<STATE, Action, MEMORY> getMachine(Long userId) {
        return userMachines.get(userId);
    }

    public StateMachine<STATE, Action, MEMORY> createMachine(Long userId, MEMORY memory, Map<TransitionDescription<STATE>, NextStateFunction<STATE>> transitions) {
        StateMachine<STATE, Action, MEMORY> stateMachine = new StateMachine<>(initialState, memory);
        transitions.forEach(
                (transitionDescription, nextStateFunction) ->
                        stateMachine.addTransition(
                                transitionDescription.getState(),
                                transitionDescription.getAction(),
                                nextStateFunction.getState(),
                                nextStateFunction.getFunctionForTransition()
                        )
        );
        userMachines.put(userId, stateMachine);
        return stateMachine;
    }

    public void removeMachine(Long userId) {
        userMachines.remove(userId);
    }

    public StateMachineProvider<STATE, MEMORY> addTransition(STATE state, Action actionForTransition, STATE nextState, Function<Update, List<BotApiMethod<?>>> functionForTransition) {
        transitions.put(new TransitionDescription<>(state, actionForTransition), new NextStateFunction<>(nextState, functionForTransition));
        return this;
    }
}
