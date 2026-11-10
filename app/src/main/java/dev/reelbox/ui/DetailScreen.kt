package dev.reelbox.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.reelbox.analytics.AppAnalytics
import dev.reelbox.data.Graph
import dev.reelbox.data.Provider
import dev.reelbox.data.RegionProviders
import dev.reelbox.data.TmdbImages
import dev.reelbox.ui.theme.Accent
import dev.reelbox.ui.theme.Gold
import dev.reelbox.ui.theme.Ink
import dev.reelbox.ui.theme.InkCard
import dev.reelbox.ui.theme.InkElevated
import dev.reelbox.ui.theme.TextPrimary
import dev.reelbox.ui.theme.TextSecondary

@Composable
fun DetailScreen(
    type: String,
    id: Int,
    onBack: () -> Unit,
    onOpen: (String, Int) -> Unit,
    onPlay: (String, Int, String) -> Unit = { _, _, _ -> },
    vm: DetailViewModel = viewModel(factory = DetailViewModel.factory(type, id)),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val savedItems by Graph.watchlist.items.collectAsStateWithLifecycle()
    val region by Graph.settings.region.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showRegions by remember { mutableStateOf(false) }

    val detail = state.detail
    val saved = savedItems.any { it.id == id && it.type == type }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!, onRetry = vm::load)
            detail != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                item(key = "header") {
                    DetailHeader(
                        backdrop = TmdbImages.backdrop(detail.backdropPath, 780)
                            ?: TmdbImages.poster(detail.posterPath, 500),
                        title = detail.displayTitle,
                        onBack = onBack,
                        onPlay = { onPlay(type, id, detail.displayTitle) },
                    )
                }

                item(key = "titles") {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = detail.displayTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                        )
                        if (detail.tagline.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = detail.tagline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (detail.year.isNotBlank()) {
                                Text(detail.year, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                Spacer(Modifier.width(10.dp))
                            }
                            if (detail.runtimeLabel.isNotBlank()) {
                                Text(detail.runtimeLabel, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                Spacer(Modifier.width(10.dp))
                            }
                            if (detail.seasons != null && detail.seasons > 0) {
                                Text(
                                    "${detail.seasons} season${if (detail.seasons == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary,
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            if (detail.voteAverage > 0.0) {
                                Icon(Icons.Filled.Star, null, tint = Gold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    String.format("%.1f", detail.voteAverage),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "(${detail.voteCount})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }

                if (detail.genres.isNotEmpty()) {
                    item(key = "genres") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(detail.genres, key = { it.id }) { genre ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(genre.name, color = TextSecondary) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = InkElevated),
                                    border = AssistChipDefaults.assistChipBorder(
                                        enabled = true,
                                        borderColor = InkCard,
                                    ),
                                )
                            }
                        }
                    }
                }

                item(key = "actions") {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { onPlay(type, id, detail.displayTitle) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = vm::toggleSaved,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (saved) Accent else Color.White,
                                contentColor = if (saved) Color.White else Color.Black,
                            ),
                        ) {
                            Icon(
                                imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(if (saved) "In watchlist" else "Watchlist", fontWeight = FontWeight.Bold)
                        }

                        detail.trailer?.let { trailer ->
                            OutlinedButton(
                                onClick = {
                                    AppAnalytics.logPlayTrailer(detail.displayTitle, type, id)
                                    openUrl("https://www.youtube.com/watch?v=${trailer.key}")
                                },
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Trailer")
                            }
                        }

                        IconButton(onClick = {
                            val text = "https://www.themoviedb.org/${if (type == "tv") "tv" else "movie"}/$id"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                this.type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${detail.displayTitle}\n$text")
                            }
                            runCatching { context.startActivity(Intent.createChooser(send, "Share")) }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = TextSecondary)
                        }
                    }
                }

                if (detail.overview.isNotBlank()) {
                    item(key = "overview") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = detail.overview,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                item(key = "providers") {
                    val providers = detail.providersFor(region)
                    WatchProvidersSection(
                        region = region,
                        providers = providers,
                        onProviderClick = { provider ->
                            AppAnalytics.logProviderClick(provider.name, detail.displayTitle, type)
                            StreamingApps.open(context, provider, providers?.link)
                        },
                        onOpenLink = { url -> openUrl(url) },
                        onChangeRegion = { showRegions = true },
                    )
                }

                val cast = detail.credits?.cast?.take(20).orEmpty()
                if (cast.isNotEmpty()) {
                    item(key = "cast") {
                        Column(Modifier.padding(top = 14.dp)) {
                            SectionHeader("Cast")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(cast, key = { it.id }) { member ->
                                    AvatarChip(member.name, member.profilePath, member.character)
                                }
                            }
                        }
                    }
                }

                if (state.similar.isNotEmpty()) {
                    item(key = "similar") {
                        Column(Modifier.padding(top = 18.dp)) {
                            SectionHeader("More like this")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.similar, key = { it.id }) { movie ->
                                    PosterCard(movie, onClick = {
                                        onOpen(if (movie.isTv) "tv" else "movie", movie.id)
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRegions) {
        RegionDialog(
            current = region,
            onSelect = {
                AppAnalytics.logRegionChange(it)
                Graph.settings.setRegion(it)
            },
            onDismiss = { showRegions = false },
        )
    }
}

@Composable
private fun DetailHeader(
    backdrop: String?,
    title: String,
    onBack: () -> Unit,
    onPlay: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        RemoteImage(
            url = backdrop,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.5f to Color.Transparent,
                        1f to Ink,
                    ),
                ),
        )

        if (onPlay != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.9f))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play movie",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun WatchProvidersSection(
    region: String,
    providers: RegionProviders?,
    onProviderClick: (Provider) -> Unit,
    onOpenLink: (String) -> Unit,
    onChangeRegion: () -> Unit,
) {
    val groups = buildList {
        providers?.free?.takeIf { it.isNotEmpty() }?.let { add("Free" to it) }
        providers?.ads?.takeIf { it.isNotEmpty() }?.let { add("Free with ads" to it) }
        providers?.flatrate?.takeIf { it.isNotEmpty() }?.let { add("Included with subscription" to it) }
        providers?.rent?.takeIf { it.isNotEmpty() }?.let { add("Rent" to it) }
        providers?.buy?.takeIf { it.isNotEmpty() }?.let { add("Buy" to it) }
    }

    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Where to watch",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = regionName(region),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onChangeRegion)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        if (groups.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No legal streaming options listed for ${regionName(region)} right now. Tap the region to change it.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        } else {
            groups.forEach { (label, list) ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.take(6).forEach { provider ->
                        ProviderBadge(provider) {
                            onProviderClick(provider)
                        }
                    }
                }
            }
            providers?.link?.let { link ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Availability data by JustWatch · tap a logo to open or install app",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.clickable { onOpenLink(link) },
                )
            }
        }
    }
}

@Composable
private fun ProviderBadge(provider: Provider, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RemoteImage(
            url = TmdbImages.providerLogo(provider.logoPath, 92),
            contentDescription = provider.name,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = provider.name,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
