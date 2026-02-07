package ru.nesterov.bot.dto;

import lombok.Data;

@Data
public class GetIncomeAnalysisForMonthResponse {
    private double actualIncome;
    private double expectedIncome;
    private double lostIncome;
    private double potentialIncome;
    private double lostIncomeDueToHoliday;
}
