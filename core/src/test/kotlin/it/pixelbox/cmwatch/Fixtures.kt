package it.pixelbox.cmwatch

import java.io.File

object Fixtures {
    private val dir = File("../contract")
    fun read(name: String): String = File(dir, name).readText()
    val stateQuestion get() = read("state-1-question.json")
    val stateIdle get() = read("state-2-idle.json")
    val stateStale get() = read("state-3-stale.json")
    val events get() = read("events-sample.json")
    val cmdResult get() = read("cmd-result-sample.json")
}
