package com.medfind.maroc.data.remote

import android.util.Log
import com.medfind.maroc.data.seed.AssetDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Source distante statique de MedFind Maroc.
 *
 * Cette classe lit les fichiers JSON publics servis par GitHub Raw
 * en HTTPS.
 *
 * En cas d'échec réseau, l'application conserve les données Room
 * déjà installées et continue de fonctionner hors ligne.
 */
class RemoteDataSource(
    private val assets: AssetDataSource,
) {

    data class SyncResult(
        val updated: Boolean,
        val remoteVersion: Int? = null,
        val updatedAt: String? = null,
        val dataset: AssetDataSource.Dataset? = null,
        val error: String? = null,
    )

    suspend fun checkAndDownload(
        localRemoteVersion: Int,
    ): SyncResult = withContext(Dispatchers.IO) {

        try {
            // 1. Télécharger version.json
            val versionText = getText(VERSION_URL)

            val versionJson = JSONObject(versionText)

            val remoteVersion =
                versionJson.optInt("version", 0)

            val updatedAt =
                versionJson.optString(
                    "updatedAt",
                    "",
                ).ifBlank {
                    null
                }

            // 2. Vérifier que la version est valide
            if (remoteVersion <= 0) {
                return@withContext SyncResult(
                    updated = false,
                    error = "version.json ne contient pas une version valide",
                )
            }

            // 3. La version distante n'est pas plus récente
            if (remoteVersion <= localRemoteVersion) {
                return@withContext SyncResult(
                    updated = false,
                    remoteVersion = remoteVersion,
                    updatedAt = updatedAt,
                )
            }

            // 4. Paramètre de version pour éviter une ancienne réponse en cache
            val suffix = "?v=$remoteVersion"

            // 5. Télécharger les trois fichiers
            val specialties =
                getText("$SPECIALTIES_URL$suffix")

            val cities =
                getText("$CITIES_URL$suffix")

            val doctors =
                getText("$DOCTORS_URL$suffix")

            // 6. Valider les données avant de modifier Room
            val dataset =
                assets.loadFromTexts(
                    specialties,
                    cities,
                    doctors,
                )

            if (dataset.doctors.isEmpty()) {
                throw IOException(
                    "La base distante ne contient aucun médecin valide",
                )
            }

            // 7. Retourner les nouvelles données
            SyncResult(
                updated = true,
                remoteVersion = remoteVersion,
                updatedAt = updatedAt,
                dataset = dataset,
            )

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Synchronisation distante ignorée",
                e,
            )

            SyncResult(
                updated = false,
                error = e.message
                    ?: e.javaClass.simpleName,
            )
        }
    }

    /**
     * Télécharge un fichier texte depuis GitHub Raw.
     */
    private fun getText(
        urlString: String,
    ): String {

        val connection =
            (URL(urlString).openConnection()
                as HttpURLConnection).apply {

                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                instanceFollowRedirects = true

                setRequestProperty(
                    "Accept",
                    "application/json",
                )

                setRequestProperty(
                    "User-Agent",
                    "MedFindMaroc/1.0.0",
                )
            }

        return try {

            val code =
                connection.responseCode

            if (code !in 200..299) {
                throw IOException(
                    "HTTP $code pour $urlString",
                )
            }

            connection.inputStream.use { input ->

                val bytes =
                    input.readBytesLimited(
                        MAX_BYTES,
                    )

                bytes.toString(
                    Charsets.UTF_8,
                )
            }

        } finally {

            connection.disconnect()
        }
    }

    /**
     * Empêche le téléchargement de fichiers trop volumineux.
     */
    private fun java.io.InputStream.readBytesLimited(
        maxBytes: Int,
    ): ByteArray {

        val out =
            java.io.ByteArrayOutputStream()

        val buffer =
            ByteArray(8192)

        var total = 0

        while (true) {

            val count =
                read(buffer)

            if (count == -1) {
                break
            }

            total += count

            if (total > maxBytes) {
                throw IOException(
                    "Fichier distant trop volumineux",
                )
            }

            out.write(
                buffer,
                0,
                count,
            )
        }

        return out.toByteArray()
    }

    private companion object {

        const val TAG =
            "RemoteDataSource"

        const val TIMEOUT_MS =
            6_000

        const val MAX_BYTES =
            10 * 1024 * 1024

        const val BASE_URL =
            "https://raw.githubusercontent.com/remijak-maker/MedFindMaroc/main/app/src/main/assets/data/"

        const val VERSION_URL =
            "${BASE_URL}version.json"

        const val DOCTORS_URL =
            "${BASE_URL}doctors.json"

        const val CITIES_URL =
            "${BASE_URL}cities.json"

        const val SPECIALTIES_URL =
            "${BASE_URL}specialties.json"
    }
}