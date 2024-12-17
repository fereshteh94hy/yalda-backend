package com.fhy.yalda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class OllamaRequest {
    private String model;
    private String prompt;
    private Map<String, Object> options;

    public OllamaRequest(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
        this.options = new HashMap<>();
        this.options.put("temperature", 0.7);
        this.options.put("max_tokens", 200);
    }
    }