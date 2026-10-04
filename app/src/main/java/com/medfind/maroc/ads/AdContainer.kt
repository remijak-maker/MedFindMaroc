package com.medfind.maroc.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.medfind.maroc.ui.components.LabeledDivider

/**
 * Emplacement publicitaire AdMob (bannière adaptative).
 *
 * Règles respectées :
 *  - toujours précédé du libellé « PUBLICITÉ » et séparé du contenu par des marges ;
 *  - jamais placé contre un bouton d'action (Appeler, Itinéraire) ni sur la navigation ;
 *  - disparaît entièrement si l'annonce ne se charge pas (hors ligne, pas d'inventaire…).
 */
@Composable
fun AdContainer(modifier: Modifier = Modifier) {
    if (!AdsConfig.enabled) return
    var failed by remember { mutableStateOf(false) }
    if (failed) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LabeledDivider("PUBLICITÉ")
            Spacer(Modifier.height(12.dp))
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Publicité" },
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
                        adUnitId = AdsConfig.bannerUnitId
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                failed = true
                            }
                        }
                        try {
                            loadAd(AdRequest.Builder().build())
                        } catch (e: Exception) {
                            failed = true
                        }
                    }
                },
                onRelease = { adView -> runCatching { adView.destroy() } },
            )
        }
    }
}
