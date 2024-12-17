package com.fhy.yalda.dto;

import lombok.Data;

@Data
public class OllamaResponse {
    private String model;
    private String response;
    private String context;
}