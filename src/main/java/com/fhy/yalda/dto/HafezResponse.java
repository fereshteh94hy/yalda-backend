package com.fhy.yalda.dto;

import lombok.Data;

import java.util.List;
@Data
public class HafezResponse {
    private String poem;
    private String interpretation;
    private String prediction;
    private List<String> advice;
}