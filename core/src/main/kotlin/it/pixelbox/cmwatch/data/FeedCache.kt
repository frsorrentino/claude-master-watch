package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.TranscriptEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.PairSerializer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Le conversazioni già lette, tenute dal processo e salvate su disco (piano prestazioni, Task 13): stavano in un `remember`
 * della composizione, e a ogni ricreazione della finestra (Chromebook ridimensionato, rotazione, tema) le chat ripartivano
 * vuote con una lettura completa per colonna. Un file per sessione, le ultime [KEEP] voci, scritto in modo atomico
 * (file temporaneo e rename) come ChatLog; si riscrive solo una sessione cambiata.
 */
class FeedCache(private val dir: File) {
    private val mem = ConcurrentHashMap<String, List<TranscriptEntry>>()
    private val serializer = PairSerializer(String.serializer(), ListSerializer(TranscriptEntry.serializer()))

    init {
        dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }.orEmpty().forEach { f ->
            runCatching { ContractJson.json.decodeFromString(serializer, f.readText()) }.getOrNull()?.let { (name, list) -> mem[name] = list }
        }
    }

    fun all(): Map<String, List<TranscriptEntry>> = HashMap(mem)

    /** Le conversazioni di adesso: si scrivono solo quelle cambiate, con le ultime [KEEP] voci. */
    fun save(feeds: Map<String, List<TranscriptEntry>>) {
        dir.mkdirs()
        for ((name, list) in feeds) {
            val kept = list.takeLast(KEEP)
            if (mem[name] == kept) continue
            mem[name] = kept
            val f = File(dir, fileName(name))
            val tmp = File(dir, f.name + ".tmp")
            runCatching {
                tmp.writeText(ContractJson.json.encodeToString(serializer, name to kept))
                if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            }
        }
    }

    private fun fileName(name: String) = java.net.URLEncoder.encode(name, "UTF-8").replace("*", "%2A") + ".json"

    companion object {
        const val KEEP = 50
    }
}
