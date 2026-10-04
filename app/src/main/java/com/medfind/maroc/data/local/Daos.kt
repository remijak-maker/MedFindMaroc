package com.medfind.maroc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DirectoryDao {

    @Query("SELECT * FROM doctors")
    abstract fun observeDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM specialties ORDER BY ordre, nom")
    abstract fun observeSpecialties(): Flow<List<SpecialtyEntity>>

    @Query("SELECT * FROM cities ORDER BY regionNom, nom")
    abstract fun observeCities(): Flow<List<CityEntity>>

    @Query("SELECT COUNT(*) FROM doctors")
    abstract suspend fun countDoctors(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDoctors(items: List<DoctorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSpecialties(items: List<SpecialtyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCities(items: List<CityEntity>)

    @Query("DELETE FROM doctors")
    abstract suspend fun clearDoctors()

    @Query("DELETE FROM specialties")
    abstract suspend fun clearSpecialties()

    @Query("DELETE FROM cities")
    abstract suspend fun clearCities()

    /** Remplace tout le jeu de données de façon atomique. */
    @Transaction
    open suspend fun replaceAll(
        doctors: List<DoctorEntity>,
        specialties: List<SpecialtyEntity>,
        cities: List<CityEntity>,
    ) {
        clearDoctors()
        clearSpecialties()
        clearCities()
        insertSpecialties(specialties)
        insertCities(cities)
        insertDoctors(doctors)
    }
}

@Dao
interface FavoriteDao {

    @Query("SELECT doctorId FROM favorites ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE doctorId = :doctorId")
    suspend fun delete(doctorId: String)

    /** Supprime les favoris dont le médecin n'existe plus dans la base. */
    @Query("DELETE FROM favorites WHERE doctorId NOT IN (SELECT id FROM doctors)")
    suspend fun deleteOrphans()
}
