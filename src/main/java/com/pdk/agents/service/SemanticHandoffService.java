package com.pdk.agents.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

/**
 * Service thực hiện cơ chế Semantic Handoff (Nén lịch sử hội thoại thành cấu trúc JSON).
 * Giúp tối ưu hóa độ dài của prompt đầu vào và tránh hiện tượng bùng nổ token.
 */
@Service
@Slf4j
public class SemanticHandoffService {

    private final ConversationService conversationService;
    private final ChatClient localChatClient;
    private final ObjectMapper objectMapper;

    // Ngưỡng kích hoạt nén lịch sử
    private static final int MESSAGE_COUNT_THRESHOLD = 4;
    private static final int CHAR_LENGTH_THRESHOLD = 1000;

    public SemanticHandoffService(ConversationService conversationService,
                                  OllamaChatModel localModel,
                                  ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.localChatClient = ChatClient.builder(localModel).build();
        this.objectMapper = objectMapper;
    }

    /**
     * Lấy lịch sử hội thoại đã được nén (hoặc lịch sử thô nếu chưa đạt ngưỡng).
     *
     * @param conversationId ID phiên hội thoại
     * @return Chuỗi chứa thông tin ngữ cảnh lịch sử trò chuyện
     */
    public String getCompressedHistory(String conversationId) {
        // Lấy lịch sử hội thoại thô tối đa 20 tin nhắn gần nhất
        String rawHistory = conversationService.getRecentHistory(conversationId, 20);

        if (rawHistory == null || rawHistory.isBlank()) {
            return "";
        }

        // Tính số lượng tin nhắn trong lịch sử bằng cách đếm dòng tiêu đề "[USER]" hoặc "[ASSISTANT]"
        int messageCount = countOccurrences(rawHistory, "[USER]:") + countOccurrences(rawHistory, "[ASSISTANT]:");

        // Nếu lịch sử chưa đạt ngưỡng kích hoạt, trả về lịch sử thô luôn để tránh phí chạy AI
        if (messageCount < MESSAGE_COUNT_THRESHOLD && rawHistory.length() < CHAR_LENGTH_THRESHOLD) {
            log.info("[SemanticHandoff] Lịch sử ngắn (messages={}, length={}), sử dụng định dạng thô.",
                    messageCount, rawHistory.length());
            return rawHistory;
        }

        log.info("[SemanticHandoff] Lịch sử vượt ngưỡng (messages={}, length={}), đang tiến hành nén bằng AI...",
                messageCount, rawHistory.length());

        try {
            // Gọi Local LLM để nén thông tin thành cấu trúc JSON tinh gọn
            String prompt = """
                    Bạn là một trợ lý nén ngữ cảnh (Semantic Handoff Compressor). 
                    Nhiệm vụ của bạn là đọc lịch sử hội thoại dưới đây và nén nó thành một cấu trúc JSON tinh gọn nhất có thể để làm ngữ cảnh đầu vào cho các Agent tiếp theo.
                    
                    YÊU CẦU BẮT BUỘC:
                    1. Trả về cấu trúc JSON hợp lệ theo đúng schema dưới đây.
                    2. KHÔNG thêm bất kỳ văn bản giải thích hoặc dẫn giải nào ngoài khối JSON.
                    3. Giữ nguyên các dữ kiện cốt lõi, ý định của người dùng và các quyết định đã được thống nhất.
                    
                    SCHEMA JSON ĐẦU RA:
                    {
                      "intent": "Mục đích cốt lõi cuối cùng của người dùng",
                      "facts": ["Các dữ kiện/thông tin quan trọng đã thu thập được"],
                      "decisions": ["Các bước xử lý hoặc kết quả chính đã được Agent trước thực hiện"],
                      "status": "Trạng thái hiện tại của cuộc trò chuyện và vấn đề đang chờ xử lý"
                    }
                    
                    Dưới đây là lịch sử hội thoại thô cần nén:
                    %s
                    """.formatted(rawHistory);

            String aiResponse = localChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Làm sạch kết quả trả về từ LLM (bỏ ký tự ```json nếu có)
            String cleanedJson = extractJson(aiResponse);

            // Kiểm tra tính hợp lệ của chuỗi JSON
            if (isValidJson(cleanedJson)) {
                log.info("[SemanticHandoff] Nén thành công. Kích thước giảm từ {} xuống {} ký tự.",
                        rawHistory.length(), cleanedJson.length());
                
                return "=== CONVERSATION HISTORY (COMPRESSED STATE) ===\n" 
                        + cleanedJson 
                        + "\n=== END HISTORY ===";
            } else {
                log.warn("[SemanticHandoff] AI trả về kết quả không đúng cấu trúc JSON. Fallback về lịch sử thô!");
                return rawHistory;
            }

        } catch (Exception e) {
            log.error("[SemanticHandoff] Quá trình nén ngữ cảnh gặp lỗi: {}. Tự động fallback về lịch sử thô!", e.getMessage());
            return rawHistory;
        }
    }

    /**
     * Đếm số lần xuất hiện của từ khóa trong văn bản.
     */
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }

    /**
     * Loại bỏ markdown block (```json) ra khỏi phản hồi của LLM.
     */
    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }

    /**
     * Xác thực cấu trúc chuỗi JSON hợp lệ.
     */
    private boolean isValidJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(jsonStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
