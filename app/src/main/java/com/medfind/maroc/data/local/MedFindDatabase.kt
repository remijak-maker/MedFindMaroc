package com.medfind.maroc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DoctorEntity::class, SpecialtyEntity::class, CityEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MedFindDatabase : RoomDatabase() {
    abstract fun directoryDao(): DirectoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val NAME = "medfind.db"

        fun create(context: Context): MedFindDatabase =
            Room.databaseBuilder(context.applicationContext, MedFindDatabase::class.java, NAME)
                // V1 : aucune migration n'existe encore. À remplacer par de vraies
                // migrations dès la V2 pour préserver les favoris des utilisateurs.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
