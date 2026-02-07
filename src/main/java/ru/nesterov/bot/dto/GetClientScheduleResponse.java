package ru.nesterov.bot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GetClientScheduleResponse {
    private LocalDateTime eventStart;
    private LocalDateTime eventEnd;
    private boolean requiresShift;
}
