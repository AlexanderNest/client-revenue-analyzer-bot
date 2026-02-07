package ru.nesterov.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetUserResponse {
    private long userId;
    private String username;
    private String mainCalendarId;
    private Boolean isCancelledCalendarEnabled;
    private String cancelledCalendarId;
    private Role role;
    private String source;
}
