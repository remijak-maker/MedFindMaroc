package com.medfind.maroc.domain.search

import com.medfind.maroc.domain.model.Doctor
import com.medfind.maroc.domain.model.DoctorType

data class SearchFilters(
    val query: String = "",
    val type: DoctorType? = null,
    val specialtyId: String? = null,
    val cityId: String? = null,
    /** Distance maximale en km (utilisée uniquement si la position est connue). */
    val maxDistanceKm: Double? = null,
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || type != null || specialtyId != null ||
            cityId != null || maxDistanceKm != null
}

data class GeoPosition(val latitude: Double, val longitude: Double)

data class DoctorResult(val doctor: Doctor, val distanceKm: Double?)

/**
 * Recherche locale : chaque mot saisi doit apparaître dans le nom, la spécialité
 * (et ses synonymes : « cardiologue », « pédiatre »…), la ville, la région ou le quartier.
 */
object DoctorSearch {

    private val stopWords = setOf(
        "dr", "docteur", "de", "du", "des", "la", "le", "les", "a", "au", "aux",
        "en", "et", "un", "une", "pour", "pres", "dans", "sur", "d", "l",
    )

    val distanceOptionsKm = listOf(1.0, 5.0, 10.0, 25.0)

    fun buildSearchIndex(vararg parts: String?): String =
        parts.filterNot { it.isNullOrBlank() }.joinToString(" ") { TextNormalizer.normalize(it) }

    fun tokenize(query: String): List<String> =
        TextNormalizer.normalize(query)
            .split(' ')
            .filter { it.isNotBlank() && it !in stopWords }

    fun matches(doctor: Doctor, tokens: List<String>): Boolean =
        tokens.all { token -> tokenMatches(doctor.searchIndex, token) }

    private fun tokenMatches(index: String, token: String): Boolean {
        if (index.contains(token)) return true
        // Pluriels simples : « cardiologues », « pediatres »
        if (token.length > 4 && token.endsWith('s')) return index.contains(token.dropLast(1))
        return false
    }

    fun filter(
        doctors: List<Doctor>,
        filters: SearchFilters,
        position: GeoPosition?,
    ): List<DoctorResult> {
        val tokens = tokenize(filters.query)
        val results = ArrayList<DoctorResult>(doctors.size)
        for (doctor in doctors) {
            if (filters.type != null && doctor.type != filters.type) continue
            if (filters.specialtyId != null && doctor.specialty?.id != filters.specialtyId) continue
            if (filters.cityId != null && doctor.city?.id != filters.cityId) continue
            if (tokens.isNotEmpty() && !matches(doctor, tokens)) continue

            val distance = if (position != null && doctor.latitude != null && doctor.longitude != null) {
                GeoUtils.distanceKm(position.latitude, position.longitude, doctor.latitude, doctor.longitude)
            } else null

            if (filters.maxDistanceKm != null && position != null) {
                if (distance == null || distance > filters.maxDistanceKm) continue
            }
            results.add(DoctorResult(doctor, distance))
        }
        return if (position != null) {
            results.sortedWith(compareBy(nullsLast<Double>()) { it.distanceKm })
        } else {
            results.sortedWith(
                compareBy<DoctorResult> { it.doctor.city?.name ?: "￿" }
                    .thenBy { it.doctor.type.ordinal }
                    .thenBy { it.doctor.lastName }
            )
        }
    }
}
