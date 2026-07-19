import nexus.DownloadRequest
import nexus.parseDownloadRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class NexusClientTest {

    @Test
    fun basic(){
        val actual = parseDownloadRequest("nxm://starfield/mods/4183/files/12955?key=abcdefghijklmnop&expires=1697023374&user_id=111111")
        val expected = DownloadRequest(4183, 12955, "abcdefghijklmnop", "1697023374")

        assertEquals(expected, actual)
    }
}
