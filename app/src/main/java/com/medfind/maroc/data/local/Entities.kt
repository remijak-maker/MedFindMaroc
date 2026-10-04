package com.medfind.maroc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Médecin tel que stocké localement. Les noms de colonnes reprennent le cahier
 * des charges (nom, prenom, specialite, typeMedecin, ville…).
 * La région est déduite de la ville (table [CityEntity]).
 */
@Entity(
    tableName = "doctors",
    indices = [Index("specialiteId"), Index("villeId")],
)
data class DoctorEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val prenom: String?,
    val specialiteId: String?,
    val typeMedecin: String,
    val villeId: String?,
    val quartier: String?,
    val adresse: String?,
    val telephone: String?,
    val telephoneSecondaire: String?,
    val latitude: Double?,
    val longitude: Double?,
    val horaires: String?,
    val description: String?,
    val clinique: String?,
    val siteWeb: String?,
    val whatsapp: String?,
    val source: String?,
    val dateVerification: String?,
    val profilVerifie: Boolean,
)

@Entity(tableName = "specialties")
data class SpecialtyEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val nomPraticien: String,
    val type: String,
    val motsCles: String,
    val populaire: Boolean,
    val icone: String,
    val ordre: Int,
)

@Entity(tableName = "cities", indices = [Index("regionId")])
data class CityEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val regionId: String,
    val regionNom: String,
    val latitude: Double?,
    val longitude: Double?,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val doctorId: String,
    val addedAt: Long,
)
