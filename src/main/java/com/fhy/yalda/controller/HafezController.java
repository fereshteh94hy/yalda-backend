package com.fhy.yalda.controller;

import com.fhy.yalda.dto.HafezResponse;
import com.fhy.yalda.dto.UserRequest;
import com.fhy.yalda.service.OllamaAIProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequestMapping("/api/hafez")
public class HafezController {

    private final OllamaAIProvider ollamaAIProvider;

    public HafezController(OllamaAIProvider ollamaAIProvider) {
        this.ollamaAIProvider = ollamaAIProvider;
    }


    @PostMapping("/getFal")
    public ResponseEntity<HafezResponse> getFortune(@RequestBody UserRequest request) {
        HafezResponse response = ollamaAIProvider.generateResponse(request);
        return ResponseEntity.ok(response);
    }
}