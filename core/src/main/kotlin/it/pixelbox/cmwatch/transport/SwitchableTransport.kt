package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/** Un Transport che si può sostituire a caldo (dopo il pairing: dal finto a Firebase) senza ricreare il Repo. */
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchableTransport(initial: Transport) : Transport {
    private val current = MutableStateFlow(initial)
    val active: Transport get() = current.value

    fun switchTo(t: Transport) { current.value = t }

    override val state: Flow<State> = current.flatMapLatest { it.state }
    override val events: Flow<List<Event>> = current.flatMapLatest { it.events }
    override suspend fun fetchState(): State = current.value.fetchState()
    override suspend fun send(cmd: Cmd): CmdResult = current.value.send(cmd)
    override suspend fun pair(code: String, deviceName: String): PairingInfo = current.value.pair(code, deviceName)
}
