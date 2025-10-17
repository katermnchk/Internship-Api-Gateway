package com.innowise.internship.controller;

import com.innowise.internship.dto.AuthDataForGateway;
import com.innowise.internship.dto.GatewayRegistrationRequest;
import com.innowise.internship.dto.UserDataForGateway;
import com.innowise.internship.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${app.api.base-path}/registration")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

  private static final String USER_SERVICE_URI = "http://user-service/api/v1/users";
  private static final String AUTH_SERVICE_URI = "http://auth-service/api/v1/auth/register";
  private static final String MSG_ROLLBACK_FAILURE = "Auth service failed and user rollback also failed for user ID: {}";

  private final WebClient.Builder webClientBuilder;

  @PostMapping
  public Mono<ResponseEntity<String>> register(@RequestBody GatewayRegistrationRequest request) {
    log.info("Registration attempt for email: {}", request.getUserData().getEmail());

    if (!request.getUserData().getEmail().equals(request.getAuthData().getEmail())) {
      log.warn("Validation failed: Emails do not match");
      return Mono.just(ResponseEntity.badRequest().body("Emails in user data and auth data must match"));
    }

    return createUser(request.getUserData())
        .flatMap(userDto -> {
          log.info(" User created with ID: {}", userDto.getId());
          return createCredentials(userDto, request.getAuthData());
        })
        .onErrorResume(this::handleRegistrationError);
  }

  private Mono<UserResponseDto.UserDto> createUser(UserDataForGateway userData) {
    return webClientBuilder.build().post()
        .uri(USER_SERVICE_URI)
        .bodyValue(userData)
        .retrieve()
        .bodyToMono(UserResponseDto.class)
        .map(UserResponseDto::getData)
        .doOnError(e -> log.error("Error calling User Service", e));
  }


  private Mono<ResponseEntity<String>> createCredentials(
      UserResponseDto.UserDto userDto,
      AuthDataForGateway authData
  ) {
    authData.setUserId(userDto.getId());

    return webClientBuilder.build().post()
        .uri(AUTH_SERVICE_URI)
        .bodyValue(authData)
        .retrieve()
        .toBodilessEntity()
        .map(response -> {
          log.info("Credentials created for user ID: {}", userDto.getId());
          return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        })
        .doOnError(e -> log.error("Initiating rollback for user ID: {}", userDto.getId(), e))
        .onErrorResume(error -> rollbackUserCreation(userDto.getId())
            .then(Mono.error(new RuntimeException("User creation has been rolled back")))
        );
  }

  private Mono<Void> rollbackUserCreation(Long userId) {
    log.warn("Deleting user with ID: {}", userId);
    return webClientBuilder.build().delete()
        .uri(USER_SERVICE_URI + "/" + userId)
        .retrieve()
        .bodyToMono(Void.class)
        .doOnError(e -> log.error(MSG_ROLLBACK_FAILURE, userId, e));
  }

  private Mono<ResponseEntity<String>> handleRegistrationError(Throwable error) {
    log.error("Registration process failed: {}", error.getMessage());
    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getMessage()));
  }
}