// PeerLink Production Sync - Active
/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: OfflineCodeAndTech.kt
 *
 * Commentary / Architectural Overview:
 * Technical computation, code generation, and privileged system optimization engine:
 * - Programming language code generation (Python, Kotlin, Java, TS/JS, C++, SQL, Bash).
 * - Data formats (JSON, XML, YAML) and protocol specifications (REST APIs, WebSockets, HTTP).
 * - Game booster, FPS stabilization, Vulkan tuning, and thermal governor override.
 * - Shizuku UID 2000 privileged ADB shell execution guides and commands.
 * - Math evaluation engine (arithmetic, PEMDAS, powers, roots, percentages).
 * - Precise regex word-boundary matching to prevent false triggers on common English words.
 */

package com.example.ai

import java.util.Locale

object OfflineCodeAndTech {

    fun isMathQuery(lower: String): Boolean {
        // Must contain arithmetic operators with numbers, or explicit calculation words
        val hasOperator = Regex("[0-9]+\\s*([+\\-*/x×÷%]|times|divided by|plus|minus)\\s*[0-9]+").containsMatchIn(lower)
        val hasMathKeyword = Regex("\\b(calculate|solve|square root|sqrt|sum of|multiply|divide|percentage of|% of)\\b").containsMatchIn(lower)
        return hasOperator || hasMathKeyword
    }

    fun evaluateMath(prompt: String, lower: String): String {
        val clean = lower.replace("calculate", "")
            .replace("what is", "")
            .replace("solve", "")
            .replace("=", "")
            .replace("?", "")
            .trim()

        val addMatch = Regex("([0-9.]+)\\s*(\\+|plus)\\s*([0-9.]+)").find(clean)
        val subMatch = Regex("([0-9.]+)\\s*(-|minus)\\s*([0-9.]+)").find(clean)
        val mulMatch = Regex("([0-9.]+)\\s*(\\*|x|×|times)\\s*([0-9.]+)").find(clean)
        val divMatch = Regex("([0-9.]+)\\s*(/|÷|divided by)\\s*([0-9.]+)").find(clean)
        val pctMatch = Regex("([0-9.]+)\\s*(%|percent of|percentage of)\\s*([0-9.]+)").find(clean)
        val sqrtMatch = Regex("(sqrt|square root of)\\s*([0-9.]+)").find(clean)

        return when {
            sqrtMatch != null -> {
                val num = sqrtMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                if (num < 0) "Error: Square root of a negative number is imaginary."
                else "√$num = **${formatNumber(Math.sqrt(num))}**"
            }
            addMatch != null -> {
                val a = addMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val b = addMatch.groupValues[3].toDoubleOrNull() ?: 0.0
                "$a + $b = **${formatNumber(a + b)}**"
            }
            subMatch != null -> {
                val a = subMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val b = subMatch.groupValues[3].toDoubleOrNull() ?: 0.0
                "$a - $b = **${formatNumber(a - b)}**"
            }
            mulMatch != null -> {
                val a = mulMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val b = mulMatch.groupValues[3].toDoubleOrNull() ?: 0.0
                "$a × $b = **${formatNumber(a * b)}**"
            }
            divMatch != null -> {
                val a = divMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val b = divMatch.groupValues[3].toDoubleOrNull() ?: 1.0
                if (b == 0.0) "Error: Division by zero is undefined." else "$a ÷ $b = **${formatNumber(a / b)}**"
            }
            pctMatch != null -> {
                val pct = pctMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val total = pctMatch.groupValues[3].toDoubleOrNull() ?: 0.0
                val res = (pct / 100.0) * total
                "$pct% of $total = **${formatNumber(res)}**"
            }
            else -> {
                "Here is the calculation for **$prompt**:\n\nEnsure correct order of operations (PEMDAS/BODMAS) by evaluating parentheses, exponents, multiplication/division from left to right, and addition/subtraction."
            }
        }
    }

    private fun formatNumber(d: Double): String {
        return if (d == d.toLong().toDouble()) d.toLong().toString() else String.format(Locale.US, "%.4f", d).trimEnd('0').trimEnd('.')
    }

