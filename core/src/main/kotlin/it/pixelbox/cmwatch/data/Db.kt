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
import it.pixelbox.cmwatch.rules.QuotaHistory

@Entity(tableName = "state") data class StateRow(@PrimaryKey val id: Int = 1, val json: String, val receivedAt: Long)
@Entity(tableName = "events") data class EventRow(@PrimaryKey val key: String, val ts: Long, val session: String?, val json: String)
@Entity(tableName = "pending") data class PendingRow(@PrimaryKey val id: String, val issued: Long, val json: String)
/** Un campione della quota delle 5 ore (versione 2, 16/09): la linea del ritmo della finestra. */
@Entity(tableName = "quota_samples", primaryKeys = ["account", "ts"]) data class QuotaSampleRow(val account: String, val ts: Long, val pct: Int)

@Dao interface CmDao {
    @Query("SELECT * FROM state WHERE id = 1") suspend fun state(): StateRow?
    @Upsert suspend fun putState(row: StateRow)
    @Query("SELECT * FROM events ORDER BY ts DESC") suspend fun events(): List<EventRow>
    @Upsert suspend fun putEvents(rows: List<EventRow>)
    @Query("DELETE FROM events WHERE ts < :olderThan") suspend fun pruneEvents(olderThan: Long)
    @Query("SELECT * FROM pending ORDER BY issued") suspend fun pending(): List<PendingRow>
    @Query("DELETE FROM pending") suspend fun clearPending()
    @Insert suspend fun putPending(rows: List<PendingRow>)
    @Upsert suspend fun putQuotaSample(row: QuotaSampleRow)
    @Query("SELECT * FROM quota_samples WHERE ts >= :since ORDER BY ts") suspend fun quotaSamples(since: Long): List<QuotaSampleRow>
    @Query("DELETE FROM quota_samples WHERE ts < :olderThan") suspend fun pruneQuotaSamples(olderThan: Long)
    @Query("DELETE FROM quota_samples WHERE account = :account") suspend fun clearQuotaSamples(account: String)
}

@Database(entities = [StateRow::class, EventRow::class, PendingRow::class, QuotaSampleRow::class], version = 2, exportSchema = true)
abstract class Db : RoomDatabase() {
    abstract fun dao(): CmDao

    companion object {
        /**
         * 1 → 2 aggiunge solo la tabella dei campioni. Migrazione scritta invece di lasciar ricreare il database: la
         * ricreazione avrebbe buttato i 7 giorni di eventi da cui si disegna «Oggi».
         */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `quota_samples` (`account` TEXT NOT NULL, `ts` INTEGER NOT NULL, `pct` INTEGER NOT NULL, PRIMARY KEY(`account`, `ts`))")
            }
        }

        fun open(ctx: Context): Db =
            Room.databaseBuilder(ctx, Db::class.java, "cmwatch.db").addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration(true).build()
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
    override suspend fun saveQuotaSample(account: String, sample: QuotaHistory.Sample) =
        dao.putQuotaSample(QuotaSampleRow(account, sample.ts, sample.pct))
    override suspend fun loadQuotaSamples(since: Long): Map<String, List<QuotaHistory.Sample>> = dao.quotaSamples(since)
        .groupBy({ row -> row.account }, { row -> QuotaHistory.Sample(row.ts, row.pct) })
    override suspend fun pruneQuotaSamples(olderThan: Long) = dao.pruneQuotaSamples(olderThan)
    override suspend fun clearQuotaSamples(account: String) = dao.clearQuotaSamples(account)
}
