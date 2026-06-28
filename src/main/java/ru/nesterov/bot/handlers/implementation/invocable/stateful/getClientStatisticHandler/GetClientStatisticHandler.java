package ru.nesterov.bot.handlers.implementation.invocable.stateful.getClientStatisticHandler;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetClientStatisticResponse;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.statemachine.dto.NoMemory;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Component
public class GetClientStatisticHandler extends StatefulCommandHandler<State, NoMemory> {
    public GetClientStatisticHandler() {
        super(State.STARTED, NoMemory.class);
    }

    @Override
    public void initTransitions() {
        stateMachineProvider
                .addTransition(State.STARTED, Action.COMMAND_INPUT, State.SELECT_CLIENT, this::handleCommandInputAndSendClientNamesKeyboard)

                .addTransition(State.SELECT_CLIENT, Action.ANY_CALLBACK_INPUT, State.FINISH, this::handleClientNameAndSendReport);
    }

    @Override
    public String getCommand() {
        return "Узнать статистику по клиенту";
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleClientNameAndSendReport(Update update) {
        long userId = update.getCallbackQuery().getFrom().getId();
        ButtonCallback buttonCallback = buttonCallbackService.buildButtonCallback(update.getCallbackQuery().getData());
        GetClientStatisticResponse response = client.getClientStatistic(userId, buttonCallback.getValue());

        if (response == null) {
            return getPlainSendMessage(
                    TelegramUpdateUtils.getChatId(update),
                    "У пользователя нет встреч"
            );
        }

        return editMessage(
                TelegramUpdateUtils.getChatId(update),
                TelegramUpdateUtils.getMessageId(update),
                formatIncomeReport(response),
                null
        );
    }

    @SneakyThrows
    private List<BotApiMethod<?>> handleCommandInputAndSendClientNamesKeyboard(Update update) {
        return getClientNamesKeyboard(update, "Выберите клиента, чью статистику хотите узнать:");
    }

    private static String formatIncomeReport(GetClientStatisticResponse response) {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        currencyFormat.setMinimumFractionDigits(0);
        currencyFormat.setMaximumFractionDigits(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru", "RU"));

        String successfulHours = String.format("%s часов", currencyFormat.format(response.getSuccessfulMeetingsHours()));
        String cancelledHours = String.format("%s часов", currencyFormat.format(response.getCancelledMeetingsHours()));
        String incomePerHour = String.format("%s ₽/час", currencyFormat.format(response.getIncomePerHour()));
        String successfulEvents = String.format("%s", currencyFormat.format(response.getSuccessfulEventsCount()));
        String plannedCancelled = String.format("%s", currencyFormat.format(response.getPlannedCancelledEventsCount()));
        String notPlannedCancelled = String.format("%s", currencyFormat.format(response.getNotPlannedCancelledEventsCount()));
        String totalIncome = String.format("%s ₽", currencyFormat.format(response.getTotalIncome()));

        return "📊 *Статистика клиента*\n\n" +
                String.format("%s %s", "Имя:", response.getName()) + "\n" +
                String.format("%s %s", "ID:", response.getId()) + "\n" +
                String.format("%s %s", "Телефон:", response.getPhone()) + "\n" +
                "─────────────────────────────" + "\n" +
                String.format("%s %s", "Описание:", response.getDescription()) + "\n" +
                String.format("%s %s", "Начало обучения:", dateFormat.format(response.getStartDate())) + "\n" +
                String.format("%s %s", "Продолжительность обучения:", response.getServiceDuration() + " месяцев") + "\n" +
                "─────────────────────────────" + "\n" +
                String.format("%s %s", "Состоявшихся занятий:", successfulHours) + "\n" +
                String.format("%s %s", "Отмененных занятий:", cancelledHours) + "\n" +
                String.format("%s %s", "Доход в час:", incomePerHour) + "\n" +
                String.format("%s %s", "Состоявшиеся занятия:", successfulEvents) + "\n" +
                String.format("%s %s", "Запланированные отмены:", plannedCancelled) + "\n" +
                String.format("%s %s", "Незапланированные отмены:", notPlannedCancelled) + "\n" +
                "─────────────────────────────" + "\n" +
                String.format("%s %s", "Суммарный доход:", totalIncome) + "\n";
    }

    @Override
    public String getDescription() {
        return "Глубокая аналитика по конкретному клиенту: отработанные часы, отмены и суммарный доход.";
    }
}