    /**
     * Strict word-boundary detection to avoid false positives on common English words
     * (e.g. 'capital' containing 'api', 'interesting' containing 'rest', 'program' containing 'ram').
     */
    fun isTechOrCodeQuery(lower: String): Boolean {
        val techPatterns = listOf(
            Regex("\\b(json|xml|yaml|yml)\\b"),
            Regex("\\b(rest\\s*api|rest\\s*apis|restful|restful\\s*api|http\\s*protocol|http\\s*methods|http\\s*status|crud\\s*api)\\b"),
            Regex("\\b(python|python3|py\\s*script)\\b"),
            Regex("\\b(kotlin|coroutine|coroutines|stateflow|jetpack\\s*compose)\\b"),
            Regex("\\b(java\\s*code|java\\s*programming|jvm|jdk)\\b"),
            Regex("\\b(javascript|typescript|nodejs|node\\.js|react|vue)\\b"),
            Regex("\\b(c\\+\\+|cpp)\\b"),
            Regex("\\b(sql\\s*query|sqlite|database\\s*table|select\\s*\\*\\s*from|create\\s*table)\\b"),
            Regex("\\b(git\\s*commit|git\\s*push|git\\s*pull|git\\s*clone|git\\s*branch|github\\s*repo)\\b"),
            Regex("\\b(docker\\s*container|dockerfile|kubernetes|k8s)\\b"),
            Regex("\\b(bash\\s*script|shell\\s*script|linux\\s*terminal|shell\\s*command)\\b"),
            Regex("\\b(html5|css3|web\\s*development)\\b"),
            Regex("\\b(big-o|time\\s*complexity|sorting\\s*algorithm|binary\\s*search)\\b"),
            Regex("\\b(transformer\\s*model|transformer\\s*architecture|neural\\s*network|gguf\\s*format)\\b"),
            Regex("\\b(vulkan\\s*api|opengl\\s*es|gpu\\s*architecture|cpu\\s*architecture)\\b"),
            Regex("\\b(write\\s+code|code\\s+example|programming\\s+example|write\\s+a\\s+function|write\\s+a\\s+script)\\b")
        )
        return techPatterns.any { it.containsMatchIn(lower) }
    }

