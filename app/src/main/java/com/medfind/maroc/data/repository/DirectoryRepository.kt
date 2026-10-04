package com.medfind.maroc.data.repository

import com.medfind.maroc.domain.model.Catalog
import kotlinx.coroutines.flow.Flow

/**
 * Point d'accès unique aux données de l'annuaire.
 *
 * La V2 utilise [LocalDirectoryRepository] : Room reste la source de lecture
 * hors ligne et le repository synchronise les JSON publics de GitHub Raw avant
 * de les importer dans Room. Aucune API GitHub n'est utilisée.
 */
interface DirectoryRepository {
    /** Catalogue complet, mis à jour automatiquement. */
    val catalog: Flow<Catalog>

    /** Prépare les données (import initial ou mise à jour). Ne lève jamais d'exception. */
    suspend fun initialize(): InitResult

    sealed interface InitResult {
        data object Ready : InitResult
        /** L'import a échoué mais des données précédentes restent disponibles. */
        data object ReadyWithStaleData : InitResult
        data object Failed : InitResult
    }
}

interface FavoritesRepository {
    val favoriteIds: Flow<List<String>>
    suspend fun add(doctorId: String)
    suspend fun remove(doctorId: String)
}
