package ru.nesterov.bot.dto;

import lombok.Data;

@Data
public class GetAllUsersByRoleAndSourceRequest {
    private String source;
    private Role role;
}
