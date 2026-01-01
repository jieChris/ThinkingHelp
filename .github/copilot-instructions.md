# AI Coding Agent Instructions for ThinkingHelp

## Project Overview
ThinkingHelp is a personalized chronic disease diet management system with AI-powered nutritionist chat. It features user health profiles, diet logging, meal planning, and RAG-based Q&A using a local knowledge base.

## Architecture
- **Backend**: Spring Boot 3.2 + Java 17 + MyBatis Plus + MySQL + Spring AI (OpenAI via Aliyun DashScope)
- **Frontend**: Vue 3 + TypeScript + Element Plus + Pinia + Vue Router + Vite
- **Key Services**: AIService (chat/streaming), RagService (knowledge retrieval), PromptManager (nutritionist prompts)
- **Data Flow**: JWT-authenticated API calls; RAG augments AI responses with knowledge base; streaming via SSE

## Key Components
- **Entities**: User, UserSettings, KnowledgeBase (with vector JSON), HealthProfiles, FoodNutrition, DietLogs
- **Controllers**: AuthController, ChatController (streaming/regular), UserSettingsController
- **Frontend Views**: Dashboard, HealthProfile, AIChat, DietLog, MealPlan, Settings
- **AI Integration**: Uses qwen-plus model; nutritionist persona; context retrieval from knowledge_base table

## Development Workflow
- **Backend Run**: `mvn spring-boot:run` (port 8080)
- **Frontend Run**: `npm run dev` (proxies /api to backend)
- **Database**: MySQL (schema in `backend/src/main/resources/db/schema.sql`); logic delete enabled
- **Build**: Backend `mvn clean package`; Frontend `npm run build`
- **API Docs**: Knife4j at `/doc.html`

## Conventions & Patterns
- **Backend**: Lombok @Data/@RequiredArgsConstructor; @Accessors(chain=true) for settings; Chinese comments; Result<T> response wrapper
- **Entities**: Table names lowercase English (e.g., `users`); JSON fields for flexible data (vectors, tags)
- **Services**: Interface + Impl pattern; Reactor Flux for streaming
- **Frontend**: Pinia stores (e.g., user.ts); Axios interceptors for auth; lazy-loaded routes
- **AI Prompts**: Nutritionist system prompt in PromptManager; append disclaimers for non-local knowledge
- **Error Handling**: GlobalExceptionHandler; frontend ElMessage for user feedback

## Examples
- **Add Entity**: Use @TableName, @TableId(auto), logic-delete field; e.g., see `User.java`
- **API Endpoint**: @RestController with @Tag/@Operation; return Result<T>; e.g., `ChatController.java`
- **AI Chat**: Use RagService.streamAsk() for SSE; build prompts via PromptManager; e.g., `AIServiceImpl.java`
- **Frontend Component**: Use <script setup>; Pinia for state; Element Plus components; e.g., `ChatWindow.vue`
- **Database Query**: MyBatis Plus QueryWrapper; logic delete auto-handled; e.g., `KnowledgeBaseMapper.java`

## Notes
- AI responses parsed as JSON when needed; clean markdown code blocks
- Vector storage as JSON string (simple implementation)
- Health data uses dynamic JSON tags for flexibility
- Always check Result.code === 200 in frontend responses</content>
<parameter name="filePath">d:\System\Desktop\毕业设计\ThinkingHelp\.github\copilot-instructions.md