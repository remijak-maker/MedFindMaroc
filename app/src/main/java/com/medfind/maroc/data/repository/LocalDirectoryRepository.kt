package com.medfind.maroc.data.repository

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.medfind.maroc.data.local.CityEntity
import com.medfind.maroc.data.local.DirectoryDao
import com.medfind.maroc.data.local.DoctorEntity
import com.medfind.maroc.data.local.FavoriteDao
import com.medfind.maroc.data.local.FavoriteEntity
import com.medfind.maroc.data.local.SpecialtyEntity
import com.medfind.maroc.data.seed.AssetDataSource
import com.medfind.maroc.domain.model.Catalog
import com.medfind.maroc.domain.model.City
import com.medfind.maroc.domain.model.Doctor
import com.medfind.maroc.domain.model.DoctorType
import com.medfind.maroc.domain.model.Specialty
import com.medfind.maroc.domain.search.DoctorSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LocalDirectoryRepository(
    private val dao: DirectoryDao,
    private val favoriteDao: FavoriteDao,
    private val assets: AssetDataSource,
    private val prefs: SharedPreferences,
) : DirectoryRepository {

    override val catalog: Flow<Catalog> = combine(
        dao.observeDoctors(),
        dao.observeSpecialties(),
        dao.observeCities(),
    ) { doctorRows, specialtyRows, cityRows ->
        val specialties = specialtyRows.map { it.toDomain() }
        val cities = cityRows.map { it.toDomain() }
        val specialtyById = specialties.associateBy { it.id }
        val cityById = cities.associateBy { it.id }
        val doctors = doctorRows.map { it.toDomain(specialtyById, cityById) }
        Catalog(doctors, specialties, cities)
    }.flowOn(Dispatchers.Default)

    override suspend fun initialize(): DirectoryRepository.InitResult = withContext(Dispatchers.IO) {
        try {
            val dataset = assets.load()
            val installedVersion = prefs.getInt(KEY_DATA_VERSION, -1)
            if (installedVersion != dataset.version || dao.countDoctors() == 0) {
                dao.replaceAll(dataset.doctors, dataset.specialties, dataset.cities)
                favoriteDao.deleteOrphans()
                prefs.edit { putInt(KEY_DATA_VERSION, dataset.version) }
            }
            DirectoryRepository.InitResult.Ready
        } catch (e: Exception) {
            Log.e(TAG, "Import des données impossible", e)
            val hasData = runCatching { dao.countDoctors() > 0 }.getOrDefault(false)
            if (hasData) DirectoryRepository.InitResult.ReadyWithStaleData
            else DirectoryRepository.InitResult.Failed
        }
    }

    private fun SpecialtyEntity.toDomain() = Specialty(
        id = id,
        name = nom,
        practitionerName = nomPraticien,
        type = DoctorType.fromValue(type) ?: DoctorType.SPECIALIST,
        keywords = motsCles,
        popular = populaire,
        icon = icone,
        order = ordre,
    )

    private fun CityEntity.toDomain() = City(
        id = id,
        name = nom,
        regionId = regionId,
        regionName = regionNom,
        latitude = latitude,
        longitude = longitude,
    )

    private fun DoctorEntity.toDomain(
        specialties: Map<String, Specialty>,
        cities: Map<String, City>,
    ): Doctor {
        val specialty = specialiteId?.let { specialties[it] }
        val city = villeId?.let { cities[it] }
        val type = DoctorType.fromValue(typeMedecin) ?: specialty?.type ?: DoctorType.GENERALIST
        return Doctor(
            id = id,
            lastName = nom,
            firstName = prenom,
            specialty = specialty,
            type = type,
            city = city,
            quarter = quartier,
            address = adresse,
            phone = telephone,
            secondaryPhone = telephoneSecondaire,
            latitude = latitude,
            longitude = longitude,
            openingHours = horaires,
            description = description,
            clinic = clinique,
            website = siteWeb,
            whatsapp = whatsapp,
            source = source,
            verificationDate = dateVerification,
            verified = profilVerifie,
            searchIndex = DoctorSearch.buildSearchIndex(
                prenom, nom, specialty?.name, specialty?.practitionerName, specialty?.keywords,
                type.label, city?.name, city?.regionName, quartier, clinique,
            ),
        )
    }

    private companion object {
        const val TAG = "DirectoryRepository"
        const val KEY_DATA_VERSION = "data_version"
    }
}

class LocalFavoritesRepository(private val dao: FavoriteDao) : FavoritesRepository {
    override val favoriteIds: Flow<List<String>> = dao.observeFavoriteIds()

    override suspend fun add(doctorId: String) = withContext(Dispatchers.IO) {
        dao.insert(FavoriteEntity(doctorId, System.currentTimeMillis()))
    }

    override suspend fun remove(doctorId: String) = withContext(Dispatchers.IO) {
        dao.delete(doctorId)
    }
}
