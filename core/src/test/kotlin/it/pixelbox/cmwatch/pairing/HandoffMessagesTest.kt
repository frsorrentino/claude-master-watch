package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class HandoffMessagesTest {
    @Test fun helloCarriesTheCompactConfig() {
        val req = HelloRequest(f = QrFirebase("k", "p", "a", "https://d", "watch"))
        val s = String(HandoffMessages.encode(HelloRequest.serializer(), req))
        assertEquals("""{"v":1,"f":{"k":"k","p":"p","a":"a","d":"https://d","t":"watch"}}""", s)
        assertEquals(req, HandoffMessages.decode(HelloRequest.serializer(), s.toByteArray()))
    }

    @Test fun repliesOmitNulls() {
        assertEquals("""{"v":1,"uid":"u","name":"n","eph":"e","restart":false}""",
            String(HandoffMessages.encode(HelloResponse.serializer(), HelloResponse(uid = "u", name = "n", eph = "e"))))
        assertEquals("""{"v":1,"restart":true}""", String(HandoffMessages.encode(HelloResponse.serializer(), HelloResponse(restart = true))))
        assertEquals("""{"v":1,"ok":false,"error":"decrypt"}""", String(HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(error = "decrypt"))))
    }

    @Test fun garbageDecodesToNull() = assertNull(HandoffMessages.decode(KeyRequest.serializer(), "nope".toByteArray()))
}
