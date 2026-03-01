package ru.nesterov.bot.handlers.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.CommandHandler;

@Component
@ConditionalOnProperty("bot.enabled")
public class MetricService {
    private final MeterRegistry meterRegistry;

    public MetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void countHandlerMetric (CommandHandler commandHandler) {
        String createClientHandlerCounter = "%s_call_counter".formatted(commandHandler.getClass().getSimpleName());
        this.meterRegistry.counter(createClientHandlerCounter).increment();
    }
}
