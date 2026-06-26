# Development Log — Multi-Agent AI Platform

---

## Giai đoạn 1: Thiết lập Hạ tầng & Nền tảng Backend

### Ngày: 2026-06-23

#### ✅ Bước 1 — Docker Compose Infrastructure
- Tạo `docker-compose.yml` tại thư mục gốc với 3 services:
  - **PostgreSQL** (`ankane/pgvector:latest`) — Port 5432, DB: `multi_agent_db`
  - **Redis** (`redis:7-alpine`) — Port 6379
  - **Ollama** (`ollama/ollama:latest`) — Port 11434
- Tạo `init-db/01-init.sql` — auto-run khi container khởi tạo lần đầu:
  - Kích hoạt extension `pgvector`
  - Tạo bảng: `workflows`, `nodes`, `edges`, `agent_memories`
  - Tạo HNSW index cho cột `embedding`

#### ✅ Bước 2 — Cấu hình Spring Boot
- Cấu hình `application.yaml`:
  - Datasource: `jdbc:postgresql://localhost:5432/multi_agent_db`
  - JPA/Hibernate: `ddl-auto: update`, `show-sql: true`
  - Spring AI Ollama: `http://localhost:11434`, model `qwen2.5:7b`, temperature `0.4`

#### ✅ Bước 3 — Mô hình hóa dữ liệu (JPA Entities)
- Tạo Enums: `AgentType` (ROUTER, EXECUTOR, VALIDATOR), `ModelSource` (LOCAL, CLOUD)
- Tạo Entities:
  - `Workflow` — bảng `workflows`, OneToMany với Node và Edge
  - `Node` — bảng `nodes`, ManyToOne Workflow, enum AgentType/ModelSource
  - `Edge` — bảng `edges`, ManyToOne Workflow/SourceNode/TargetNode
  - `AgentMemory` — bảng `agent_memories`, cột `embedding vector(768)`
- Tạo Repositories:
  - `WorkflowRepository`
  - `NodeRepository` (query by workflowId, agentType)
  - `EdgeRepository` (query by workflowId, sourceNodeId, targetNodeId)
  - `AgentMemoryRepository` (native pgvector semantic search queries)

#### ✅ Verification
- `mvnw clean compile` — BUILD SUCCESS
- `mvnw spring-boot:run` — ✅ Connected to PostgreSQL, 4 repositories scanned, Tomcat started on port 8080

---

## Giai đoạn 2: Service Layer + REST APIs + Agent Engine

### Ngày: 2026-06-23

#### ✅ Bước 1 — Exception & DTO Layer
- `ResourceNotFoundException` — custom 404 exception
- DTOs (Java Records):
  - `WorkflowRequest` / `WorkflowResponse`
  - `NodeRequest` / `NodeResponse`
  - `EdgeRequest` / `EdgeResponse`
  - `ChatRequest` / `ChatResponse` / `ExecutionStep`

#### ✅ Bước 2 — Service Layer
- `WorkflowService` — CRUD Workflow + detailed response mapper (nested nodes/edges)
- `NodeService` — CRUD Node + filter by workflowId/agentType
- `EdgeService` — CRUD Edge + validation source/target nodes exist
- `AgentExecutionService` — Gọi Ollama qua Spring AI `ChatClient`, inject system prompt per-agent
- `WorkflowExecutorService` — **Graph Traversal Engine**:
  - Tìm Entry Node (node không có incoming edge, ưu tiên ROUTER)
  - Duyệt Node → Edge → Node tuần tự
  - Output Agent trước = Input Agent sau
  - Trả về `ChatResponse` với execution log từng bước

#### ✅ Bước 3 — REST Controllers
- `WorkflowController` — `POST/GET/PUT/DELETE /api/workflows`
- `NodeController` — `POST/GET/PUT/DELETE /api/nodes` + filter by workflow/type
- `EdgeController` — `POST/GET/DELETE /api/edges`
- `ChatController` — `POST /api/chat` — endpoint chính chạy Workflow end-to-end

#### ✅ Verification
- `mvnw clean compile` — BUILD SUCCESS (30 source files)

---

