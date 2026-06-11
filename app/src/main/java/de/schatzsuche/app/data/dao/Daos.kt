package de.schatzsuche.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.schatzsuche.app.data.model.HuntSessionEntity
import de.schatzsuche.app.data.model.HuntSessionStatus
import de.schatzsuche.app.data.model.HuntStepEntity
import de.schatzsuche.app.data.model.QrCodeEntity
import de.schatzsuche.app.data.model.StepCompletionEntity
import de.schatzsuche.app.data.model.TreasureHuntEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrCodeDao {
    @Query("SELECT * FROM qr_codes ORDER BY number ASC")
    fun observeAll(): Flow<List<QrCodeEntity>>

    @Query("SELECT * FROM qr_codes ORDER BY number ASC")
    suspend fun getAll(): List<QrCodeEntity>

    @Query("SELECT COUNT(*) FROM qr_codes")
    suspend fun count(): Int

    @Query("SELECT * FROM qr_codes WHERE codeId = :codeId LIMIT 1")
    suspend fun getById(codeId: String): QrCodeEntity?

    @Query("SELECT * FROM qr_codes WHERE payload = :payload LIMIT 1")
    suspend fun getByPayload(payload: String): QrCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<QrCodeEntity>)

    @Query("DELETE FROM qr_codes")
    suspend fun deleteAll()
}

@Dao
interface TreasureHuntDao {
    @Query("SELECT * FROM treasure_hunts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TreasureHuntEntity>>

    @Query("SELECT * FROM treasure_hunts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TreasureHuntEntity?

    @Query("SELECT * FROM treasure_hunts WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TreasureHuntEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hunt: TreasureHuntEntity)

    @Update
    suspend fun update(hunt: TreasureHuntEntity)

    @Query("DELETE FROM treasure_hunts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HuntStepDao {
    @Query("SELECT * FROM hunt_steps WHERE huntId = :huntId ORDER BY orderIndex ASC")
    fun observeByHunt(huntId: String): Flow<List<HuntStepEntity>>

    @Query("SELECT * FROM hunt_steps WHERE huntId = :huntId ORDER BY orderIndex ASC")
    suspend fun getByHunt(huntId: String): List<HuntStepEntity>

    @Query("SELECT * FROM hunt_steps WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HuntStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: HuntStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<HuntStepEntity>)

    @Update
    suspend fun update(step: HuntStepEntity)

    @Query("DELETE FROM hunt_steps WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM hunt_steps WHERE huntId = :huntId")
    suspend fun deleteByHunt(huntId: String)
}

@Dao
interface HuntSessionDao {
    @Query("SELECT * FROM hunt_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<HuntSessionEntity>>

    @Query("SELECT * FROM hunt_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HuntSessionEntity?

    @Query("SELECT * FROM hunt_sessions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<HuntSessionEntity?>

    @Query("SELECT * FROM hunt_sessions WHERE huntId = :huntId ORDER BY startedAt DESC")
    fun observeByHunt(huntId: String): Flow<List<HuntSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: HuntSessionEntity)

    @Update
    suspend fun update(session: HuntSessionEntity)
}

@Dao
interface StepCompletionDao {
    @Query("SELECT * FROM step_completions WHERE sessionId = :sessionId ORDER BY stepIndex ASC")
    fun observeBySession(sessionId: String): Flow<List<StepCompletionEntity>>

    @Query("SELECT * FROM step_completions WHERE sessionId = :sessionId ORDER BY stepIndex ASC")
    suspend fun getBySession(sessionId: String): List<StepCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: StepCompletionEntity)
}
