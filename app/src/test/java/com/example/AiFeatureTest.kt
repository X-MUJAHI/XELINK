package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiManager
import com.example.ui.screens.AiScreen
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiFeatureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testVariousPromptPairs() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val pairs = listOf(
            "Hello" to "Calculate 124 * 85",
            "Game booster" to "how to boost fps"
        )

        for ((p1, p2) in pairs) {
            val aiManager = AiManager(context)
            ShadowLooper.idleMainLooper()
            aiManager.createNewChat()
            ShadowLooper.idleMainLooper()

            aiManager.sendMessage(p1)
            for (i in 0..30) {
                ShadowLooper.idleMainLooper()
                Thread.sleep(15)
            }

            aiManager.sendMessage(p2)
            for (i in 0..30) {
                ShadowLooper.idleMainLooper()
                Thread.sleep(15)
            }

            val session = aiManager.activeSession.value
            assertNotNull(session)
            assertTrue("Expected at least 4 messages in session", (session?.messages?.size ?: 0) >= 4)
        }
    }

    @Test
    fun testModelDeletionRemovesFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val aiManager = AiManager(context)
        ShadowLooper.idleMainLooper()

        val modelsDir = aiManager.downloader.getModelsDirectory()
        val dummyModelFile = java.io.File(modelsDir, "SmolLM2-135M-Instruct.Q4_K_M.gguf")
        dummyModelFile.writeBytes(ByteArray(1024) { 0x42 })
        assertTrue(dummyModelFile.exists())

        // Execute deletion
        val deleted = aiManager.deleteModel("smollm2_135m")
        assertTrue("Model file should be deleted", deleted)
        assertTrue("Physical .gguf file must no longer exist", !dummyModelFile.exists())
    }

    @Test
    fun testAiScreenComposeTwoPrompts() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        composeTestRule.setContent {
            AiScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        viewModel.aiManager.sendMessage("Hello")
        for (i in 0..40) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(30)
            composeTestRule.waitForIdle()
        }

        viewModel.aiManager.sendMessage("Tell me more about game booster")
        for (i in 0..40) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(30)
            composeTestRule.waitForIdle()
        }

        val session = viewModel.aiManager.activeSession.value
        assertNotNull(session)
        assertTrue((session?.messages?.size ?: 0) >= 4)
    }
}
