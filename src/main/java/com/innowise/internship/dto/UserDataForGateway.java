package com.innowise.internship.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserDataForGateway {

  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;

}
