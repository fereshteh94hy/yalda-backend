package com.fhy.yalda.service;

import com.fhy.yalda.dto.GanjoorResponse;
import com.fhy.yalda.dto.HafezResponse;
import com.fhy.yalda.dto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OllamaAIProvider {

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Value("${spring.ai.ollama.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final String HAFEZ_API_URL = "https://ganjgah.ir/api/ganjoor/hafez/faal";
    private final Map<String, HafezResponse> cache = new ConcurrentHashMap<>();
    private final int MAX_RETRIES = 3;

    public OllamaAIProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    public HafezResponse generateResponse(UserRequest request) {
        try {

            String cacheKey = request.getWish() + request.getLanguage();
            if (cache.containsKey(cacheKey)) {
                return cache.get(cacheKey);
            }

            HafezResponse response;
            if ("fa".equals(request.getLanguage())) {
                response = getPersianFortune();
            } else {
                response = getEnglishFortuneWithRetry(request);
            }

            cache.put(cacheKey, response);
            return response;

        } catch (Exception e) {
            log.error("Error in generateResponse: ", e);
            return createErrorResponse(request.getLanguage());
        }
    }

    private HafezResponse getPersianFortune() {
        try {
            ResponseEntity<GanjoorResponse> response = restTemplate.getForEntity(HAFEZ_API_URL, GanjoorResponse.class);
            GanjoorResponse ganjoorResponse = response.getBody();

            if (ganjoorResponse != null && !ganjoorResponse.getVerses().isEmpty()) {
                HafezResponse fortune = new HafezResponse();

                String fullPoem = ganjoorResponse.getVerses().get(0).getText() + "\n" +
                        (ganjoorResponse.getVerses().size() > 1 ? ganjoorResponse.getVerses().get(1).getText() : "");

                fortune.setPoem(fullPoem);
                fortune.setInterpretation(createInterpretation(ganjoorResponse));
                fortune.setPrediction(createPrediction(ganjoorResponse));

                return fortune;
            }
        } catch (Exception e) {
            log.error("Error getting Persian fortune", e);
        }
        return createErrorResponse("fa");
    }

    private HafezResponse getEnglishFortuneWithRetry(UserRequest request) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                return getEnglishFortune(request);
            } catch (Exception e) {
                attempts++;
                log.warn("Attempt {} failed: {}", attempts, e.getMessage());
                if (attempts == MAX_RETRIES) {
                    log.error("All attempts failed for English fortune", e);
                    return createDefaultEnglishResponse();
                }
                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return createDefaultEnglishResponse();
                }
            }
        }
        return createDefaultEnglishResponse();
    }

    private HafezResponse getEnglishFortune(UserRequest request) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", createEnglishPrompt(request));
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 50);
            requestBody.put("top_p", 0.9);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/generate",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("response")) {
                return parseResponse(response.getBody().get("response").toString(), "en");
            }

            return createDefaultEnglishResponse();

        } catch (Exception e) {
            log.error("Error in English fortune generation", e);
            throw e;
        }
    }

    private String createEnglishPrompt(UserRequest request) {
        return String.format("W:%s\nV:\nM:\nP:", request.getWish());
    }

    private String createInterpretation(GanjoorResponse response) {
        if (response.getPlainText() != null) {
            return response.getPlainText().split("\n")[0];
        }
        return "این فال نشان از گشایش در کار شما دارد";
    }

    private String createPrediction(GanjoorResponse response) {
        return "این فال نشان می‌دهد که " +
//                (response.getTitle() != null ? response.getTitle() : "به زودی به مراد دل خواهید رسید");
                ( "به زودی به مراد دل خواهید رسید");
    }

    private HafezResponse parseResponse(String rawResponse, String language) {
        HafezResponse response = new HafezResponse();
        try {
            String[] lines = rawResponse.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("V:")) {
                    response.setPoem(line.substring(2).trim());
                } else if (line.startsWith("M:")) {
                    response.setInterpretation(line.substring(2).trim());
                } else if (line.startsWith("P:")) {
                    response.setPrediction(line.substring(2).trim());
                }
            }

            validateAndFillResponse(response, language);
            return response;
        } catch (Exception e) {
            log.error("Parse error: ", e);
            return createErrorResponse(language);
        }
    }

    private void validateAndFillResponse(HafezResponse response, String language) {
        if ("fa".equals(language)) {
            if (response.getPoem() == null) {
                response.setPoem("در همه دیر مغان نیست چو من شیدایی");
            }
            if (response.getInterpretation() == null) {
                response.setInterpretation("نشانه‌های خوبی در راه است");
            }
            if (response.getPrediction() == null) {
                response.setPrediction("به زودی به نتیجه می‌رسید");
            }
        } else {
            if (response.getPoem() == null) {
                response.setPoem("If thou would ‘st know the secret of Love’s fire.\n" +
                        "It shall be manifest unto thine eyes ");
            }
            if (response.getInterpretation() == null) {
                response.setInterpretation("Good signs are on the way");
            }
            if (response.getPrediction() == null) {
                response.setPrediction("You will reach your goal soon");
            }
        }
    }

    private HafezResponse createErrorResponse(String language) {
        HafezResponse response = new HafezResponse();
        if ("fa".equals(language)) {
            response.setPoem("در همه دیر مغان نیست چو من شیدایی");
            response.setInterpretation("متأسفانه در دریافت پاسخ مشکلی پیش آمد");
            response.setPrediction("لطفاً دوباره تلاش کنید");
        } else {
            response.setPoem("Life is a journey through light and shade");
            response.setInterpretation("Sorry, there was an error");
            response.setPrediction("Please try again later");
        }
        return response;
    }

    private HafezResponse createDefaultEnglishResponse() {
        HafezResponse response = new HafezResponse();
        response.setPoem("Life is a journey through light and shade");
        response.setInterpretation("Every challenge brings new opportunities");
        response.setPrediction("Good fortune awaits your patience");
        return response;
    }
}
