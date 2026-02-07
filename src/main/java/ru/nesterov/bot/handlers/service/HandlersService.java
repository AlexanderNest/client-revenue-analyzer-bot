package ru.nesterov.bot.handlers.service;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.handlers.abstractions.CommandHandler;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.handlers.abstractions.Priority;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.implementation.UndefinedHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createUser.CreateUserHandler;
import ru.nesterov.bot.integration.ClientRevenueAnalyzerIntegrationClient;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
@Slf4j
public class HandlersService {
    private final List<CommandHandler> highestPriorityCommandHandlers;
    private final List<CommandHandler> normalPriorityCommandHandlers;
    private final List<CommandHandler> lowestPriorityCommandHandlers;

    private final UndefinedHandler undefinedHandler;
    private final CreateUserHandler createUserHandler;
    private final ClientRevenueAnalyzerIntegrationClient integrationClient;

    private final List<StatefulCommandHandler<?, ?>> statefulCommandHandlers;
    private final List<InvocableCommandHandler> invocableCommandHandlers;

    private final List<CommandHandler> commandHandlers;

    public HandlersService(List<CommandHandler> commandHandlers,
                           List<StatefulCommandHandler<?, ?>> statefulCommandHandlers,
                           UndefinedHandler undefinedHandler,
                           CreateUserHandler createUserHandler, ClientRevenueAnalyzerIntegrationClient integrationClient,
                           List<InvocableCommandHandler> invocableCommandHandlers) {
        this.commandHandlers = commandHandlers;
        this.statefulCommandHandlers = statefulCommandHandlers;
        this.integrationClient = integrationClient;
        this.invocableCommandHandlers = invocableCommandHandlers;
        this.undefinedHandler = undefinedHandler;
        this.createUserHandler = createUserHandler;

        highestPriorityCommandHandlers = commandHandlers.stream()
                .filter(ch -> ch.getPriority() == Priority.HIGHEST)
                .toList();

        normalPriorityCommandHandlers = commandHandlers.stream()
                .filter(ch -> ch.getPriority() == Priority.NORMAL)
                .toList();

        lowestPriorityCommandHandlers = commandHandlers.stream()
                .filter(ch -> ch.getPriority() == Priority.LOWEST)
                .toList();
    }

    @PostConstruct
    private void logHandlersInfo() {
        log.info("Зарегистрировано обработчиков: {}", commandHandlers.size());

        log.info("Обработчики с высоким приоритетом (HIGHEST):");
        logHandlerList(highestPriorityCommandHandlers);

        log.info("Обработчики с нормальным приоритетом (NORMAL):");
        logHandlerList(normalPriorityCommandHandlers);

        log.info("Обработчики с низким приоритетом (LOWEST):");
        logHandlerList(lowestPriorityCommandHandlers);

        log.info("Stateful обработчики:");
        logHandlerList(statefulCommandHandlers);

        log.info("Invocable обработчики:");
        logHandlerList(invocableCommandHandlers);
    }

    @Nullable
    public CommandHandler getHandler(Update update) {
        if (isUnregisteredUser(update)) {
            return createUserHandler;
        }

        if (isCommandUpdate(update)) {
            resetAllHandlers(TelegramUpdateUtils.getChatId(update));
        }

        CommandHandler started = getStartedHandler(update);
        if (started != null) {
            return started;
        }

        return Stream.of(highestPriorityCommandHandlers, normalPriorityCommandHandlers, lowestPriorityCommandHandlers)
                .map(handlers -> selectHandler(handlers, update))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Не удалось найти Handler для этого Update [{}]", update);
                    return undefinedHandler;
                });
    }

    private boolean isUnregisteredUser(Update update) {
        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setUsername(String.valueOf(TelegramUpdateUtils.getUserId(update)));

        return integrationClient.getUserByUsername(getUserRequest) == null;
    }

    public void resetBrokeHandler(CommandHandler commandHandler, long userId) {
        if (commandHandler instanceof StatefulCommandHandler<?, ?>) {
            ((StatefulCommandHandler<?, ?>) commandHandler).resetState(userId);
        }
    }

    public void resetFinishedHandlers(Long userId) {
        resetHandlers(userId, handler -> handler.isFinishedOrNotStarted(userId));
    }

    public void resetAllHandlers(Long userId) {
        resetHandlers(userId, handler -> true);
    }

    private void resetHandlers(Long userId, Predicate<StatefulCommandHandler<?, ?>> predicate) {
        statefulCommandHandlers.stream()
                .filter(predicate)
                .forEach(handler -> handler.resetState(userId));
    }

    private CommandHandler getStartedHandler(Update update) {
        long chatId = TelegramUpdateUtils.getChatId(update);

        return statefulCommandHandlers.stream()
                .filter(handler -> !handler.isFinishedOrNotStarted(chatId))
                .findFirst()
                .filter(handler -> handler.isApplicable(update))
                .orElse(null);
    }

    private CommandHandler selectHandler(List<CommandHandler> commandHandlers, Update update) {
        return commandHandlers.stream()
                .filter(handler -> handler.isApplicable(update))
                .findFirst()
                .orElse(null);
    }

    private boolean isCommandUpdate(Update update) {
        String text = update.getMessage() != null ? update.getMessage().getText() : null;
        return text != null
                && invocableCommandHandlers.stream().anyMatch(handler -> handler.getCommand().equals(text));
    }

    private void logHandlerList(List<? extends CommandHandler> handlers) {
        if (handlers.isEmpty()) {
            log.info("  - Нет обработчиков");
            return;
        }

        for (CommandHandler handler : handlers) {
            String name = handler.getClass().getSimpleName();
            String command = "";
            if (handler instanceof InvocableCommandHandler invocableHandler) {
                command = " (" + invocableHandler.getCommand() + ")";
            }
            log.info("  - {}{}{}", name, command, isStateful(handler) ? " [stateful]" : "");
        }
    }

    private boolean isStateful(CommandHandler handler) {
        return handler instanceof StatefulCommandHandler;
    }
}
