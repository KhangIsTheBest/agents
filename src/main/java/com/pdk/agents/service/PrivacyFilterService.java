package com.pdk.agents.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service lọc và che mờ thông tin cá nhân/nhạy cảm (PII Redaction).
 * Giúp đảm bảo an toàn thông tin khi gửi dữ liệu sang các mô hình Cloud LLM (OpenAI).
 */
@Service
public class PrivacyFilterService {

    public record FilterResult(String cleanText, Map<String, String> replacements) {}

    // Regex tìm kiếm các thông tin nhạy cảm
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)\\d{10,11}(?!\\d)|\\+?\\d{1,4}[\\s.-]?\\(?\\d{1,3}\\)?[\\s.-]?\\d{3,4}[\\s.-]?\\d{4}");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    /**
     * Che mờ (Anonymize) thông tin nhạy cảm.
     * Thay thế Email, SĐT, IP bằng placeholder và lưu lại ánh xạ để giải mã sau.
     */
    public FilterResult anonymize(String text) {
        if (text == null || text.isBlank()) {
            return new FilterResult(text, new HashMap<>());
        }

        Map<String, String> replacements = new HashMap<>();
        String result = text;

        // 1. Che mờ Email
        result = replaceAndStore(result, EMAIL_PATTERN, "EMAIL", replacements);

        // 2. Che mờ Số điện thoại
        result = replaceAndStore(result, PHONE_PATTERN, "PHONE", replacements);

        // 3. Che mờ Số thẻ tín dụng
        result = replaceAndStore(result, CREDIT_CARD_PATTERN, "CARD", replacements);

        // 4. Che mờ Địa chỉ IP
        result = replaceAndStore(result, IP_PATTERN, "IP", replacements);

        return new FilterResult(result, replacements);
    }

    /**
     * Phục hồi (De-Anonymize) thông tin nhạy cảm ban đầu.
     * Thay thế các placeholder bằng giá trị thô ban đầu.
     */
    public String deAnonymize(String text, Map<String, String> replacements) {
        if (text == null || text.isBlank() || replacements.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String replaceAndStore(String text, Pattern pattern, String prefix, Map<String, String> replacements) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        int index = replacements.size() + 1;

        while (matcher.find()) {
            String originalValue = matcher.group();
            String placeholder = String.format("[%s_%d]", prefix, index++);
            
            replacements.put(placeholder, originalValue);
            
            sb.append(text, lastEnd, matcher.start());
            sb.append(placeholder);
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }
}