### API Endpoints Summary:
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/workflows` | Tạo Workflow |
| GET | `/api/workflows` | List tất cả Workflows |
| GET | `/api/workflows/{id}` | Chi tiết Workflow (+ nodes, edges) |
| POST | `/api/nodes` | Tạo Node (Agent) |
| GET | `/api/nodes/workflow/{id}` | List Nodes trong Workflow |
| POST | `/api/edges` | Tạo Edge (nối 2 Nodes) |
| **POST** | **`/api/chat`** | **Chạy Workflow end-to-end** |

---

## Giai đoạn 3: Intelligence Layer

### Ngày: 2026-06-23

#### ✅ Bước 1 — Conversation History
- Tạo bảng `conversations` + `conversation_messages` (SQL init + JPA Entity)
- Enum `MessageRole` (USER, ASSISTANT, SYSTEM)
- `ConversationService` — tạo session, lưu tin nhắn, lấy recent history cho AI context
- Cập nhật `ChatRequest` / `ChatResponse` thêm `conversationId`
- Workflow Executor tự động lưu user input + agent output vào conversation

#### ✅ Bước 2 — Branching Logic
- `ConditionEvaluator` — đánh giá `condition_expression` trên Edge:
  - `contains:keyword`, `not_contains:keyword`, `starts_with:prefix`, `equals:value`
  - `default` → fallback path
- Workflow Executor: đánh giá tất cả outgoing edges, chọn edge match condition trước, rồi default, rồi fallback

#### ✅ Bước 3 — Semantic Memory (RAG)
- `EmbeddingService` — gọi Ollama `nomic-embed-text` → vector 768 chiều
- `MemoryService` — lưu output Agent vào pgvector, tìm kiếm cosine similarity
- Workflow Executor: trước khi Agent xử lý → tìm memory liên quan → inject vào prompt

#### ✅ Bước 4 — Redis Cache
- Thêm dependency `spring-boot-starter-data-redis`
- `CacheService` — cache AI response bằng SHA-256(nodeId + prompt + input), TTL cấu hình
- Agent Execution: check cache trước → nếu hit thì skip gọi AI → cache response sau khi gọi

#### ✅ Bước 5 — Nâng cấp Core Services
- `AgentExecutionService`: enriched prompt (history + memory + input), Redis cache, multi-model placeholder
- `WorkflowExecutorService`: branching logic, conversation persistence, RAG injection, graceful error handling

#### ✅ Verification
- `mvnw clean compile` — BUILD SUCCESS (40 source files)

#### ⚠ Lưu ý quan trọng:
- Cần chạy `docker exec -it enterprise-ollama ollama pull nomic-embed-text` để tải model embedding

---

## Giai đoạn 4: Giao diện Người dùng (Frontend Workflow Designer)

### Ngày: 2026-06-26

#### ✅ Bước 1 — Thiết lập dự án & Thư viện
- Cài đặt và cấu hình thư viện đồ thị React Flow (`@xyflow/react`) tương thích React 19.
- Thiết lập CORS phía Spring Boot để cho phép kết nối từ `http://localhost:5173`.

#### ✅ Bước 2 — Trình kết nối API (`src/api.js`)
- Xây dựng API client đồng bộ các endpoint của Backend:
  - Workflows (CRUD)
  - Nodes/Agents (CRUD)
  - Edges/Connections (Create, Delete)
  - Chat/Execute (POST `/api/chat` chạy chuỗi tác nhân)

#### ✅ Bước 3 — Thiết kế giao diện (index.css & index.html)
- Thiết lập hệ thống UI/UX cao cấp (Premium Dark Mode & Glassmorphism):
  - Tông màu tối sâu (Midnight Space) kết hợp với các hiệu ứng kính mờ `backdrop-filter`.
  - Các viền phát sáng (Glow border) phân biệt loại Agent: Router (Xanh dương), Executor (Tím), Validator (Vàng).
  - Tối ưu hóa SEO: Thêm thẻ meta description, thẻ tiêu đề tiếng Việt và thẻ `<h1>` ẩn phục vụ cấu trúc ngữ nghĩa.

#### ✅ Bước 4 — Thành phần giao diện (React Components)
- `App.jsx`: State trung tâm, điều phối luồng dữ liệu giữa các panel, tự động lưu tọa độ di chuyển Node (drag & drop) vào `localStorage` của trình duyệt.
- `Sidebar.jsx`: Quản lý danh sách các kịch bản, cho phép thêm nhanh và xóa kịch bản.
- `CustomAgentNode.jsx`: Node tùy chỉnh trong React Flow hiển thị tên agent, loại agent, mô tả chỉ dẫn và nguồn model.
- `WorkflowCanvas.jsx`: Bản vẽ Canvas kéo thả của React Flow, tích hợp Toolbar thêm nhanh các loại tác nhân.
- `NodeConfigPanel.jsx`: Điều chỉnh tên agent, system prompt, loại agent và model source.
- `EdgeConfigPanel.jsx`: Cho phép cấu hình các điều kiện rẽ nhánh (Condition Expression) của Router Agent (ví dụ: `contains:yes`, `default`).
- `ChatPanel.jsx`: Giao diện hộp thoại tương tác thời gian thực, có khả năng vẽ lại chi tiết nhật ký thực thi (Execution Log) của từng Agent bước-bước để người dùng dễ theo dõi luồng tư duy của AI.

