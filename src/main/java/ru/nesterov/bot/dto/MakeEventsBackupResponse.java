package ru.nesterov.bot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MakeEventsBackupResponse {
    private Integer savedEventsCount;
    private Boolean isBackupMade;
    private LocalDateTime from;
    private LocalDateTime to;
    private Integer cooldownMinutes;
}
