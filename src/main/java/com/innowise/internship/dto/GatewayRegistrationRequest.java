package com.innowise.internship.dto;

import lombok.Data;

@Data
public class GatewayRegistrationRequest {

  private UserDataForGateway userData;
  private AuthDataForGateway authData;

}
