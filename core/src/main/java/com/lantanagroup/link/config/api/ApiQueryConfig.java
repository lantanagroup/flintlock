package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter @Setter
public class ApiQueryConfig {
  @NotNull
  private ApiQueryConfigModes mode;

  private String url;
  private String apiKey;
}
