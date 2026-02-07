package ru.nesterov.bot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GetUnpaidEventsResponse {
    private String summary;
    private LocalDateTime eventStart;
}