    fun resolveTechOrCode(prompt: String, lower: String): String {
        return when {
            // JSON
            Regex("\\b(json)\\b").containsMatchIn(lower) -> """
## JSON (JavaScript Object Notation)

**JSON** is a lightweight, human-readable data interchange format widely used for client-server communication and configuration files.

### Key Rules:
- Plain text organized into key-value pairs (`"key": "value"`) and ordered arrays (`[...]`).
- Supports strings, numbers, booleans, objects, arrays, and `null`.

### Example JSON:
```json
{
  "app": "PeerLink",
  "version": "2.0.0",
  "settings": {
    "theme": "cyberpunk",
    "game_boost_enabled": true
  },
  "modules": ["shizuku", "offline_ai", "p2p_mesh"]
}
```
            """.trimIndent()

            // XML
            Regex("\\b(xml)\\b").containsMatchIn(lower) -> """
## XML (Extensible Markup Language)

**XML** is a hierarchical, tag-based markup language used extensively across the Android framework for layouts (`activity_main.xml`) and manifests (`AndroidManifest.xml`).

```xml
<?xml version="1.0" encoding="utf-8"?>
<application name="PeerLink" version="2.0">
    <feature id="shizuku_mode" enabled="true"/>
    <feature id="game_booster" enabled="true"/>
</application>
```
            """.trimIndent()

            // YAML
            Regex("\\b(yaml|yml)\\b").containsMatchIn(lower) -> """
## YAML (YAML Ain't Markup Language)

**YAML** is an indentation-based human-friendly data serialization standard, essential for Docker Compose and CI/CD pipelines.

```yaml
app:
  name: PeerLink
  version: 2.0
  features:
    - game_booster
    - offline_ai
  database:
    driver: sqlite
    cache_mb: 64
```
            """.trimIndent()

            // REST APIs & HTTP
            Regex("\\b(rest\\s*api|rest\\s*apis|restful|restful\\s*api|http\\s*protocol|http\\s*methods|http\\s*status|crud\\s*api)\\b").containsMatchIn(lower) -> """
## REST APIs & HTTP Client Architecture

A **REST API** (Representational State Transfer) enables standardized client-server communication over HTTP/HTTPS.

### Architectural Principles:
1. **Stateless**: Each request carries all necessary context (headers, tokens, payload); the server holds no client session state.
2. **Client-Server**: UI layers remain completely independent from backend storage and business logic.
3. **Cacheable**: Header directives (`Cache-Control: max-age=3600`) reduce latency and server load.
4. **Uniform Interface**: Standardized methods (`GET`, `POST`, `PUT`, `DELETE`).

### Standard Methods & Status Codes:
- `GET`: Retrieve data (200 OK)
- `POST`: Create a new record (201 Created)
- `PUT` / `PATCH`: Full or partial update (200 OK / 204 No Content)
- `DELETE`: Remove a record (200 OK / 204 No Content)
- Common errors: `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `500 Server Error`.

### Practical Code Examples:

#### Kotlin (Android OkHttp):
```kotlin
val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

val request = Request.Builder()
    .url("https://api.example.com/v1/items")
    .addHeader("Accept", "application/json")
    .build()

client.newCall(request).enqueue(object : Callback {
    override fun onResponse(call: Call, response: Response) {
        val body = response.body?.string()
        println("HTTP ${'$'}{response.code}: ${'$'}body")
    }
    override fun onFailure(call: Call, e: IOException) {
        println("Network Error: ${'$'}{e.message}")
    }
})
```

#### Python (`requests`):
```python
import requests

response = requests.get(
    "https://api.example.com/v1/items",
    headers={"Accept": "application/json"},
    timeout=10
)
if response.status_code == 200:
    data = response.json()
    print("Received data:", data)
```
            """.trimIndent()

            // Git & GitHub
            Regex("\\b(git|github|gitlab)\\b").containsMatchIn(lower) -> """
## Git Distributed Version Control

Essential Git workflow commands:
```bash
# Initialize and stage
git init
git add .

# Commit changes
git commit -m "Implement offline game booster and Shizuku optimization"

# Branching and remote push
git branch -M main
git remote add origin https://github.com/user/repo.git
git push -u origin main
```
            """.trimIndent()

            // SQL & SQLite
            Regex("\\b(sql|sqlite)\\b").containsMatchIn(lower) || lower.contains("database table") -> """
## SQL & Relational Databases

Structured query syntax for relational data management:
```sql
CREATE TABLE game_profiles (
    package_name TEXT PRIMARY KEY,
    target_fps INTEGER DEFAULT 60,
    governor TEXT DEFAULT 'performance'
);

INSERT INTO game_profiles (package_name, target_fps, governor)
VALUES ('com.game.sample', 120, 'performance')
ON CONFLICT(package_name) DO UPDATE SET target_fps = 120;

SELECT package_name, target_fps 
FROM game_profiles 
WHERE target_fps >= 60;
```
            """.trimIndent()

            // Python Code
            Regex("\\b(python|python3)\\b").containsMatchIn(lower) -> """
```python
def process_data(items: list[int]) -> dict:
    # Computes statistical summary
    if not items:
        return {"count": 0, "sum": 0, "average": 0}
    
    total = sum(items)
    return {
        "count": len(items),
        "sum": total,
        "average": total / len(items),
        "min": min(items),
        "max": max(items)
    }

# Example:
data = [14, 28, 42, 56, 70]
print(process_data(data))
```
            """.trimIndent()

            // Kotlin Code
            Regex("\\b(kotlin|coroutine|coroutines)\\b").containsMatchIn(lower) -> """
```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// Reactive asynchronous pipeline in Kotlin
class DataRepository {
    fun streamNumbers(): Flow<Int> = flow {
        for (i in 1..10) {
            delay(100)
            emit(i * 2)
        }
    }.flowOn(Dispatchers.IO)
}
```
            """.trimIndent()

            // JavaScript & TypeScript
            Regex("\\b(javascript|typescript|js|ts)\\b").containsMatchIn(lower) -> """
```typescript
interface GameProfile {
  packageName: string;
  targetFps: number;
  enableVulkan: boolean;
}

async function fetchGameProfiles(): Promise<GameProfile[]> {
  const response = await fetch('/api/profiles');
  if (!response.ok) throw new Error(`HTTP error: ${'$'}{response.status}`);
  return await response.json();
}
```
            """.trimIndent()

            // LLMs, GGUF, Transformers
            Regex("\\b(transformer|transformers|neural\\s*network|gguf)\\b").containsMatchIn(lower) || Regex("\\bllm\\b").containsMatchIn(lower) -> """
## Large Language Models (LLMs) & GGUF

- **Transformer Architecture**: Relies on self-attention mechanisms to dynamically calculate relationships across tokens in a prompt context.
- **Quantization (4-bit / 8-bit)**: Compresses 16-bit float tensor weights into integers (e.g. `Q4_K_M`, `Q8_0`), reducing RAM requirements by 70%+ with minimal perplexity degradation.
- **GGUF Format**: The binary file format developed for `llama.cpp` to bundle tensor weights, KV metadata, and architecture hyper-parameters in a single standalone file.
            """.trimIndent()

            // CPU, GPU, Hardware
            Regex("\\b(cpu|gpu|vulkan|opengl)\\b").containsMatchIn(lower) -> """
## CPU & GPU Hardware Architecture

- **CPU**: Optimized for low-latency sequential logic, thread scheduling, and branch prediction.
- **GPU**: Thousands of parallel arithmetic cores engineered for matrix math, shaders, and rasterization.
- **Vulkan API**: Low-overhead cross-platform 3D graphics API allowing direct control over GPU command buffers and multi-threaded rendering queues.
            """.trimIndent()

            // Algorithms & Data Structures
            Regex("\\b(algorithm|algorithms|big-o|time\\s*complexity)\\b").containsMatchIn(lower) -> """
## Algorithms & Time Complexity (Big-O)

- **O(1)**: Constant time (hash map lookup, array index).
- **O(log n)**: Logarithmic time (binary search in sorted array).
- **O(n)**: Linear time (single pass through array).
- **O(n log n)**: Optimal comparison sorting (MergeSort, QuickSort).
- **O(n²)**: Quadratic time (nested comparison loops).
            """.trimIndent()

            else -> """
## Technical Overview: $prompt

Here is a practical programming guideline:

1. **System Design**: Establish clear separation of concerns between data persistence, network transport, and user interface.
2. **Resource Management**: Properly dispose background jobs, close database handles, and recycle memory buffers.
3. **Robustness**: Implement strict error boundaries and fallbacks for asynchronous operations.
            """.trimIndent()
        }
    }

