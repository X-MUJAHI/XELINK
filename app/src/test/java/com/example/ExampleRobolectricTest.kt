package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.StoragePermissionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PeerLink", appName)
    }

    @Test
    fun `test storage permission helper`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val description = StoragePermissionHelper.getStatusDescription(context)
        assertNotNull(description)
        assertTrue(description.isNotEmpty())

        val legacyPerms = StoragePermissionHelper.getLegacyStoragePermissions()
        assertTrue(legacyPerms.isNotEmpty())
    }
}
