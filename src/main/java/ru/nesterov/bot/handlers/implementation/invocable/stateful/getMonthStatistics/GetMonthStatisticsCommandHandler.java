package ru.nesterov.bot.handlers.implementation.invocable.stateful.getMonthStatistics;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.dto.GetIncomeAnalysisForMonthResponse;
import ru.nesterov.bot.handlers.abstractions.StatefulCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.statemachine.dto.Action;
import ru.nesterov.bot.utils.MonthUtil;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Получение месячного отчета для выбранного месяца
 */
@Component
public class GetMonthStatisticsCommandHandler extends StatefulCommandHandler<State, MonthRequest> {
    private static final String[] months = {
            "Январь", "Февраль",
            "Март", "Апрель", "Май",
            "Июнь", "Июль", "Август",
            "Сентябрь", "Октябрь", "Ноябрь",
            "Декабрь"
    };

    private static final String markSymbol = "\u2B50";

    public GetMonthStatisticsCommandHandler() {
        super(State.STARTED, MonthRequest.class);
    }

    @Override
    protected void initTransitions() {
        stateMachineProvider.addTransition(State.STARTED, Action.COMMAND_INPUT, State.SELECT_MONTH, this::sendMonthKeyboard);
        stateMachineProvider.addTransition(State.SELECT_MONTH, Action.ANY_CALLBACK_INPUT, State.FINISH, this::sendMonthStatistics);
    }

    private static String formatIncomeReport(GetIncomeAnalysisForMonthResponse response) {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        currencyFormat.setMinimumFractionDigits(0);
        currencyFormat.setMaximumFractionDigits(0);


        return String.format(
                "📊 *Анализ доходов за месяц*\n\n" +
                        "%-22s %10s ₽\n" +
                        "%-22s %10s ₽\n" +
                        "-----------------------------\n" +
                        "%-22s %10s ₽\n" +
                        "-----------------------------\n" +
                        "%-22s %10s ₽\n" +
                        "%-22s %10s ₽",
                "Фактический доход:", currencyFormat.format(response.getActualIncome()),
                "Ожидаемый доход:", currencyFormat.format(response.getExpectedIncome()),
                "Потенциальный доход:", currencyFormat.format(response.getPotentialIncome()),
                "Потерянный доход:", currencyFormat.format(response.getLostIncome()),
                "Из них из-за праздников потеряно:", currencyFormat.format(response.getLostIncomeDueToHoliday())
        );
    }

    @SneakyThrows
    private List<BotApiMethod<?>> sendMonthStatistics(Update update) {
        long userId = update.getCallbackQuery().getFrom().getId();
        CallbackQuery callbackQuery = update.getCallbackQuery();
        ButtonCallback callback = objectMapper.readValue(callbackQuery.getData(), ButtonCallback.class);

        GetIncomeAnalysisForMonthResponse response = client.getIncomeAnalysisForMonth(userId, clearFromMark(callback.getValue()));

        return editMessage(
                callbackQuery.getMessage().getChatId(),
                callbackQuery.getMessage().getMessageId(),
                formatIncomeReport(response),
                null
        );
    }

    @SneakyThrows
    private List<BotApiMethod<?>> sendMonthKeyboard(Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(TelegramUpdateUtils.getChatId(update)));
        message.setText("Выберите месяц для анализа дохода:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        String[] monthsWithMark = getArrayWithCurrentMonthMark();
        for (int i = 0; i < monthsWithMark.length; i += 3) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int j = i; j < i + 3 && j < monthsWithMark.length; j++) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(monthsWithMark[j]);
                ButtonCallback callback = new ButtonCallback();
                callback.setValue(clearFromMark(monthsWithMark[j]));
                callback.setCommand(getCommand());
                button.setCallbackData(objectMapper.writeValueAsString(callback));
                row.add(button);
            }
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return List.of(message);
    }

    private String clearFromMark(String string) {
        return string.replace(markSymbol, "");
    }

    private String[] getArrayWithCurrentMonthMark() {
        String[] copy = Arrays.copyOf(months, months.length);

        int currentMonth = MonthUtil.getCurrentMonth();
        copy[currentMonth] = markSymbol + copy[currentMonth];

        return copy;
    }

    @Override
    public String getCommand() {
        return "Узнать доход";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}