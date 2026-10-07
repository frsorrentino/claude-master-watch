package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** «Copia» su un file della chat (Franz, 07/10 15:32): gli stessi casi di fileActions.test.ts nella web app. */
class FileActionsTest {
    @Test fun textFilesAreCopiedAsText() {
        for (m in listOf("text/plain", "text/markdown", "application/json", "application/xml", "image/svg+xml", "application/javascript", "text/csv; charset=utf-8"))
            assertEquals(m, FileActions.CopyKind.TEXT, FileActions.copyKind(m))
    }

    @Test fun everythingElseIsCopiedAsTheFile() {
        for (m in listOf("image/png", "image/jpeg", "application/pdf", "application/zip", "video/mp4", null))
            assertEquals(m, FileActions.CopyKind.FILE, FileActions.copyKind(m))
    }
}
