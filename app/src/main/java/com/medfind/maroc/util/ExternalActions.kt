package com.medfind.maroc.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.medfind.maroc.domain.model.Doctor
import java.util.Locale

/**
 * Lancement des applications externes (téléphone, WhatsApp, navigation, navigateur).
 * Chaque action est protégée : aucune exception ne peut remonter jusqu'à l'utilisateur.
 */
object ExternalActions {

    fun dial(context: Context, phone: String?) {
        val digits = sanitizePhone(phone)
        if (digits == null) {
            toast(context, "Numéro de téléphone non disponible.")
            return
        }
        // ACTION_DIAL ouvre le composeur sans appeler : aucune permission requise.
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(digits)))
        if (!start(context, intent)) toast(context, "Aucune application téléphone n'est disponible.")
    }

    fun openWhatsApp(context: Context, number: String?) {
        val international = toInternationalDigits(number)
        if (international == null) {
            toast(context, "Numéro WhatsApp non disponible.")
            return
        }
        // wa.me ouvre WhatsApp s'il est installé, sinon le navigateur.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + Uri.encode(international)))
        if (!start(context, intent)) toast(context, "WhatsApp n'est pas disponible sur cet appareil.")
    }

    fun openDirections(context: Context, doctor: Doctor) {
        val lat = doctor.latitude
        val lon = doctor.longitude
        if (lat != null && lon != null) {
            val coords = String.format(Locale.US, "%.6f,%.6f", lat, lon)
            val label = Uri.encode(doctor.displayName)
            val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coords?q=$coords($label)"))
            if (start(context, geo)) return
            // Secours 1 : itinéraire OpenStreetMap dans le navigateur
            val osm = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.openstreetmap.org/directions?route=%3B" + Uri.encode(coords) + "#map=16/$lat/$lon"),
            )
            if (start(context, osm)) return
            // Secours 2 : copier les coordonnées
            copyToClipboard(context, "Coordonnées", coords)
            toast(context, "Aucune application de navigation. Coordonnées copiées : $coords")
            return
        }
        val address = listOfNotNull(doctor.address, doctor.quarter, doctor.city?.name, "Maroc")
            .joinToString(", ")
        if (doctor.address == null && doctor.city == null) {
            toast(context, "Emplacement non disponible pour ce médecin.")
            return
        }
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)))
        if (start(context, geo)) return
        val osm = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/search?query=" + Uri.encode(address)))
        if (!start(context, osm)) {
            copyToClipboard(context, "Adresse", address)
            toast(context, "Aucune application de navigation. Adresse copiée.")
        }
    }

    fun openWebsite(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        val full = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        if (!start(context, Intent(Intent.ACTION_VIEW, Uri.parse(full)))) {
            toast(context, "Aucun navigateur n'est disponible.")
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        if (!start(context, intent)) toast(context, "Impossible d'ouvrir les paramètres.")
    }

    fun openLocationSettings(context: Context) {
        if (!start(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))) openAppSettings(context)
    }

    fun openStoreListing(context: Context) {
        val pkg = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        if (start(context, market)) return
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
        if (!start(context, web)) toast(context, "Google Play n'est pas disponible.")
    }

    /** Garde uniquement les chiffres et le « + » initial. */
    fun sanitizePhone(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val trimmed = phone.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length < 6) return null
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }

    /** « 06 12 34 56 78 » → « 212612345678 » (format attendu par wa.me). */
    fun toInternationalDigits(phone: String?): String? {
        val clean = sanitizePhone(phone) ?: return null
        val digits = clean.removePrefix("+")
        return when {
            clean.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") && digits.length == 10 -> "212" + digits.drop(1)
            digits.startsWith("212") -> digits
            else -> digits
        }.takeIf { it.length >= 8 }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    } catch (e: Exception) {
        false
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        }
    }

    private fun toast(context: Context, message: String) {
        runCatching { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
}
