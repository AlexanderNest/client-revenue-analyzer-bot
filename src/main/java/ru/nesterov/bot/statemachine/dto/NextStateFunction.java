package ru.nesterov.bot.statemachine.dto;

import lombok.Value;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.function.Function;

@Value
public class NextStateFunction<STATE> {
    private final STATE state;
    private final Function<Update, List<BotApiMethod<?>>> functionForTransition;
}
