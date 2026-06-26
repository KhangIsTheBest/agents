package com.pdk.agents.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service đánh giá điều kiện trên Edge để quyết định phân luồng (Branching).
 *
 * Cú pháp condition_expression hỗ trợ:
 * - null hoặc rỗng  → luôn đúng (đường đi mặc định)
 * - "default"        → luôn đúng (đường fallback, ưu tiên thấp nhất)
 * - "contains:từ_khóa" → đúng nếu output chứa từ_khóa (case-insensitive)
 * - "not_contains:từ_khóa" → đúng nếu output KHÔNG chứa từ_khóa
 * - "starts_with:chuỗi"   → đúng nếu output bắt đầu bằng chuỗi
 * - "equals:giá_trị"      → đúng nếu output bằng giá_trị (trim + case-insensitive)
 *
 * Ví dụ:
 * - Router Agent output "CATEGORY: technical" → Edge condition "contains:technical" → true
 * - Validator Agent output "APPROVED" → Edge condition "equals:approved" → true
 */
@Service
@Slf4j
public class ConditionEvaluator {

    /**
     * Đánh giá condition expression dựa trên output của Agent trước đó.
     *
     * @param conditionExpression Biểu thức điều kiện trên Edge
     * @param agentOutput         Output từ Agent nguồn
     * @return true nếu điều kiện thỏa mãn
     */
    public boolean evaluate(String conditionExpression, String agentOutput) {
        // Null hoặc rỗng → luôn đúng
        if (conditionExpression == null || conditionExpression.isBlank()) {
            return true;
        }

        String expr = conditionExpression.trim();
        String exprLower = expr.toLowerCase();
        String output = agentOutput != null ? agentOutput.trim() : "";
        String outputLower = output.toLowerCase();

        // "default" → luôn đúng (fallback path)
        if (exprLower.equals("default")) {
            return true;
        }

        // "contains:keyword"
        if (exprLower.startsWith("contains:")) {
            String keyword = expr.substring("contains:".length()).trim().toLowerCase();
            boolean result = outputLower.contains(keyword);
            log.debug("Condition [contains:{}] on output '{}...' → {}", keyword,
                    output.substring(0, Math.min(50, output.length())), result);
            return result;
        }

        // "not_contains:keyword"
        if (exprLower.startsWith("not_contains:")) {
            String keyword = expr.substring("not_contains:".length()).trim().toLowerCase();
            boolean result = !outputLower.contains(keyword);
            log.debug("Condition [not_contains:{}] → {}", keyword, result);
            return result;
        }

        // "starts_with:prefix"
        if (exprLower.startsWith("starts_with:")) {
            String prefix = expr.substring("starts_with:".length()).trim().toLowerCase();
            boolean result = outputLower.startsWith(prefix);
            log.debug("Condition [starts_with:{}] → {}", prefix, result);
            return result;
        }

        // "equals:value"
        if (exprLower.startsWith("equals:")) {
            String value = expr.substring("equals:".length()).trim().toLowerCase();
            boolean result = outputLower.equals(value);
            log.debug("Condition [equals:{}] → {}", value, result);
            return result;
        }

        // Fallback: coi expression như keyword contains
        log.warn("Unknown condition format '{}', treating as contains check", expr);
        return outputLower.contains(exprLower);
    }

    /**
     * Kiểm tra đây có phải edge "default" (fallback) không.
     */
    public boolean isDefaultEdge(String conditionExpression) {
        return conditionExpression != null
                && conditionExpression.trim().equalsIgnoreCase("default");
    }
}
