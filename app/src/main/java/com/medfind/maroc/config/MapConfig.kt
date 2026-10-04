package com.medfind.maroc.config

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

/**
 * Configuration de la carte OpenStreetMap (osmdroid).
 *
 * Les tuiles standard d'OpenStreetMap (tile.openstreetmap.org) sont soumises à une
 * politique d'usage équitable : https://operations.osmfoundation.org/policies/tiles/
 * Pour une forte audience, utilisez un fournisseur de tuiles OSM dédié ou votre
 * propre serveur en remplaçant [tileSource] par un XYTileSource.
 */
object MapConfig {
    val tileSource: OnlineTileSourceBase = TileSourceFactory.MAPNIK

    const val ATTRIBUTION = "© les contributeurs d'OpenStreetMap"

    /** Vue initiale : centre du Maroc. */
    const val DEFAULT_LATITUDE = 31.79
    const val DEFAULT_LONGITUDE = -7.09
    const val DEFAULT_ZOOM = 5.6
    const val CITY_ZOOM = 13.0
    const val DOCTOR_ZOOM = 16.0
    const val MIN_ZOOM = 4.0
    const val MAX_ZOOM = 19.0

    /** Au-delà, seuls les premiers résultats sont affichés pour garder une carte fluide. */
    const val MAX_MARKERS = 400
}
