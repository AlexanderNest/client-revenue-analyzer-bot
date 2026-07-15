package ru.nesterov.bot.handlers.implementation.invocable.stateful.GetPdfReport;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GetPdfReportRequest {
    private String clientName;
    private LocalDate displayedMonth;
    private LocalDate firstDate;
    private LocalDate secondDate;
}
