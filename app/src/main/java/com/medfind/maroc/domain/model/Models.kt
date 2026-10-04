package com.medfind.maroc.domain.model

/** Type de médecin. Deux valeurs uniquement (pas de dentistes). */
enum class DoctorType(val label: String) {
    GENERALIST("Médecin généraliste"),
    SPECIALIST("Médecin spécialiste");

    companion object {
        fun fromValue(value: String?): DoctorType? = when (value?.trim()?.uppercase()) {
            "GENERALIST" -> GENERALIST
            "SPECIALIST" -> SPECIALIST
            else -> null
        }
    }
}

data class Specialty(
    val id: String,
    val name: String,
    /** Nom du praticien : « Cardiologue », « Pédiatre »… */
    val practitionerName: String,
    val type: DoctorType,
    val keywords: String,
    val popular: Boolean,
    val icon: String,
    val order: Int,
)

data class City(
    val id: String,
    val name: String,
    val regionId: String,
    val regionName: String,
    val latitude: Double?,
    val longitude: Double?,
)

data class Doctor(
    val id: String,
    val lastName: String,
    val firstName: String?,
    val specialty: Specialty?,
    val type: DoctorType,
    val city: City?,
    val quarter: String?,
    val address: String?,
    val phone: String?,
    val secondaryPhone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val openingHours: String?,
    val description: String?,
    val clinic: String?,
    val website: String?,
    val whatsapp: String?,
    val source: String?,
    val verificationDate: String?,
    val verified: Boolean,
    /** Texte normalisé utilisé par la recherche (pré-calculé une seule fois). */
    val searchIndex: String,
) {
    val displayName: String
        get() = buildString {
            append("Dr. ")
            if (!firstName.isNullOrBlank()) append(firstName).append(' ')
            append(lastName)
        }

    val specialtyLabel: String
        get() = specialty?.practitionerName
            ?: if (type == DoctorType.GENERALIST) "Médecin généraliste" else "Spécialité non renseignée"

    val locationLabel: String
        get() = listOfNotNull(quarter?.takeIf { it.isNotBlank() }, city?.name)
            .joinToString(", ")
            .ifEmpty { "Ville non renseignée" }

    val hasCoordinates: Boolean get() = latitude != null && longitude != null

    /** Les profils de démonstration ne doivent jamais être présentés comme réels. */
    val isDemo: Boolean get() = source.equals(SOURCE_DEMO, ignoreCase = true)

    companion object {
        const val SOURCE_DEMO = "DEMO"
    }
}

/** Catalogue complet chargé depuis la source de données. */
data class Catalog(
    val doctors: List<Doctor>,
    val specialties: List<Specialty>,
    val cities: List<City>,
) {
    val containsDemoData: Boolean get() = doctors.any { it.isDemo }

    companion object {
        val EMPTY = Catalog(emptyList(), emptyList(), emptyList())
    }
}
