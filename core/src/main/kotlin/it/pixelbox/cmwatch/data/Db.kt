package it.pixelbox.cmwatch.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.State

@Entity(tableName = "state") data class StateRow(@PrimaryKey val id: Int = 1, val json: String, val receivedAt: Long)
@Entity(tableName = "events") data class EventRow(@PrimaryKey val key: String, val ts: Long, val session: String?, val json: String)
@Entity(tableName = "pending") data class PendingRow(@PrimaryKey val id: String, val issued: Long, val json: String)

@Dao interface CmDao {
    @Query("SELECT * FROM state WHERE id = 1") suspend fun state(): StateRow?
    @Upsert suspend fun putState(row: StateRow)
    @Query("SELECT * FROM events ORDER BY ts DESC") suspend fun events(): List<EventRow>
    @Upsert suspend fun putEvents(rows: List<EventRow>)
    @Query("DELETE FROM events WHERE ts < :olderThan") suspend fun pruneEvents(olderThan: Long)
    @Query("SELECT * FROM pending ORDER BY issued") suspend fun pending(): List<PendingRow>
    @Query("DELETE FROM pending") suspend fun clearPending()
    @Insert suspend fun putPending(rows: List<PendingRow>)
}

@Database(entities = [StateRow::class, EventRow::class, PendingRow::class], version = 1, exportSchema = true)
abstract class Db : RoomDatabase() {
    abstract fun dao(): CmDao

    companion object {
        fun open(ctx: Context): Db =
            Room.databaseBuilder(ctx, Db::class.java, "cmwatch.db").fallbackToDestructiveMigration(true).build()
    }
}

class RoomStore(private val dao: CmDao) : Store {
    companion object {
        /** L'unico punto in cui gli altri moduli toccano Room. */
        fun open(ctx: Context): Store = RoomStore(Db.open(ctx).dao())
    }

    override suspend fun loadState() = dao.state()?.let { ContractJson.decodeState(it.json) to it.receivedAt }
    override suspend fun saveState(s: State, receivedAt: Long) =
        dao.putState(StateRow(1, ContractJson.json.encodeToString(State.serializer(), s), receivedAt))
    override suspend fun loadEvents() = dao.events().map { ContractJson.json.decodeFromString(Event.serializer(), it.json) }
    override suspend fun saveEvents(ev: List<Event>) =
        dao.putEvents(ev.map { EventRow(it.key, it.ts, it.session, ContractJson.json.encodeToString(Event.serializer(), it)) })
    override suspend fun pruneEvents(olderThan: Long) = dao.pruneEvents(olderThan)
    override suspend fun loadPending() = dao.pending().map { ContractJson.json.decodeFromString(Cmd.serializer(), it.json) }
    override suspend fun savePending(c: List<Cmd>) {
        dao.clearPending()
        dao.putPending(c.map { PendingRow(it.id, it.issued, ContractJson.encode(it)) })
    }
}
