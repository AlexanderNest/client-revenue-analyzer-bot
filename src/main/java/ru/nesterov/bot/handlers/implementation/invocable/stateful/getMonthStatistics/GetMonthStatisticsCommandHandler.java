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
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.text.NumberFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Получение месячного отчета для выбранного месяца
 */
@Component
public class GetMonthStatisticsCommandHandler extends StatefulCommandHandler<State, MonthRequest> {

    private static final Calendar CALENDAR = Calendar.getInstance();

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

        GetIncomeAnalysisForMonthResponse response = client.getIncomeAnalysisForMonth(userId, callback.getValue());

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

        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (Month month : Month.values()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(getLocalizedMonthAndMarkCurrent(month));
            ButtonCallback callback = new ButtonCallback();
            callback.setValue(month.name());
            callback.setCommand(getCommand());
            button.setCallbackData(objectMapper.writeValueAsString(callback));
            buttons.add(button);
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(splitList(buttons));
        message.setReplyMarkup(keyboardMarkup);

        return List.of(message);
    }

    private String getLocalizedMonthAndMarkCurrent(Month month) {
        final int monthNumber = CALENDAR.get(Calendar.MONTH) + 1;
        String localizedMonth = month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.of("ru"));
        return month.getValue() == monthNumber ? markSymbol + " " + localizedMonth : localizedMonth;
    }

    private <T> List<List<T>> splitList(List<T> list) {
        List<List<T>> lists = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 3) {
            lists.add(list.subList(i, Math.min(list.size(), i + 3)));
        }
        return lists;
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