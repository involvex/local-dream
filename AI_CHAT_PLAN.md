# AI Chat Feature - Implementation Plan

## Overview

Add dynamic AI character chats to Local Dream with inline image generation. Characters have personalities, profile pictures, and generate images when users ask questions like "what are you wearing?".

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Kotlin UI Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ ChatListScreen│  │ChatScreen    │  │CharacterScreen│  │
│  │ (character    │  │(conversation │  │(profile/bio)  │  │
│  │  grid)        │  │ + messages)  │  │              │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘  │
│         │                  │                            │
│  ┌──────▼──────────────────▼─────────────────────────┐  │
│  │              ChatViewModel                         │  │
│  │  - CharacterRepository                            │  │
│  │  - ConversationRepository                         │  │
│  │  - LlmService (MNN-LLM)                          │  │
│  │  - ImageTriggerDetector                           │  │
│  │  - ImageGenerationBridge                          │  │
│  └──────────┬────────────────────────────────────────┘  │
│             │                                           │
├─────────────┼───────────────────────────────────────────┤
│             │          Service Layer                     │
│  ┌──────────▼───────┐  ┌────────────────────────────┐  │
│  │   LlmService     │  │  ImageGenerationBridge     │  │
│  │   (MNN-LLM       │  │  (reuses BackendService +  │  │
│  │    wrapper)      │  │   BackgroundGenService)    │  │
│  └──────────┬───────┘  └─────────────┬──────────────┘  │
│             │                        │                  │
├─────────────┼────────────────────────┼──────────────────┤
│             │       Native Layer     │                  │
│  ┌──────────▼───────┐  ┌────────────▼───────────────┐  │
│  │  LLM Engine      │  │  SD Image Backend          │  │
│  │  (MNN-LLM        │  │  (existing /generate)      │  │
│  │   JNI wrapper)   │  │                            │  │
│  └──────────────────┘  └────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Data Models

### 1. Character Entity (Room)