#### ✅ Verification
- Chạy `npm run build` thành công, biên dịch ra 100% mã sạch không có cảnh báo/lỗi (Vite build success in 381ms).

---

## Giai đoạn WebSockets: Đồng bộ tiến trình thời gian thực

### Ngày: 2026-06-26

#### ✅ Bước 1 — Cấu hình WebSocket Backend
- Thêm dependency `spring-boot-starter-websocket` trong `pom.xml`.
- Tạo `WebSocketConfig.java` và đăng ký endpoint kết nối tại `/ws/execution` (cho phép CORS cho localhost).
- Tạo `WebSocketEvent.java` để chuẩn hóa các gói tin JSON gửi qua socket.
- Xây dựng `AgentWebSocketHandler.java` quản lý các active sessions và phát sự kiện dạng broadcast.

#### ✅ Bước 2 — Tích hợp vòng lặp duyệt đồ thị
- Nâng cấp `WorkflowExecutorService.java`:
  - Trước khi gọi AI của Agent: Gửi sự kiện `NODE_RUNNING`.
  - Khi Agent hoàn tất xử lý: Gửi sự kiện `NODE_COMPLETED` đi kèm với thông tin của bước thực thi `ExecutionStep`.
  - Khi hoàn thành toàn bộ kịch bản: Gửi sự kiện `WORKFLOW_COMPLETED` đi kèm kết quả cuối cùng.
  - Khi phát sinh lỗi: Gửi sự kiện `WORKFLOW_FAILED` để giao diện phản hồi lập tức.

#### ✅ Bước 3 — Giao diện Người dùng (Frontend Client)
- `App.jsx`: Khởi tạo kết nối WebSockets thuần với cơ chế tự động kết nối lại (auto-reconnect) sau 3 giây nếu bị gián đoạn.
- `CustomAgentNode.jsx`: Nhận trạng thái `activeRunning` và áp dụng hiệu ứng CSS nhấp nháy phát sáng tương ứng với màu của từng loại Agent khi tác nhân đó đang làm việc.
- `ChatPanel.jsx`: Cập nhật log thực thi thời gian thực của từng Agent khi tin nhắn sự kiện `NODE_COMPLETED` đổ về, người dùng không cần đợi toàn bộ kịch bản kết thúc để xem nhật ký.

#### ✅ Verification
- Đẩy toàn bộ mã nguồn frontend và backend lên Git repositories của người dùng.
- Biên dịch backend (`mvnw clean compile`): **BUILD SUCCESS**.
- Đóng gói frontend (`npm run build`): **SUCCESS** (biên dịch ra 100% mã javascript/css sạch).

---

## Giai đoạn Hybrid Routing & Local Fallback: Định tuyến mô hình & Tự phục hồi

### Ngày: 2026-06-26

#### ✅ Bước 1 — Cấu hình tích hợp OpenAI Cloud
- Thêm dependency `spring-ai-starter-model-openai` vào `pom.xml`.
- Cấu hình các thông số kết nối OpenAI trong `application.yaml` đi kèm API key placeholder mặc định (`demo-key`) để tránh ứng dụng bị lỗi khởi động khi thiếu biến môi trường.

#### ✅ Bước 2 — Tích hợp Định tuyến và Phục hồi tại AgentExecutionService
- Cập nhật `AgentExecutionService.java`:
  - Khởi tạo rõ ràng hai client: `localChatClient` (gắn với Ollama) và `cloudChatClient` (gắn với OpenAI).
  - Triển khai thuật toán định tuyến (Hybrid Routing): Tác vụ chạy `CLOUD` sẽ được điều hướng sang OpenAI, tác vụ chạy `LOCAL` sẽ chạy trực tiếp dưới local Ollama.
  - Thiết kế khối lệnh xử lý dự phòng (Local Fallback): Nếu có bất cứ sự cố nào xảy ra với Cloud LLM (như API key không hợp lệ, lỗi mạng...), hệ thống sẽ tự động bắt exception, ghi log cảnh báo và chuyển hướng yêu cầu xử lý sang Local Ollama (qwen2.5:7b) ngay lập tức, bảo toàn luồng chạy nghiệp vụ Multi-Agent.

#### ✅ Verification
- Biên dịch thành công backend (`mvnw clean compile`): **BUILD SUCCESS**.
- Đẩy toàn bộ thay đổi mã nguồn lên Git repository.
