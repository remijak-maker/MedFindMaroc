package com.medfind.maroc.data.seed

import android.content.Context
import android.util.Log
import com.medfind.maroc.data.local.CityEntity
import com.medfind.maroc.data.local.DoctorEntity
import com.medfind.maroc.data.local.SpecialtyEntity
import com.medfind.maroc.domain.model.DoctorType
import com.medfind.maroc.domain.search.GeoUtils
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lit les fichiers JSON embarqués dans `app/src/main/assets/data/` :
 *  - specialties.json : liste des spécialités
 *  - cities.json      : régions et villes
 *  - doctors.json     : médecins
 *
 * Un enregistrement invalide est ignoré (journalisé) sans faire échouer l'import.
 */
class AssetDataSource(private val context: Context) {

    data class Dataset(
        val version: Int,
        val doctors: List<DoctorEntity>,
        val specialties: List<SpecialtyEntity>,
        val cities: List<CityEntity>,
    )

    fun load(): Dataset {
        val specialtiesJson = readJson("data/specialties.json")
        val citiesJson = readJson("data/cities.json")
        val doctorsJson = readJson("data/doctors.json")
        return parseDataset(specialtiesJson, citiesJson, doctorsJson)
    }

    /**
     * Parse un jeu de données téléchargé depuis la source distante.
     * Le même parseur que pour les assets est utilisé afin que les deux formats
     * restent strictement compatibles.
     */
    fun loadFromTexts(
        specialtiesText: String,
        citiesText: String,
        doctorsText: String,
    ): Dataset = parseDataset(
        JSONObject(specialtiesText),
        JSONObject(citiesText),
        JSONObject(doctorsText),
    )

    private fun parseDataset(
        specialtiesJson: JSONObject,
        citiesJson: JSONObject,
        doctorsJson: JSONObject,
    ): Dataset {
        val specialties = parseSpecialties(specialtiesJson.optJSONArray("specialites"))
        val cities = parseCities(citiesJson.optJSONArray("regions"))
        val specialtyById = specialties.associateBy { it.id }
        val doctors = parseDoctors(doctorsJson.optJSONArray("medecins"), specialtyById)

        // Version globale : la somme garantit qu'une modification de n'importe
        // quel fichier (avec incrément de sa version) déclenche une réimportation.
        val version = specialtiesJson.optInt("version", 1) +
            citiesJson.optInt("version", 1) * 1_000 +
            doctorsJson.optInt("version", 1) * 1_000_000
        return Dataset(version, doctors, specialties, cities)
    }

    private fun readJson(path: String): JSONObject =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }

    private fun parseSpecialties(array: JSONArray?): List<SpecialtyEntity> {
        if (array == null) return emptyList()
        val result = mutableListOf<SpecialtyEntity>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            try {
                val id = o.str("id") ?: continue
                val name = o.str("nom") ?: continue
                if (isExcluded(id, name)) continue
                val type = DoctorType.fromValue(o.str("type")) ?: DoctorType.SPECIALIST
                result += SpecialtyEntity(
                    id = id,
                    nom = name,
                    nomPraticien = o.str("nomPraticien") ?: name,
                    type = type.name,
                    motsCles = o.str("motsCles").orEmpty(),
                    populaire = o.optBoolean("populaire", false),
                    icone = o.str("icone") ?: "default",
                    ordre = o.optInt("ordre", i),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Spécialité ignorée (index $i)", e)
            }
        }
        return result.distinctBy { it.id }
    }

    private fun parseCities(regions: JSONArray?): List<CityEntity> {
        if (regions == null) return emptyList()
        val result = mutableListOf<CityEntity>()
        for (r in 0 until regions.length()) {
            val region = regions.optJSONObject(r) ?: continue
            val regionId = region.str("id") ?: continue
            val regionName = region.str("nom") ?: regionId
            val cities = region.optJSONArray("villes") ?: continue
            for (c in 0 until cities.length()) {
                val o = cities.optJSONObject(c) ?: continue
                val id = o.str("id") ?: continue
                val lat = o.dbl("latitude")
                val lon = o.dbl("longitude")
                val valid = GeoUtils.isValidCoordinate(lat, lon)
                result += CityEntity(
                    id = id,
                    nom = o.str("nom") ?: id,
                    regionId = regionId,
                    regionNom = regionName,
                    latitude = if (valid) lat else null,
                    longitude = if (valid) lon else null,
                )
            }
        }
        return result.distinctBy { it.id }
    }

    private fun parseDoctors(
        array: JSONArray?,
        specialties: Map<String, SpecialtyEntity>,
    ): List<DoctorEntity> {
        if (array == null) return emptyList()
        val result = mutableListOf<DoctorEntity>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            try {
                val id = o.str("id") ?: continue
                val lastName = o.str("nom") ?: continue
                val specialtyId = o.str("specialite")
                if (specialtyId != null && isExcluded(specialtyId, specialtyId)) continue
                val specialty = specialtyId?.let { specialties[it] }
                val type = DoctorType.fromValue(o.str("typeMedecin"))
                    ?: specialty?.let { DoctorType.fromValue(it.type) }
                    ?: DoctorType.GENERALIST
                val lat = o.dbl("latitude")
                val lon = o.dbl("longitude")
                val valid = GeoUtils.isValidCoordinate(lat, lon)
                result += DoctorEntity(
                    id = id,
                    nom = lastName,
                    prenom = o.str("prenom"),
                    specialiteId = specialty?.id,
                    typeMedecin = type.name,
                    villeId = o.str("ville"),
                    quartier = o.str("quartier"),
                    adresse = o.str("adresse"),
                    telephone = o.str("telephone"),
                    telephoneSecondaire = o.str("telephoneSecondaire"),
                    latitude = if (valid) lat else null,
                    longitude = if (valid) lon else null,
                    horaires = o.str("horaires"),
                    description = o.str("description"),
                    clinique = o.str("clinique"),
                    siteWeb = o.str("siteWeb"),
                    whatsapp = o.str("whatsapp"),
                    source = o.str("source"),
                    dateVerification = o.str("dateVerification"),
                    profilVerifie = o.optBoolean("profilVerifie", false),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Médecin ignoré (index $i)", e)
            }
        }
        return result.distinctBy { it.id }
    }

    /** Garde-fou : la V1 exclut explicitement les dentistes. */
    private fun isExcluded(id: String, name: String): Boolean {
        val text = (id + " " + name).lowercase()
        return "dentist" in text || "dentaire" in text || "odonto" in text
    }

    private fun JSONObject.str(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

    private fun JSONObject.dbl(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

    private companion object {
        const val TAG = "AssetDataSource"
    }
}