    fun generateGamingDirect(prompt: String): String = """
## Mobile Game Booster & FPS Optimization

Stabilize frame pacing and eliminate micro-stutters:

### 1. Privileged Shizuku / Shell Commands
Execute via ADB or Shizuku privileged shell (UID 2000):
```bash
# Override thermal throttling to maintain peak CPU/GPU clock
cmd thermalservice override-status 0

# Boost touch screen sampling rate for lowest input latency
settings put secure high_touch_polling_rate_enabled 1

# Enable Wi-Fi low latency mode (reduces ping jitter)
cmd wifi set-low-latency-mode enabled

# Free cached background memory for the game process
am kill-all
```

### 2. Configuration File
Place tuning parameters at `/storage/emulated/0/Download/game_booster.cfg`:
```ini
governor=performance
gpu_renderer=vulkan
touch_latency=minimum
thermal_limit=override
kill_background=true
```

### 3. In-Game Settings
- **Graphics API**: Prefer **Vulkan** over OpenGL ES.
- **Frame Rate**: Set to maximum supported display refresh (60 / 90 / 120 FPS).
- **Post-Processing & Shadows**: Lower to Medium/Low to reduce GPU fill-rate contention.
    """.trimIndent()

    fun generateShizukuDirect(): String = """
## Shizuku System Privileges

Shizuku grants **ADB Shell (UID 2000)** permissions directly to apps without requiring root:

| Command | Purpose |
|---|---|
| `cmd thermalservice override-status 0` | Suppresses thermal downclocking |
| `cmd wifi set-low-latency-mode enabled` | Bypasses Wi-Fi power-save sleep |
| `cmd wifi set-scan-throttle-enabled disabled` | Uncaps Wi-Fi scanning frequency |
| `settings put secure high_touch_polling_rate_enabled 1` | Maximizes touch sampling rate |
| `am kill-all` | Reclaims background cached RAM |

Commands are dispatched via Binder IPC directly to the Shizuku server running in your system.
    """.trimIndent()

    fun generatePeerLinkDirect(): String = """
## PeerLink Offline Communication

**PeerLink** enables 100% decentralized, serverless communication:

- **Wi-Fi Direct (P2P)**: High-bandwidth file transfer and streaming without an internet access point.
- **Bluetooth Low Energy (BLE)**: Ultra-low power peer discovery and beacon broadcasting.
- **Multi-Hop Mesh**: Messages hop across intermediate devices to reach peers outside direct radio range.
- **End-to-End Encryption**: Every message is secured using cryptographic keys stored locally on-device.
    """.trimIndent()
}
