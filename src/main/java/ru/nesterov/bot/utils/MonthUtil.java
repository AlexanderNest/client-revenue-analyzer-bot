package ru.nesterov.bot.utils;

import java.util.Calendar;

public class MonthUtil {
    public static int getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH);
    }
}
