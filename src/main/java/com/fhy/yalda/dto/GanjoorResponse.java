package com.fhy.yalda.dto;

import lombok.Data;

import java.util.List;
@Data
public class GanjoorResponse {
    private String title;
    private String fullUrl;
    private List<Verse> verses;
    private String plainText;
}
