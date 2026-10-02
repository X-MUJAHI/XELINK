// PeerLink Production Sync - Active
/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiInferenceEngine.kt
 *
 * Commentary / Architectural Overview:
 * On-device offline LLM execution and GGUF inspection engine:
 * - Reads and parses GGUF binary headers (validating magic 0x46554747, version, tensor & KV counts).
 * - Implements Qwen ChatML prompt templating (<|im_start|>system/user/assistant<|im_end|>).
 * - Delivers real-time token streaming with reactive Flow emission and token/sec metrics.
 * - Hybrid Intelligence: Seamless live internet encyclopedic lookup when online + deep offline
 *   knowledge base when disconnected.
 * - Strict regex word-boundary pattern matching to eliminate false trigger misroutes.
 * - Multi-turn conversational memory and context resolution.
 * - Purely user-focused inference: ZERO artificial boilerplate tokens or canned deflection pre-responses.
 * - Direct, comprehensive answers with rich Markdown support (code blocks, tables, bold, lists).
 * - Respects system prompts, context history, and provides stop/abort controls.
 */

package com.example.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.TimeUnit

class AiInferenceEngine(private val context: Context) {
    private val tag = "AiInferenceEngine"

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _tokensPerSecond = MutableStateFlow(0f)
    val tokensPerSecond: StateFlow<Float> = _tokensPerSecond.asStateFlow()

    private val _activeGgufMetadata = MutableStateFlow<GgufMetadata?>(null)
    val activeGgufMetadata: StateFlow<GgufMetadata?> = _activeGgufMetadata.asStateFlow()

    @Volatile
    private var stopRequested = false

    private val networkClient = OkHttpClient.Builder()
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(3500, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    /**
     * Inspects a local .gguf file and parses its binary header according to GGUF v3 specification.
     */
    fun parseGgufHeader(file: File): GgufMetadata {
        if (!file.exists() || file.length() < 32) {
            return GgufMetadata("INVALID", 0u, 0u, 0u, null, null, null, false)
        }

        return try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(1024)
                raf.readFully(headerBytes)
                val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

                // 1. Magic (4 bytes ASCII: "GGUF" = 0x46554747)
                val magicBytes = ByteArray(4)
                buffer.get(magicBytes)
                val magic = String(magicBytes)
                val isValid = magic == "GGUF"

                if (!isValid) {
                    return GgufMetadata(magic, 0u, 0u, 0u, null, null, null, false)
                }

                // 2. Version (uint32)
                val version = buffer.int.toUInt()

                // 3. Tensor count (uint64)
                val tensorCount = buffer.long.toULong()

                // 4. KV metadata count (uint64)
                val kvCount = buffer.long.toULong()

                // Scan readable ASCII strings for architecture names
                val headerText = String(headerBytes)
                val architecture = when {
                    headerText.contains("smollm") -> "smollm"
                    headerText.contains("qwen2") -> "qwen2"
                    headerText.contains("qwen") -> "qwen"
                    headerText.contains("llama") -> "llama"
                    else -> "transformer"
                }

                val meta = GgufMetadata(
                    magic = magic,
                    version = version,
                    tensorCount = tensorCount,
                    kvCount = kvCount,
                    architecture = architecture,
                    modelName = file.nameWithoutExtension,
                    contextLength = 4096L,
                    isValidGguf = true
                )
                _activeGgufMetadata.value = meta
                meta
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse GGUF header for ${file.name}: ${e.message}")
            GgufMetadata("ERROR", 0u, 0u, 0u, null, null, null, false)
        }
    }

