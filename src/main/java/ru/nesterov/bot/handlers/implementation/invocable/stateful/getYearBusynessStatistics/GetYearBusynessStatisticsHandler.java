package ru.nesterov.bot.handlers.implementation.invocable.stateful.getYearBusynessStatistics;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetYearBusynessStatisticsRequest;
import ru.nesterov.bot.dto.GetYearBusynessStatisticsResponse;
import ru.nesterov.bot.exception.UserFriendlyException;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Получение годового отчета за указанный год
 */

@Component
public class GetYearBusynessStatisticsHandler extends StatefulCommandHandler<State, GetYearBusynessStatisticsRequest> {

    public GetYearBusynessStatisticsHandler() {
        super(State.STARTED, GetYearBusynessStatisticsRequest.class);
    }

    @Override
    public void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.COMMAND_INPUT, State.WAITING_YEAR_INPUT, this::askForAYear)
                .addTransition(State.WAITING_YEAR_INPUT, Action.ANY_STRING, State.FINISH, this::handleYearInput);
    }

    private List<BotApiMethod<?>> askForAYear(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Введите год для расчета занятости");
    }

    private List<BotApiMethod<?>> handleYearInput(Update update) {
        int year;
        try {
            year = Integer.parseInt(update.getMessage().getText());
        } catch (NumberFormatException e) {
            throw new UserFriendlyException("Введите корректный год");
        }

        getStateMachine(update).getMemory().setYear(year);
        return sendYearStatistics(update);
    }

    @SneakyThrows
    private List<BotApiMethod<?>> sendYearStatistics(Update update) {
        GetYearBusynessStatisticsResponse response = client.getYearBusynessStatistics(TelegramUpdateUtils.getChatId(update),
                getStateMachine(update).getMemory().getYear());
        return getPlainSendMessage(update.getMessage().getChatId(), formatYearStatistics(response));
    }

    private String formatYearStatistics(GetYearBusynessStatisticsResponse response) {
        if (response.getMonths().isEmpty()) {
            return "📅 Встречи не запланированы";
        }

        String monthHours = response.getMonths().entrySet().stream()
                .map(monthStatistics -> {
                    String monthName = monthStatistics.getKey();
                    Double hours = monthStatistics.getValue();
                    return String.format(Locale.US, "%s: %.2f ч.", monthName, hours);
                })
                .collect(Collectors.joining("\n"));

        String dayHours = response.getDays().entrySet().stream()
                .map(dayStatistics -> {
                    String dayName = dayStatistics.getKey();
                    Double hours = dayStatistics.getValue();
                    return String.format(Locale.US, "%s: %.2f ч.", dayName, hours);
                })
                .collect(Collectors.joining("\n"));

        return "📊 Анализ занятости за год:\n\n" +
                "🗓️ Занятость по месяцам:\n" + monthHours + "\n\n" +
                "📅 Занятость по дням недели:\n" + dayHours;
    }

    @Override
    public String getCommand() {
        return "Анализ занятости за год";
    }
}

