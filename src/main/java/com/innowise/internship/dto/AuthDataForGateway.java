package com.innowise.internship.dto;

import lombok.Data;

@Data
public class AuthDataForGateway {

  private Long userId;
  private String email;
  private String password;

}
