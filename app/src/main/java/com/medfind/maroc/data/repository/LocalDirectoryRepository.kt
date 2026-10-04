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
import com.medfind.maroc.data.remote.RemoteDataSource
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
    private val remote: RemoteDataSource,
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
            // 1) Toujours garantir une base locale utilisable hors ligne.
            val dataset = assets.load()
            val installedVersion = prefs.getInt(KEY_DATA_VERSION, -1)
            if (installedVersion != dataset.version || dao.countDoctors() == 0) {
                dao.replaceAll(dataset.doctors, dataset.specialties, dataset.cities)
                favoriteDao.deleteOrphans()
                prefs.edit { putInt(KEY_DATA_VERSION, dataset.version) }
            }

            // 2) Vérifier ensuite la base GitHub. Un échec réseau ne casse jamais
            //    la base locale : Room reste la source de lecture de l'application.
            val localRemoteVersion = prefs.getInt(KEY_REMOTE_DATA_VERSION, -1)
            val sync = remote.checkAndDownload(localRemoteVersion)
            if (sync.updated && sync.dataset != null && sync.remoteVersion != null) {
                dao.replaceAll(
                    sync.dataset.doctors,
                    sync.dataset.specialties,
                    sync.dataset.cities,
                )
                favoriteDao.deleteOrphans()
                prefs.edit {
                    putInt(KEY_DATA_VERSION, sync.dataset.version)
                    putInt(KEY_REMOTE_DATA_VERSION, sync.remoteVersion)
                }
            } else if (sync.remoteVersion != null) {
                // Même lorsqu'aucun téléchargement n'est nécessaire, mémoriser
                // la version distante déjà connue.
                prefs.edit { putInt(KEY_REMOTE_DATA_VERSION, sync.remoteVersion) }
            }

            DirectoryRepository.InitResult.Ready
        } catch (e: Exception) {
            Log.e(TAG, "Initialisation des données impossible", e)
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
        const val KEY_REMOTE_DATA_VERSION = "remote_data_version"
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
