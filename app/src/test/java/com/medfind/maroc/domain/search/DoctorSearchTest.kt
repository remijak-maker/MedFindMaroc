package com.medfind.maroc.domain.search

import com.medfind.maroc.domain.model.City
import com.medfind.maroc.domain.model.Doctor
import com.medfind.maroc.domain.model.DoctorType
import com.medfind.maroc.domain.model.Specialty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctorSearchTest {

    private fun specialty(id: String, name: String, practitioner: String, keywords: String, type: DoctorType = DoctorType.SPECIALIST) =
        Specialty(id, name, practitioner, type, keywords, popular = false, icon = "x", order = 0)

    private val cardio = specialty("cardiologie", "Cardiologie", "Cardiologue", "cardiologue coeur")
    private val general = specialty("medecine-generale", "Médecine générale", "Médecin généraliste", "generaliste", DoctorType.GENERALIST)
    private val pedia = specialty("pediatrie", "Pédiatrie", "Pédiatre", "pediatre enfant")

    private val rabat = City("rabat", "Rabat", "rsk", "Rabat-Salé-Kénitra", 34.02, -6.84)
    private val larache = City("larache", "Larache", "tta", "Tanger-Tétouan-Al Hoceïma", 35.19, -6.15)
    private val casa = City("casablanca", "Casablanca", "cs", "Casablanca-Settat", 33.57, -7.59)

    private fun doctor(id: String, s: Specialty, c: City, lat: Double? = c.latitude) = Doctor(
        id = id, lastName = "Exemple $id", firstName = null, specialty = s, type = s.type, city = c,
        quarter = "Centre-ville", address = null, phone = null, secondaryPhone = null,
        latitude = lat, longitude = c.longitude, openingHours = null, description = null, clinic = null,
        website = null, whatsapp = null, source = "DEMO", verificationDate = null, verified = false,
        searchIndex = DoctorSearch.buildSearchIndex(
            "Exemple $id", s.name, s.practitionerName, s.keywords, s.type.label, c.name, c.regionName, "Centre-ville",
        ),
    )

    private val all = listOf(
        doctor("1", cardio, rabat),
        doctor("2", general, larache),
        doctor("3", pedia, casa),
        doctor("4", cardio, casa, lat = null),
    )

    private fun ids(query: String) = DoctorSearch.filter(all, SearchFilters(query = query), null).map { it.doctor.id }

    @Test fun `recherche spécialité + ville`() = assertEquals(listOf("1"), ids("Cardiologue Rabat"))
    @Test fun `recherche sans accents ni majuscules`() = assertEquals(listOf("2"), ids("medecin GENERALISTE larache"))
    @Test fun `recherche avec accents`() = assertEquals(listOf("3"), ids("Pédiatre Casablanca"))
    @Test fun `aucun résultat`() = assertTrue(ids("Dermatologue Tanger").isEmpty())
    @Test fun `recherche vide renvoie tout`() = assertEquals(4, ids("").size)
    @Test fun `pluriel`() = assertEquals(setOf("1", "4"), ids("cardiologues").toSet())

    @Test fun `tri par distance et filtre de distance`() {
        val pos = GeoPosition(33.58, -7.60)
        val sorted = DoctorSearch.filter(all, SearchFilters(), pos)
        assertEquals("3", sorted.first().doctor.id)
        assertEquals("4", sorted.last().doctor.id) // sans coordonnées : en dernier
        val near = DoctorSearch.filter(all, SearchFilters(maxDistanceKm = 5.0), pos)
        assertEquals(listOf("3"), near.map { it.doctor.id })
    }

    @Test fun `filtres type et ville`() {
        val r = DoctorSearch.filter(all, SearchFilters(type = DoctorType.SPECIALIST, cityId = "casablanca"), null)
        assertEquals(setOf("3", "4"), r.map { it.doctor.id }.toSet())
    }

    @Test fun `format des distances`() {
        assertEquals("430 m", GeoUtils.formatDistance(0.43))
        assertEquals("1,4 km", GeoUtils.formatDistance(1.43))
        assertEquals("23 km", GeoUtils.formatDistance(23.4))
    }
}
