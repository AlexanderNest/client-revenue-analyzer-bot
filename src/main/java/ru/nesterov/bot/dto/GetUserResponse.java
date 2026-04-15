package ru.nesterov.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetUserResponse implements Serializable {
    private long userId;
    private String username;
    private String mainCalendarId;
    private Boolean isCancelledCalendarEnabled;
    private String cancelledCalendarId;
    private Role role;
    private String source;
}