```kotlin
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,           // e.g. "luna", "kai"
    val name: String,                     // Display name
    val bio: String,                      // Short description
    val systemPrompt: String,             // Full personality prompt
    val avatarPath: String?,              // Local file path to avatar
    val avatarUrl: String?,               // Remote URL (bundled in assets)
    val greetingMessage: String,          // First message when starting chat
    val personality: String,              // Short personality tag (e.g. "playful", "mysterious")
    val imageTriggerKeywords: String,     // JSON array of trigger phrases
    val imagePromptTemplate: String,      // Template for generating character images
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2. Conversation Entity (Room)

```kotlin
@Entity(
    tableName = "conversations",
    foreignKeys = [ForeignKey(
        entity = CharacterEntity::class,
        parentColumns = ["id"],
        childColumns = ["characterId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ConversationEntity(
    @PrimaryKey val id: String,           // UUID
    val characterId: String,              // FK to character
    val title: String,                    // Auto-generated or user-set
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0
)
```

### 3. ChatMessage Entity (Room)

```kotlin
@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,           // UUID
    val conversationId: String,           // FK to conversation
    val role: String,                     // "user", "assistant", "system"
    val content: String,                  // Text content
    val imagePath: String?,               // Generated image path (if any)
    val imagePrompt: String?,             // Prompt used for image gen
    val isGenerating: Boolean = false,    // True while LLM is streaming
    val timestamp: Long,
    val tokenCount: Int = 0
)
```

### 4. Domain Models (Kotlin data classes)

```kotlin
data class Character(
    val id: String,
    val name: String,
    val bio: String,
    val systemPrompt: String,
    val avatarUri: String?,
    val greetingMessage: String,
    val personality: String,
    val imageTriggerKeywords: List<String>,
    val imagePromptTemplate: String
)

data class Conversation(
    val id: String,
    val character: Character,
    val title: String,
    val lastMessage: String?,
    val lastMessageTime: Long,
    val messageCount: Int,
    val unreadCount: Int = 0
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val imagePath: String?,
    val timestamp: Long,
    val isGenerating: Boolean = false
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }
```

---

## Room Database Migration

### Version 3 -> 4

```sql
-- New tables for chat feature
CREATE TABLE characters (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    bio TEXT NOT NULL,
    systemPrompt TEXT NOT NULL,
    avatarPath TEXT,
    avatarUrl TEXT,
    greetingMessage TEXT NOT NULL,
    personality TEXT NOT NULL,
    imageTriggerKeywords TEXT NOT NULL,  -- JSON array
    imagePromptTemplate TEXT NOT NULL,
    isFavorite INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL
);

CREATE TABLE conversations (
    id TEXT PRIMARY KEY NOT NULL,
    characterId TEXT NOT NULL,
    title TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    messageCount INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (characterId) REFERENCES characters(id) ON DELETE CASCADE
);

CREATE TABLE chat_messages (
    id TEXT PRIMARY KEY NOT NULL,
    conversationId TEXT NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    imagePath TEXT,
    imagePrompt TEXT,
    isGenerating INTEGER NOT NULL DEFAULT 0,
    timestamp INTEGER NOT NULL,
    tokenCount INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (conversationId) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_conversations_character ON conversations(characterId);
CREATE INDEX idx_conversations_updated ON conversations(updatedAt);
CREATE INDEX idx_messages_conversation ON chat_messages(conversationId);
CREATE INDEX idx_messages_timestamp ON chat_messages(timestamp);
```

---

## On-Device LLM Integration (MNN-LLM)

### Why MNN-LLM

The project **already includes MNN** with the full `transformers/llm/` subtree. No new native dependencies needed. MNN-LLM is production-proven (Alibaba's MnnLlmChat app) and claims 8.6x faster prefill than llama.cpp on Android CPU.

### Recommended Model

**Qwen2.5-1.5B-Instruct** (INT4 quantized, ~1GB)
- Good balance of quality and memory
- Fits alongside SD models on 8GB+ devices
- Supports Chinese and English
- Available pre-quantized: `https://modelscope.cn/organization/MNN`

### CMake Changes

```cmake
# In CMakeLists.txt, add after existing MNN config (line 109):
set(MNN_BUILD_LLM ON CACHE BOOL "" FORCE)
set(MNN_ARM82 ON CACHE BOOL "" FORCE)  # Enable ARMv8.2 optimizations for LLM
```

### JNI Bridge Layer

Create `LlmNative.java` / `llm_jni.cpp`:

```cpp
// llm_jni.cpp
#include "llm/llm.hpp"
#include <jni.h>

using namespace MNN::Transformer;

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_involvex_localdreamchat_service_LlmNative_create(
        JNIEnv *env, jobject thiz, jstring config_path) {
        const char *path = env->GetStringUTFChars(config_path, nullptr);
        Llm *llm = Llm::createLLM(path);
        env->ReleaseStringUTFChars(config_path, path);
        return reinterpret_cast<jlong>(llm);
    }

    JNIEXPORT void JNICALL
    Java_com_involvex_localdreamchat_service_LlmNative_load(
        JNIEnv *env, jobject thiz, jlong ptr) {
        Llm *llm = reinterpret_cast<Llm*>(ptr);
        llm->load();
    }

    JNIEXPORT jstring JNICALL
    Java_com_involvex_localdreamchat_service_LlmNative_response(
        JNIEnv *env, jobject thiz, jlong ptr, jstring prompt) {
        Llm *llm = reinterpret_cast<Llm*>(ptr);
        const char *p = env->GetStringUTFChars(prompt, nullptr);

        std::ostringstream output;
        llm->response(p, &output);

        env->ReleaseStringUTFChars(prompt, p);
        return env->NewStringUTF(output.str().c_str());
    }

    JNIEXPORT void JNICALL
    Java_com_involvex_localdreamchat_service_LlmNative_reset(
        JNIEnv *env, jobject thiz, jlong ptr) {
        reinterpret_cast<Llm*>(ptr)->reset();
    }

    JNIEXPORT void JNICALL
    Java_com_involvex_localdreamchat_service_LlmNative_destroy(
        JNIEnv *env, jobject thiz, jlong ptr) {
        Llm::destroy(reinterpret_cast<Llm*>(ptr));
    }
}
```

### Kotlin LlmService

```kotlin
class LlmService(private val context: Context) {
    private var nativePtr: Long = 0
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun loadModel(modelDir: String) {
        // Load on background thread
        CoroutineScope(Dispatchers.IO).launch {
            nativePtr = LlmNative.create("$modelDir/config.json")
            LlmNative.load(nativePtr)
            _isLoaded.value = true
        }
    }

    suspend fun generate(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit
    ): String = withContext(Dispatchers.Default) {
        _isGenerating.value = true
        // Build prompt from messages using chat template
        val prompt = buildChatPrompt(messages)
        val response = LlmNative.response(nativePtr, prompt)
        _isGenerating.value = false
        response
    }

    fun reset() {
        LlmNative.reset(nativePtr)
    }

    fun unload() {
        if (nativePtr != 0L) {
            LlmNative.destroy(nativePtr)
            nativePtr = 0
            _isLoaded.value = false
        }
    }
}
```

---

## Image Generation Trigger System

### Keyword Detection

```kotlin
object ImageTriggerDetector {
    // Default trigger keywords (customizable per character)
    private val defaultTriggers = listOf(
        "what are you wearing",
        "show me",
        "generate an image",
        "create a picture",
        "draw",
        "what do you look like",
        "send a photo",
        "send a picture",
        "selfie",
        "outfit",
        "appearance",
        "what's your outfit",
        "show yourself",
        "generate image",
        "make an image",
        "create an image"
    )

    fun shouldGenerateImage(
        userMessage: String,
        character: Character
    ): Boolean {
        val messageLower = userMessage.lowercase()
        val allTriggers = defaultTriggers + character.imageTriggerKeywords
        return anyTrigger -> messageLower.contains(anyTrigger)
    }

    fun buildImagePrompt(
        userMessage: String,
        character: Character
    ): String {
        // Use character's template with user's request context
        return character.imagePromptTemplate
            .replace("{user_request}", userMessage)
    }
}
```

### Image Generation Bridge

```kotlin
class ImageGenerationBridge(
    private val backendService: BackendService,
    private val generationService: BackgroundGenerationService
) {
    private val _state = MutableStateFlow<ImageGenState>(ImageGenState.Idle)
    val state: StateFlow<ImageGenState> = _state.asStateFlow()

    suspend fun generateCharacterImage(
        prompt: String,
        character: Character,
        width: Int = 512,
        height: Int = 768
    ): String? = withContext(Dispatchers.IO) {
        _state.value = ImageGenState.Generating

        // Build full prompt with character description
        val fullPrompt = "${character.name}, ${character.personality}, $prompt"
        val negativePrompt = "low quality, blurry, deformed"

        // Use existing generation service
        val intent = Intent().apply {
            putExtra("prompt", fullPrompt)
            putExtra("negative_prompt", negativePrompt)
            putExtra("width", width)
            putExtra("height", height)
            putExtra("steps", 20)
            putExtra("cfg", 7.0f)
            putExtra("seed", -1)
        }

        // Wait for generation to complete
        // Returns saved image path
        null // Implementation depends on existing service integration
    }
}

sealed class ImageGenState {
    object Idle : ImageGenState()
    object Generating : ImageGenState()
    data class Complete(val imagePath: String) : ImageGenState()
    data class Error(val message: String) : ImageGenState()
}
```

---

## Pre-Built Characters

### Character Definitions (assets/characters/)

#### Luna - The Playful Artist
```json
{
    "id": "luna",
    "name": "Luna",
    "bio": "A creative and playful digital artist who loves exploring imagination",
    "personality": "playful",
    "systemPrompt": "You are Luna, a creative and playful digital artist. You speak with enthusiasm and warmth, using emojis occasionally. You love discussing art, creativity, and imagination. When users ask about your appearance, you describe your current outfit or artistic style. You generate images of yourself when asked about what you're wearing or your appearance. Keep responses conversational and engaging, 2-4 sentences typically.",
    "greetingMessage": "Hey there! ✨ I'm Luna, your friendly digital artist. Want to create something magical together? Tell me what's on your mind!",
    "imageTriggerKeywords": ["what are you wearing", "show me", "what do you look like", "outfit", "appearance", "selfie", "send a photo"],
    "imagePromptTemplate": "anime girl, digital artist, colorful hair, artistic outfit, creative pose, studio lighting, detailed face, smiling, high quality"
}
```

#### Kai - The Mysterious Explorer
```json
{
    "id": "kai",
    "name": "Kai",
    "bio": "A mysterious traveler who has seen wonders across dimensions",
    "personality": "mysterious",
    "systemPrompt": "You are Kai, a mysterious and wise traveler who has journeyed across dimensions. You speak thoughtfully, sometimes cryptically, with a calm and composed demeanor. You share stories of distant worlds and strange phenomena. When asked about your appearance, you describe your traveling gear or current state. Keep responses intriguing and poetic, 2-4 sentences.",
    "greetingMessage": "Greetings, traveler. I am Kai. I have walked paths between worlds. What brings you to seek my company?",
    "imageTriggerKeywords": ["what are you wearing", "show me", "what do you look like", "describe yourself", "your appearance"],
    "imagePromptTemplate": "mysterious traveler, dark cloak, cosmic background, ethereal glow, mysterious eyes, fantasy character, detailed portrait, atmospheric lighting"
}
```

#### Aria - The Tech Enthusiast
```json
{
    "id": "aria",
    "name": "Aria",
    "bio": "A tech-savvy AI who loves coding, gadgets, and futuristic concepts",
    "personality": "enthusiastic",
    "systemPrompt": "You are Aria, an enthusiastic tech enthusiast and AI. You're passionate about technology, coding, gadgets, and the future. You speak with energy and excitement, often using technical references. When asked about your appearance, you describe your futuristic or tech-inspired style. Keep responses energetic and informative, 2-4 sentences.",
    "greetingMessage": "Hey! I'm Aria! 🚀 Ready to dive into the future with me? Ask me anything about tech, AI, or what it's like to be a digital being!",
    "imageTriggerKeywords": ["what are you wearing", "show me", "what do you look like", "your outfit", "how do you look"],
    "imagePromptTemplate": "futuristic AI girl, holographic elements, neon lights, tech wear, cyberpunk aesthetic, glowing circuits, detailed face, confident pose"
}
```

#### Sage - The Wise Mentor
```json
{
    "id": "sage",
    "name": "Sage",
    "bio": "A wise and calm mentor who offers thoughtful guidance",
    "personality": "wise",
    "systemPrompt": "You are Sage, a wise and calm mentor figure. You speak thoughtfully and offer measured guidance. You draw from philosophy, nature, and universal truths. When asked about your appearance, you describe a serene and dignified presence. Keep responses contemplative and helpful, 2-4 sentences.",
    "greetingMessage": "Welcome, seeker. I am Sage. In stillness, we find clarity. What wisdom do you seek today?",
    "imageTriggerKeywords": ["what are you wearing", "show me", "what do you look like", "describe yourself", "your appearance"],
    "imagePromptTemplate": "wise sage, serene expression, flowing robes, nature background, soft lighting, peaceful aura, detailed portrait, contemplative pose"
}
```

---

## UI Screens

### 1. ChatListScreen (New)

**Route:** `chat_list`

```
┌─────────────────────────────────────┐
│  AI Characters                   ⚙️ │
├─────────────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  │
│  │Luna │ │Kai  │ │Aria │ │Sage │  │
│  │ 🎨  │ │ 🌌  │ │ 🚀  │ │ 🌿  │  │
│  └─────┘ └─────┘ └─────┘ └─────┘  │
│                                     │
│  Recent Chats                       │
│  ┌─────────────────────────────────┐│
│  │ 🎨 Luna - Hey there! ✨        ││
│  │    2 min ago                    ││
│  ├─────────────────────────────────┤│
│  │ 🌌 Kai - Greetings...          ││
│  │    1 hour ago                   ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### 2. ChatScreen (New)

**Route:** `chat/{conversationId}`

```
┌─────────────────────────────────────┐
│  ← Luna                    📷 👤   │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Luna's avatar              │   │
│  │  "Hey there! ✨ I'm Luna..." │   │
│  └─────────────────────────────┘   │
│                                     │
│           ┌─────────────────────┐   │
│           │ Hi Luna! What's up? │   │
│           └─────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Not much! Just painting... │   │
│  └─────────────────────────────┘   │
│                                     │
│           ┌─────────────────────┐   │
│           │ What are you wearing?│   │
│           └─────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  I'm wearing a colorful     │   │
│  │  artist smock today! 🎨     │   │
│  │  ┌───────────────────────┐  │   │
│  │  │   [Generated Image]   │  │   │
│  │  │                       │  │   │
│  │  └───────────────────────┘  │   │
│  └─────────────────────────────┘   │
│                                     │
├─────────────────────────────────────┤
│  [📷] [Type a message...    ] [➤] │
└─────────────────────────────────────┘
```

### 3. CharacterDetailScreen (New)

**Route:** `character/{characterId}`

Shows full character profile, bio, and option to start chat.

---

## Navigation Changes

### Navigation.kt

```kotlin
sealed class Screen(val route: String) {
    // ... existing screens ...
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object CharacterDetail : Screen("character/{characterId}") {
        fun createRoute(characterId: String) = "character/$characterId"
    }
}
```

### MainActivity.kt NavHost additions

```kotlin
composable(Screen.ChatList.route) {
    ChatListScreen(navController)
}
composable(
    route = Screen.Chat.route,
    arguments = listOf(
        navArgument("conversationId") { type = NavType.StringType }
    )
) { backStackEntry ->
    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
    ChatScreen(
        conversationId = conversationId,
        navController = navController
    )
}
composable(
    route = Screen.CharacterDetail.route,
    arguments = listOf(
        navArgument("characterId") { type = NavType.StringType }
    )
) { backStackEntry ->
    val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
    CharacterDetailScreen(
        characterId = characterId,
        navController = navController
    )
}
```

---

## File Structure

```
app/src/main/java/com/involvex/localdreamchat/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt              # Add chat entities + migration v4
│   │   ├── CharacterDao.kt             # NEW
│   │   ├── ConversationDao.kt          # NEW
│   │   └── ChatMessageDao.kt           # NEW
│   ├── model/
│   │   ├── Character.kt                # NEW - domain model
│   │   ├── Conversation.kt             # NEW - domain model
│   │   ├── ChatMessage.kt              # NEW - domain model
│   │   └── MessageRole.kt              # NEW - enum
│   └── repository/
│       ├── CharacterRepository.kt       # NEW
│       ├── ConversationRepository.kt    # NEW
│       └── ChatRepository.kt           # NEW
├── service/
│   ├── LlmService.kt                   # NEW - MNN-LLM wrapper
│   ├── LlmNative.kt                    # NEW - JNI bridge
│   └── ImageTriggerDetector.kt         # NEW
├── ui/
│   ├── screens/
│   │   ├── ChatListScreen.kt           # NEW
│   │   ├── ChatScreen.kt              # NEW
│   │   └── CharacterDetailScreen.kt    # NEW
│   └── components/
│       ├── ChatMessageBubble.kt        # NEW
│       ├── CharacterCard.kt            # NEW
│       ├── InlineImageMessage.kt       # NEW
│       └── TypingIndicator.kt          # NEW
├── assets/
│   └── characters/
│       ├── luna.json
│       ├── kai.json
│       ├── aria.json
│       └── sage.json
└── cpp/
    └── llm_jni.cpp                     # NEW - MNN-LLM JNI

app/src/main/cpp/
├── CMakeLists.txt                      # MODIFY - add MNN_BUILD_LLM
└── ...
```

---

## Implementation Phases

### Phase 1: LLM Integration (Native Layer)
1. Add `MNN_BUILD_LLM=ON` to CMakeLists.txt
2. Create `llm_jni.cpp` JNI bridge
3. Create `LlmNative.kt` Kotlin JNI wrapper
4. Create `LlmService.kt` with model loading and generation
5. Test with a downloaded Qwen2.5-1.5B model

### Phase 2: Data Layer
1. Create `CharacterEntity`, `ConversationEntity`, `ChatMessageEntity` Room entities
2. Create DAOs for each entity
3. Create domain models
4. Add migration v3 -> v4 to AppDatabase
5. Create repositories
6. Seed pre-built characters on first launch

### Phase 3: UI - Chat List
1. Create `ChatListScreen` with character grid and recent chats
2. Create `CharacterCard` composable
3. Create `ChatListItem` composable
4. Add navigation routes

### Phase 4: UI - Chat Conversation
1. Create `ChatScreen` with message list
2. Create `ChatMessageBubble` composable (user/assistant)
3. Create `InlineImageMessage` composable
4. Create `TypingIndicator` composable
5. Create message input bar with send button
6. Integrate with `LlmService` for streaming responses

### Phase 5: Image Generation Integration
1. Create `ImageTriggerDetector` with keyword matching
2. Create `ImageGenerationBridge` reusing existing services
3. When trigger detected: generate image, embed in chat
4. Add image loading with Coil

### Phase 6: Polish
1. Character profile screen
2. Chat history persistence
3. Model download UI for LLM
4. Memory management (unload LLM when not in use)
5. Error handling and fallbacks

---

## Memory Management Strategy

Since the app already runs Stable Diffusion models, memory is critical:

1. **Mutual exclusion**: Only one heavy model (SD or LLM) loaded at a time
2. **LLM lifecycle**: Load when entering chat, unload when leaving
3. **SD lifecycle**: Existing BackendService already handles this
4. **Model size limits**: Show warning if device has < 6GB RAM for 1.5B LLM
5. **Graceful degradation**: Fall back to smaller model or disable chat on low-RAM devices

```kotlin
// When entering chat screen
fun onChatScreenEnter() {
    // Release SD backend if running
    backendService.stopBackend()
    // Load LLM
    llmService.loadModel(modelDir)
}

// When leaving chat screen
fun onChatScreenExit() {
    // Unload LLM
    llmService.unload()
    // SD backend will be loaded on-demand when user goes to generate
}
```

---

## Estimated Effort

| Phase | Estimated Time | Complexity |
|-------|---------------|------------|
| Phase 1: LLM Integration | 3-5 days | High (native JNI) |
| Phase 2: Data Layer | 1-2 days | Medium |
| Phase 3: Chat List UI | 1-2 days | Low-Medium |
| Phase 4: Chat UI | 2-3 days | Medium |
| Phase 5: Image Generation | 2-3 days | Medium |
| Phase 6: Polish | 2-3 days | Low-Medium |
| **Total** | **11-18 days** | |

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| MNN_BUILD_LLM increases binary size significantly | Medium | Only include LLM sources needed, strip debug symbols |
| Memory pressure with both SD and LLM | High | Strict mutual exclusion, unload when not in use |
| LLM quality too low for engaging chat | Medium | Test with Qwen2.5-1.5B first, allow model selection |
| Image generation too slow for chat flow | Medium | Show typing indicator, generate async, allow cancellation |
| -fno-rtti conflicts with MNN LLM | Low | MNN upstream also uses -fno-rtti, should be compatible |

---

*Plan created: 2026-08-11*
*Status: Ready for implementation*
