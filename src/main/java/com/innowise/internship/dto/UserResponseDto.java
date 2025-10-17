package com.innowise.internship.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

@Getter
public class UserResponseDto {

  private UserDto data;

  @Data
  public static class UserDto {
    @JsonProperty("userId")
    private Long id;
  }

}