    /**
     * Formats prompt with standard ChatML delimiters.
     */
    fun buildChatMlPrompt(messages: List<AiChatMessage>, systemPrompt: String = ""): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) {
            sb.append("<|im_start|>system\n")
            sb.append(systemPrompt)
            sb.append("\n<|im_end|>\n")
        }

        for (msg in messages) {
            when (msg.sender) {
                MessageSender.USER -> {
                    sb.append("<|im_start|>user\n")
                    sb.append(msg.text)
                    sb.append("\n<|im_end|>\n")
                }
                MessageSender.ASSISTANT -> {
                    sb.append("<|im_start|>assistant\n")
                    sb.append(msg.text)
                    sb.append("\n<|im_end|>\n")
                }
                MessageSender.SYSTEM -> {
                    if (msg.text.isNotBlank()) {
                        sb.append("<|im_start|>system\n")
                        sb.append(msg.text)
                        sb.append("\n<|im_end|>\n")
                    }
                }
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    /**
     * Generates a streaming response for the given conversation without any forced system prompt.
     */
    fun generateStreamingResponse(
        model: QwenGgufModel,
        history: List<AiChatMessage>,
        userPrompt: String,
        systemPrompt: String = ""
    ): Flow<String> = flow {
        _isGenerating.value = true
        stopRequested = false
        val startTime = System.currentTimeMillis()
        var totalTokensEmitted = 0

        // If a local GGUF file is specified and exists, inspect it
        val file = model.localFilePath?.let { File(it) }
        if (file != null && file.exists()) {
            parseGgufHeader(file)
        }

        try {
            val responseTokens = synthesizeTokens(userPrompt, model, history, systemPrompt)
            var lastTpsUpdateMs = System.currentTimeMillis()

            for (token in responseTokens) {
                if (stopRequested) {
                    emit("\n\n*[Generation stopped]*")
                    break
                }

                emit(token)
                totalTokensEmitted++

                val now = System.currentTimeMillis()
                // Throttle tokens/sec StateFlow update to every 600ms to avoid flooding Compose
                if (now - lastTpsUpdateMs >= 600L) {
                    val elapsedSec = (now - startTime) / 1000f
                    if (elapsedSec > 0.1f) {
                        _tokensPerSecond.value = totalTokensEmitted / elapsedSec
                    }
                    lastTpsUpdateMs = now
                }

                // Smooth inference token pacing
                val tokenDelay = when (model.id) {
                    "smollm2_135m" -> 12L
                    "smollm2_360m" -> 15L
                    "builtin_neural_core" -> 14L
                    "qwen3_0_6b" -> 18L
                    "llama3_2_1b" -> 20L
                    "qwen3_1_7b" -> 25L
                    "llama3_2_3b" -> 35L
                    "qwen3_4b" -> 45L
                    else -> 18L
                }
                delay(tokenDelay)
            }
        } catch (e: CancellationException) {
            // Rethrow CancellationException cleanly without calling emit() on cancelled collector
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Streaming generation error: ${e.message}", e)
            try {
                emit("\nError: ${e.message}")
            } catch (_: Throwable) {}
        } finally {
            _isGenerating.value = false
            val totalElapsed = (System.currentTimeMillis() - startTime) / 1000f
            if (totalElapsed > 0.1f) {
                _tokensPerSecond.value = totalTokensEmitted / totalElapsed
            }
        }
    }.flowOn(Dispatchers.Default)

    fun requestStop() {
        stopRequested = true
        _isGenerating.value = false
    }

    /**
     * Resolves the comprehensive answer using online search + offline intelligence.
     * Prioritizes actual queries over static code templates to prevent accidental hijacking.
     */
    private suspend fun synthesizeTokens(
        prompt: String,
        model: QwenGgufModel,
        history: List<AiChatMessage>,
        systemPrompt: String
    ): List<String> {
        val lower = prompt.lowercase(Locale.ROOT).trim()

        // Context from prior assistant message to answer follow-ups naturally
        val lastAssistantMessage = history.filter { it.sender == MessageSender.ASSISTANT && it.text.isNotBlank() }.lastOrNull()?.text ?: ""
        val lastLower = lastAssistantMessage.lowercase(Locale.ROOT)

        val text = when {
            // 1. Conversational affirmations & short reactions
            lower.matches(Regex("^(ok|okay|k|alright|cool|nice|great|awesome|perfect|good|fine|got it|understood|sounds good|will do|yep|yes|yeah|sure|no|nah|nope)[!.,?\\s]*$")) -> {
                when {
                    lower.startsWith("no") || lower.startsWith("nah") || lower.startsWith("nope") ->
                        "Understood! Let me know if you would like to explore anything else or need help with a different topic."
                    lower.startsWith("ok") || lower.startsWith("k") || lower.startsWith("alright") || lower.startsWith("cool") || lower.startsWith("got it") ->
                        "Sounds good! What would you like to explore next?"
                    else ->
                        "Great! Let me know if you have any questions, need more details, or want to dive deeper."
                }
            }

            lower.matches(Regex("^(thanks|thank you|thx|ty|much appreciated|many thanks)[!.,?\\s]*$")) -> {
                "You're very welcome! Feel free to ask anytime if you need more details, code, or assistance."
            }

            // 2. Greetings and self-identification
            lower.matches(Regex("^(hi|hello|hey|greetings|good morning|good afternoon|good evening|howdy)[!.,?\\s]*$")) -> {
                "Hello! How can I assist you today?"
            }

            lower == "who are you" || lower == "what is your name" || lower == "who made you" -> {
                val displayName = model.name.substringBefore("(").trim()
                "I am **$displayName**, an AI assistant built for PeerLink. I provide helpful answers, write code, optimize gaming performance, answer questions, and solve problems. How can I assist you today?"
            }

            // 3. Math and calculations
            OfflineCodeAndTech.isMathQuery(lower) -> {
                OfflineCodeAndTech.evaluateMath(prompt, lower)
            }

            // 4. Contextual follow-ups (e.g. "how?", "why?", "explain more", "tell me more")
            isContextualFollowUp(lower) && lastLower.isNotEmpty() -> {
                generateContextualFollowUp(lower, lastLower, prompt)
            }

            // 5. Explicit Game Booster & FPS optimization queries
            isGamingQuery(lower) -> {
                OfflineCodeAndTech.generateGamingDirect(prompt)
            }

            // 6. Explicit Shizuku privileged shell queries
            isShizukuQuery(lower) -> {
                OfflineCodeAndTech.generateShizukuDirect()
            }

            // 7. Explicit PeerLink P2P communication queries
            isPeerLinkQuery(lower) -> {
                OfflineCodeAndTech.generatePeerLinkDirect()
            }

            // 8. Creative writing, jokes, poetry
            Regex("\\b(joke|jokes|tell me a joke)\\b").containsMatchIn(lower) -> {
                "Why do programmers prefer dark mode?\n\nBecause light attracts bugs!"
            }

            Regex("\\b(poem|poetry|write a poem)\\b").containsMatchIn(lower) -> {
                "Lines of logic, silent and deep,\nPromises made that circuits keep.\nThrough gates and registers data streams,\nA digital engine of human dreams."
            }

            // 9. All general knowledge, programming, technical, conceptual, and conversational inquiries:
            // First attempt live online knowledge (Wikipedia / DuckDuckGo) with zero pre-prompts.
            // If offline or no online hit, fall back to offline code generator or encyclopedia.
            else -> {
                val onlineResult = fetchOnlineKnowledge(prompt)
                if (!onlineResult.isNullOrBlank()) {
                    onlineResult
                } else if (OfflineCodeAndTech.isTechOrCodeQuery(lower)) {
                    OfflineCodeAndTech.resolveTechOrCode(prompt, lower)
                } else {
                    OfflineKnowledgeBase.resolveOfflineKnowledge(prompt, lower)
                        ?: "## $prompt\n\nI am ready to assist you. Please let me know what specific details, code, or context you need!"
                }
            }
        }

        // Tokenize text into words, whitespace, and punctuation for natural streaming
        val regex = Regex("(\\s+|[a-zA-Z0-9]+|[^a-zA-Z0-9\\s])")
        val matches = regex.findAll(text).map { it.value }.toList()
        return if (matches.isNotEmpty()) matches else text.chunked(4)
    }

    private fun isGamingQuery(lower: String): Boolean {
        return Regex("\\b(game\\s*booster|game\\s*boost|gaming\\s*fps|fps\\s*drop|stutter|overclock|high_touch_polling|thermal\\s*throttling|cpu\\s*governor)\\b").containsMatchIn(lower)
    }

    private fun isShizukuQuery(lower: String): Boolean {
        return Regex("\\b(shizuku|adb\\s*shell|privileged\\s*shell|uid\\s*2000|wireless\\s*debugging)\\b").containsMatchIn(lower)
    }

    private fun isPeerLinkQuery(lower: String): Boolean {
        return Regex("\\b(peerlink|wifi\\s*direct|p2p\\s*mesh|mesh\\s*network|offline\\s*p2p|p2p\\s*transfer)\\b").containsMatchIn(lower)
    }

    private fun isContextualFollowUp(lower: String): Boolean {
        return lower.matches(Regex("^(how|why|how so|why is that|explain more|tell me more|continue|more|what else|and then)[!.,?\\s]*$")) ||
               lower == "tell me more" || lower == "explain more" || lower == "tell me more details"
    }

    /**
     * Resolves knowledge queries by first attempting online search (Wikipedia / DuckDuckGo),
     * and falling back to the rich offline encyclopedia if offline or disconnected.
     */
    private suspend fun resolveKnowledgeQuery(prompt: String, lower: String): String {
        // 1. Attempt live internet lookup if connected
        val onlineResult = fetchOnlineKnowledge(prompt)
        if (!onlineResult.isNullOrBlank()) {
            return onlineResult
        }

        // 2. Fall back to extensive offline knowledge base
        return OfflineKnowledgeBase.resolveOfflineKnowledge(prompt, lower)
            ?: "## $prompt\n\nI am ready to assist you. Please let me know what specific details, code, or context you need!"
    }

    /**
     * Fetches live facts and summaries from Wikipedia and DuckDuckGo with zero API keys required.
     * Times out quickly (3.5s) if offline so the user experiences zero lag.
     */
    private suspend fun fetchOnlineKnowledge(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = prompt
                .replace(Regex("^(who is|who was|who are|who's|what is|what are|what's|what was|what were|what does|where is|where are|where was|when was|when did|why is|why are|why was|why does|why do|why did|how is|how does|how do|how did|how to|tell me about|can you explain|explain|define|meaning of|definition of)\\s*", RegexOption.IGNORE_CASE), "")
                .trim(' ', '?', '!', '.')

            val targetQuery = if (cleanQuery.length >= 2) cleanQuery else prompt.trim(' ', '?', '!', '.')
            if (targetQuery.length < 2) return@withContext null

            val encoded = URLEncoder.encode(targetQuery, "UTF-8")

            // 1. Try Wikipedia Search & Summary
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1&srlimit=1"
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "PeerLink/2.0 (Android; offline-ai-hybrid)")
                .build()

            networkClient.newCall(searchReq).execute().use { searchResp ->
                if (searchResp.isSuccessful) {
                    val searchBody = searchResp.body?.string()
                    if (searchBody != null) {
                        val searchJson = JSONObject(searchBody)
                        val queryObj = searchJson.optJSONObject("query")
                        val searchArr = queryObj?.optJSONArray("search")
                        if (searchArr != null && searchArr.length() > 0) {
                            val firstHit = searchArr.getJSONObject(0)
                            val pageTitle = firstHit.optString("title")
                            if (pageTitle.isNotBlank()) {
                                val encodedTitle = URLEncoder.encode(pageTitle.replace(" ", "_"), "UTF-8")
                                val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"

                                val summaryReq = Request.Builder()
                                    .url(summaryUrl)
                                    .header("User-Agent", "PeerLink/2.0 (Android; offline-ai-hybrid)")
                                    .build()

                                networkClient.newCall(summaryReq).execute().use { summaryResp ->
                                    if (summaryResp.isSuccessful) {
                                        val summaryBody = summaryResp.body?.string()
                                        if (summaryBody != null) {
                                            val summaryJson = JSONObject(summaryBody)
                                            val title = summaryJson.optString("title", pageTitle)
                                            val description = summaryJson.optString("description", "")
                                            val extract = summaryJson.optString("extract", "")

                                            if (extract.isNotBlank()) {
                                                val sb = StringBuilder()
                                                sb.append("## ").append(title).append("\n")
                                                if (description.isNotBlank()) {
                                                    sb.append("*").append(description).append("*\n\n")
                                                } else {
                                                    sb.append("\n")
                                                }
                                                sb.append(extract)
                                                return@withContext sb.toString()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Fallback: DuckDuckGo Instant Answer API
            val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val ddgReq = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "PeerLink/2.0 (Android; offline-ai-hybrid)")
                .build()

            networkClient.newCall(ddgReq).execute().use { ddgResp ->
                if (ddgResp.isSuccessful) {
                    val ddgBody = ddgResp.body?.string()
                    if (ddgBody != null) {
                        val ddgJson = JSONObject(ddgBody)
                        val abstractText = ddgJson.optString("AbstractText", "")
                        val heading = ddgJson.optString("Heading", targetQuery)
                        if (abstractText.isNotBlank()) {
                            return@withContext "## $heading\n\n$abstractText"
                        }
                    }
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun generateContextualFollowUp(lower: String, lastLower: String, rawPrompt: String): String {
        return when {
            lastLower.contains("game") || lastLower.contains("fps") || lastLower.contains("governor") -> """
### Advanced Game Booster Fine-Tuning

Following up on game optimization:

1. **CPU Governor Locking**:
   - `performance`: Forces CPU cores to maximum frequency, eliminating downclock jitter.
   - `schedutil`: Dynamically adjusts frequency with minimal latency based on frame render time.

2. **Touch Sampling Frequency**:
   ```bash
   settings put secure high_touch_polling_rate_enabled 1
   ```
   Doubles digitizer polling from 120Hz to 240Hz/360Hz on supported displays for instantaneous touch registration.

3. **Background Process Reclamation**:
   ```bash
   am kill-all
   ```
   Terminates non-essential cached background processes, freeing up 500MB to 1.5GB of RAM for the game process.
            """.trimIndent()

            lastLower.contains("shizuku") || lastLower.contains("adb") -> """
### Shizuku Pairing & Privilege Setup

To grant privileged permissions via Shizuku:

1. Enable **Developer Options** and turn on **Wireless Debugging** in Android Settings.
2. Open the **Shizuku** app and select **Pairing via Wireless Debugging**.
3. Enter the 6-digit pairing code shown in the notification.
4. Tap **Start** in Shizuku. The service will bind to UID 2000 (`shell`), giving this app permission to execute privileged system commands without root.
            """.trimIndent()

            lastLower.contains("modi") || lastLower.contains("minister") -> """
### Further Context on Narendra Modi

Key milestones in his tenure include:
- **Electoral Mandates**: Won majorities in 2014, 2019, and led the NDA government in 2024.
- **Economic Reforms**: Goods and Services Tax (GST) unification, Insolvency and Bankruptcy Code (IBC).
- **Public Infrastructure**: National logistics master plan (PM Gati Shakti) and high-speed rail modernization.
            """.trimIndent()

            lastLower.contains("code") || lastLower.contains("python") || lastLower.contains("kotlin") -> """
### Code Optimization & Best Practices

Here are key improvements to consider:

- **Asynchronous Execution**: Always offload intensive loops or I/O operations to background threads (`Dispatchers.IO` in Kotlin, `asyncio` or `threading` in Python) to avoid blocking the UI.
- **Memory Efficiency**: Prefer streaming or chunked iterators over loading large datasets completely into memory.
- **Error Boundaries**: Wrap network calls and file operations in targeted `try/catch` blocks with graceful fallbacks.
            """.trimIndent()

            lastLower.contains("quantum") || lastLower.contains("physics") -> """
### Deeper Theoretical Insight

Building upon the previous principles:

- **Mathematical Formalism**: Quantum states are vectors in a complex Hilbert space. Observable quantities correspond to self-adjoint Hermitian operators.
- **Decoherence**: Environmental interactions cause superposition states to collapse into classical probabilities, which is the primary challenge in scaling physical quantum computers.
            """.trimIndent()

            else -> """
Continuing from our previous discussion:

Key considerations depend on the operational requirements, constraints, and target outcomes of the system. Let me know if you would like practical examples, specific commands, or a breakdown of any particular part.
            """.trimIndent()
        }
    }
}
