package ru.nesterov.bot.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetAllUsersByRoleAndSourceResponse {
    private List<String> userIds;
}
