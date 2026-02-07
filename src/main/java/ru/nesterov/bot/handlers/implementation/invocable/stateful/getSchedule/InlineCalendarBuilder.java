package ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nesterov.bot.handlers.callback.ButtonCallback;
import ru.nesterov.bot.handlers.service.ButtonCallbackService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class InlineCalendarBuilder {
    private static final Map<DayOfWeek, String> shortDaysOfWeek = Map.of(
            DayOfWeek.MONDAY, "ПН",
            DayOfWeek.TUESDAY, "ВТ",
            DayOfWeek.WEDNESDAY, "СР",
            DayOfWeek.THURSDAY, "ЧТ",
            DayOfWeek.FRIDAY, "ПТ",
            DayOfWeek.SATURDAY, "СБ",
            DayOfWeek.SUNDAY, "ВС"
    );

    public InlineKeyboardMarkup createCalendarMarkup(LocalDate dateForDisplay, String command, ButtonCallbackService buttonCallbackService) {
        YearMonth yearMonth = YearMonth.of(dateForDisplay.getYear(), dateForDisplay.getMonth());
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        String displayedDate = dateForDisplay
                .getMonth()
                .getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"))
                .toUpperCase();
        rowsInline.add(getHeaderRow(displayedDate + " " + dateForDisplay.getYear(), command, buttonCallbackService));
        rowsInline.add(getDaysOfWeek());
        rowsInline.addAll(getDaysOfMonthRows(firstDayOfMonth, lastDayOfMonth, command, buttonCallbackService));

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rowsInline);

        return keyboardMarkup;
    }

    @SneakyThrows
    private List<InlineKeyboardButton> getHeaderRow(String text, String command, ButtonCallbackService buttonCallbackService) {
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        ButtonCallback prevCallback = new ButtonCallback();
        prevCallback.setCommand(command);
        prevCallback.setValue("Prev");

        ButtonCallback todayCallback = new ButtonCallback();
        todayCallback.setCommand(command);
        todayCallback.setValue("Today");

        ButtonCallback nextCallback = new ButtonCallback();
        nextCallback.setCommand(command);
        nextCallback.setValue("Next");

        headerRow.add(InlineKeyboardButton.builder()
                .text("◀")
                .callbackData(buttonCallbackService.getTelegramButtonCallbackString(prevCallback))
                .build());

        headerRow.add(InlineKeyboardButton.builder()
                .text(text)
                .callbackData(buttonCallbackService.getTelegramButtonCallbackString(todayCallback))
                .build());

        headerRow.add(InlineKeyboardButton.builder()
                .text("▶")
                .callbackData(buttonCallbackService.getTelegramButtonCallbackString(nextCallback))
                .build());

        return headerRow;
    }

    private List<InlineKeyboardButton> getDaysOfWeek() {
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            daysOfWeekRow.add(InlineKeyboardButton.builder()
                    .text(shortDaysOfWeek.get(dayOfWeek))
                    .callbackData("ignore")
                    .build());
        }
        return daysOfWeekRow;
    }

    @SneakyThrows
    private List<List<InlineKeyboardButton>> getDaysOfMonthRows(LocalDate firstDayOfMonth,
                                                                LocalDate lastDayOfMonth,
                                                                String command,
                                                                ButtonCallbackService buttonCallbackService) {
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        for (int i = 1; i < firstDayOfMonth.getDayOfWeek().getValue(); i++) {
            rowInline.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("ignore")
                    .build());
        }

        for (LocalDate day = firstDayOfMonth; !day.isAfter(lastDayOfMonth); day = day.plusDays(1)) {
            ButtonCallback dayCallback = new ButtonCallback();
            dayCallback.setCommand(command);
            dayCallback.setValue(day.toString());

            rowInline.add(InlineKeyboardButton.builder()
                    .text(String.valueOf(day.getDayOfMonth()))
                    .callbackData(buttonCallbackService.getTelegramButtonCallbackString(dayCallback))
                    .build());

            if (rowInline.size() == 7) {
                rowsInline.add(rowInline);
                rowInline = new ArrayList<>();
            }
        }

        if (!rowInline.isEmpty()) {
            while (rowInline.size() < 7) {
                rowInline.add(InlineKeyboardButton.builder()
                        .text(" ")
                        .callbackData("ignore")
                        .build());
            }
            rowsInline.add(rowInline);
        }

        return rowsInline;
    }
}