package ru.nesterov.bot.handlers.implementation.invocable.stateful.GetPdfPeport;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GetPdfReportRequest {
    private String clientName;
    private LocalDate displayMonth;
    private LocalDate firstDate;
    private LocalDate secondDate;
}
