package ru.nesterov.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String mainCalendarId;
    private String cancelledCalendarId;
    private String userIdentifier;
    private boolean isCancelledCalendarEnabled;
    private String source;
}
