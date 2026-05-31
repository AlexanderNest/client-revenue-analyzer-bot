package ru.nesterov.bot.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.nesterov.bot.config.BotProperties;
import ru.nesterov.bot.config.RevenueAnalyzerProperties;
import ru.nesterov.bot.dto.AiAnalyzerResponse;
import ru.nesterov.bot.dto.ClientAllScheduleResponse;
import ru.nesterov.bot.dto.CreateClientRequest;
import ru.nesterov.bot.dto.CreateClientResponse;
import ru.nesterov.bot.dto.CreateUserRequest;
import ru.nesterov.bot.dto.CreateUserResponse;
import ru.nesterov.bot.dto.GetActiveClientResponse;
import ru.nesterov.bot.dto.GetAllUsersByRoleAndSourceRequest;
import ru.nesterov.bot.dto.GetAllUsersByRoleAndSourceResponse;
import ru.nesterov.bot.dto.GetClientStatisticResponse;
import ru.nesterov.bot.dto.GetForClientScheduleRequest;
import ru.nesterov.bot.dto.GetForMonthRequest;
import ru.nesterov.bot.dto.GetForYearRequest;
import ru.nesterov.bot.dto.GetIncomeAnalysisForMonthResponse;
import ru.nesterov.bot.dto.GetUnpaidEventsResponse;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.dto.GetUserResponse;
import ru.nesterov.bot.dto.GetYearBusynessStatisticsResponse;
import ru.nesterov.bot.dto.MakeEventsBackupResponse;
import ru.nesterov.bot.dto.Role;
import ru.nesterov.bot.exception.InternalException;
import ru.nesterov.bot.exception.UserFriendlyException;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient.UpdateClientRequest;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient.UpdateClientResponse;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClientRevenueAnalyzerIntegrationClient {
    private final RestTemplate restTemplate;
    private final RevenueAnalyzerProperties revenueAnalyzerProperties;
    private final BotProperties botProperties;
    private final ObjectMapper objectMapper;

    public GetIncomeAnalysisForMonthResponse getIncomeAnalysisForMonth(long userId, String monthName) {
        GetForMonthRequest getForMonthRequest = new GetForMonthRequest();
        getForMonthRequest.setMonthName(monthName);

        return post(String.valueOf(userId), getForMonthRequest, revenueAnalyzerProperties.getGetIncomeAnalysisForMonthUrl(), GetIncomeAnalysisForMonthResponse.class).getBody();
    }

    public GetClientStatisticResponse getClientStatistic(long userId, String clientName) {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("clientName", clientName);

        return get(String.valueOf(userId), requestParams, revenueAnalyzerProperties.getGetClientStatisticUrl(), GetClientStatisticResponse.class).getBody();
    }

    public GetYearBusynessStatisticsResponse getYearBusynessStatistics(long userId, int year) {
        GetForYearRequest getForYearRequest = new GetForYearRequest();
        getForYearRequest.setYear(year);

        return post(String.valueOf(userId), getForYearRequest, revenueAnalyzerProperties.getGetYearBusynessStatisticsUrl(), GetYearBusynessStatisticsResponse.class).getBody();
    }

    public AiAnalyzerResponse getAiStatistics(long userId) {
        String currentMonth = LocalDate.now().getMonth().name().toLowerCase();
        GetForMonthRequest request = new GetForMonthRequest();
        request.setMonthName(currentMonth);

        return post(String.valueOf(userId), request, revenueAnalyzerProperties.getGenerateRecommendationUrl(), AiAnalyzerResponse.class).getBody();
    }

    @Cacheable(value = "getUserByUsername", key = "#request.username", unless = "#result == null")
    public GetUserResponse getUserByUsername(GetUserRequest request) {
        ResponseEntity<GetUserResponse> responseEntity = post(request.getUsername(), request, revenueAnalyzerProperties.getGetUserByUsernameUrl(), GetUserResponse.class);
        if (responseEntity.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return null;
        }
        return responseEntity.getBody();
    }

    public CreateClientResponse createClient(String userId, CreateClientRequest createClientRequest) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    revenueAnalyzerProperties.getUrl() + revenueAnalyzerProperties.getClientCreateUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(createClientRequest, createHeaders(userId)),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), CreateClientResponse.class);
            }

            if (response.getStatusCode() == HttpStatus.CONFLICT) {
                String message = extractErrorMessage(response.getBody());
                return CreateClientResponse.builder()
                        .responseCode(HttpStatus.CONFLICT.value())
                        .errorMessage(message)
                        .build();
            }

            throw new UserFriendlyException(
                    String.format("Непредвиденный ответ от сервера: %d", response.getStatusCodeValue())
            );
        } catch (HttpClientErrorException.Conflict ex) {
            String body = ex.getResponseBodyAsString();
            String message = extractErrorMessage(body);
            return CreateClientResponse.builder()
                    .responseCode(HttpStatus.CONFLICT.value())
                    .errorMessage(message)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Не удалось десериализовать CreateClientResponse", e);
            throw new InternalException(e);
        }
    }

    public CreateUserResponse createUser(CreateUserRequest createUserRequest) {
        ResponseEntity<CreateUserResponse> responseEntity = post(createUserRequest.getUserIdentifier(), createUserRequest, revenueAnalyzerProperties.getCreateUserUrl(), CreateUserResponse.class);
        return responseEntity.getBody();
    }

    public ClientAllScheduleResponse getClientSchedule(long userId, String clientName, LocalDateTime leftDate, LocalDateTime rightDate) {
        GetForClientScheduleRequest request = new GetForClientScheduleRequest();
        request.setClientName(clientName);
        request.setLeftDate(leftDate);
        request.setRightDate(rightDate);

        ResponseEntity<ClientAllScheduleResponse> response = post(
                String.valueOf(userId),
                request,
                revenueAnalyzerProperties.getGetScheduleUrl(),
                ClientAllScheduleResponse.class
        );

        return response.getBody();

    }

    public List<GetActiveClientResponse> getActiveClients(long userId) {
        return postForList(String.valueOf(userId),
                null,
                revenueAnalyzerProperties.getGetActiveClientsUrl(),
                new ParameterizedTypeReference<>() {
                }
        );
    }

    public MakeEventsBackupResponse makeEventsBackup(long userId) {
        ResponseEntity<MakeEventsBackupResponse> response = get(
                String.valueOf(userId),
                null,
                revenueAnalyzerProperties.getEventsBackupUrl(),
                MakeEventsBackupResponse.class
        );

        return response.getBody();
    }

    public List<GetUnpaidEventsResponse> getUnpaidEvents(long userId) {
        return getForList(String.valueOf(userId),
                revenueAnalyzerProperties.getGetUnpaidEventsUrl(),
                new ParameterizedTypeReference<>() {
                }
        );
    }

    public ResponseEntity<Void> deleteClient(long userId, String clientName) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("clientName", clientName);

        return delete(String.valueOf(userId),
                params,
                revenueAnalyzerProperties.getClientUrl()
        );
    }

    public UpdateClientResponse updateClient(long userId, UpdateClientRequest updateClientRequest) {

        return post(String.valueOf(userId),
                updateClientRequest,
                revenueAnalyzerProperties.getClientUpdateUrl(),
                UpdateClientResponse.class
        ).getBody();
    }

    private <T> ResponseEntity<T> get(String username, MultiValueMap<String, String> requestParams, String endpoint, Class<T> responseType) {
        return exchange(username, requestParams, null, endpoint, responseType, HttpMethod.GET);
    }

    private ResponseEntity<Void> delete(String username, MultiValueMap<String, String> requestParams, String endpoint) {
        return exchange(username, requestParams, null, endpoint, Void.class, HttpMethod.DELETE);
    }

    public GetAllUsersByRoleAndSourceResponse getUsersIdByRoleAndSource(long chatId, Role role, String source) {
        GetAllUsersByRoleAndSourceRequest request = new GetAllUsersByRoleAndSourceRequest();
        request.setRole(role);
        request.setSource(source);
        ResponseEntity<GetAllUsersByRoleAndSourceResponse> responseEntity = post(String.valueOf(chatId), request, revenueAnalyzerProperties.getGetUsersIdByRoleAndSourceUrl(),
                GetAllUsersByRoleAndSourceResponse.class
        );
        return responseEntity.getBody();
    }


    private <T> ResponseEntity<T> post(String username, Object request, String endpoint, Class<T> responseType) {
        return exchange(username, null, request, endpoint, responseType, HttpMethod.POST);
    }

    private <T> List<T> getForList(String username, String endpoint, ParameterizedTypeReference<List<T>> typeReference) {
        HttpEntity<Object> requestEntity = new HttpEntity<>(createHeaders(username));

        ResponseEntity<List<T>> responseEntity = restTemplate.exchange(
                revenueAnalyzerProperties.getUrl() + endpoint,
                HttpMethod.GET,
                requestEntity,
                typeReference
        );

        return responseEntity.getBody();
    }

    private <T> List<T> postForList(String username, Object request, String endpoint, ParameterizedTypeReference<List<T>> typeReference) {
        HttpEntity<Object> requestEntity = new HttpEntity<>(request, createHeaders(username));

        ResponseEntity<List<T>> responseEntity = restTemplate.exchange(
                revenueAnalyzerProperties.getUrl() + endpoint,
                HttpMethod.POST,
                requestEntity,
                typeReference
        );

        return responseEntity.getBody();
    }

    private <T> ResponseEntity<T> exchange(String username, MultiValueMap<String, String> requestParams, Object request, String endpoint, Class<T> responseType, HttpMethod httpMethod) {
        HttpEntity<Object> entity = new HttpEntity<>(request, createHeaders(username));

        URI uri = UriComponentsBuilder.fromHttpUrl(revenueAnalyzerProperties.getUrl() + endpoint)
                .queryParams(requestParams)
                .build()
                .encode()
                .toUri();

        try {
            return restTemplate.exchange(
                    uri,
                    httpMethod,
                    entity,
                    responseType
            );
        } catch (HttpClientErrorException.NotFound ignore) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (HttpClientErrorException.Conflict ignore) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (HttpServerErrorException.InternalServerError ignore) {
            throw new UserFriendlyException(getResponseMessage(ignore.getResponseBodyAsString()));
        }
    }

    private String getResponseMessage(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("message").asText();
        } catch (Exception e) {
            log.error("Cannot parse response = [{}]", responseBody);
            throw new InternalException(e);
        }
    }

    private HttpHeaders createHeaders(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-secret-token", botProperties.getSecretToken());
        headers.set("X-username", username);

        return headers;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("message")) {
                return root.get("message").asText();
            }
        } catch (JsonProcessingException e) {
            log.warn("Не удалось распарсить тело ошибки при создании клиента: {}", responseBody, e);
        }
        return "Клиент уже существует";
    }
}