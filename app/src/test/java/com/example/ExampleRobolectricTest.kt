package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.DemoRouterOSClient
import com.example.data.routeros.HotspotUserModel
import com.example.ui.components.formatAriary
import com.example.ui.components.formatBytes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        assertEquals("MIKROTECH 301", appName)
    }

    @Test
    fun `formatAriary formats numbers correctly`() {
        val formatted = formatAriary(1000)
        assertTrue(formatted.contains("1") && formatted.contains("000") && formatted.endsWith("Ar"))
    }

    @Test
    fun `formatBytes formats sizes accurately`() {
        assertEquals("1.0 KB", formatBytes(1024))
        assertEquals("1.0 MB", formatBytes(1024 * 1024))
        assertEquals("1.5 GB", formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `demo routeros client crud operations`() = runBlocking {
        val client = DemoRouterOSClient()
        val testRes = client.testConnection()
        assertTrue(testRes.isSuccess)
        assertEquals("hAP ac² (RBD52G-5HacD2HnD)", testRes.getOrNull()?.boardName)

        val usersRes = client.getHotspotUsers()
        assertTrue(usersRes.isSuccess)
        val initialCount = usersRes.getOrNull()?.size ?: 0

        // Add user
        val addRes = client.addHotspotUser(
            HotspotUserModel(
                name = "test_unit_user",
                password = "123",
                profile = "1H_500Ar",
                limitUptime = "1h"
            )
        )
        assertTrue(addRes.isSuccess)

        val updatedUsers = client.getHotspotUsers().getOrNull() ?: emptyList()
        assertEquals(initialCount + 1, updatedUsers.size)
        assertTrue(updatedUsers.any { it.name == "test_unit_user" })
    }
}
