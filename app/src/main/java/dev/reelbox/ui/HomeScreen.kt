package dev.reelbox.ui

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.reelbox.data.Graph
import dev.reelbox.data.Movie
import dev.reelbox.data.TmdbImages
import dev.reelbox.ui.theme.Accent
import dev.reelbox.ui.theme.Gold
import dev.reelbox.ui.theme.Ink
import dev.reelbox.ui.theme.TextPrimary
import dev.reelbox.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onOpen: (Movie) -> Unit,
    onSearch: () -> Unit,
    onWatchlist: () -> Unit,
    onPlay: (Movie) -> Unit = {},
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val region by Graph.settings.region.collectAsStateWithLifecycle()
    var showRegions by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Ink)) {
        when {
            state.loading && state.rows.isEmpty() -> LoadingState()
            state.error != null && state.rows.isEmpty() ->
                ErrorState(state.error!!, onRetry = vm::load)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp),
            ) {
                item(key = "hero") {
                    HeroPager(state.hero, onOpen, onPlay)
                }
                items(state.rows, key = { it.title }) { row ->
                    Column(Modifier.padding(top = 18.dp)) {
                        SectionHeader(row.title)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(row.items, key = { it.id }) { movie ->
                                PosterCard(movie, onClick = { onOpen(movie) })
                            }
                        }
                    }
                }
                item(key = "credits") {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Metadata from TMDB. This app does not host or stream any video.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }

        TopBar(
            region = region,
            onRegion = { showRegions = true },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showRegions) {
        RegionDialog(
            current = region,
            onSelect = { Graph.settings.setRegion(it) },
            onDismiss = { showRegions = false },
        )
    }
}

@Composable
private fun TopBar(
    region: String,
    onRegion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "REEL",
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 19.sp,
            letterSpacing = 2.5.sp,
        )
        Text(
            text = "BOX",
            color = Accent,
            fontWeight = FontWeight.Black,
            fontSize = 19.sp,
            letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onRegion)
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = region.uppercase(),
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun HeroPager(
    items: List<Movie>,
    onOpen: (Movie) -> Unit,
    onPlay: (Movie) -> Unit = {},
) {
    if (items.isEmpty()) return

    val pager = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items.size) {
        if (items.size < 2) return@LaunchedEffect
        while (true) {
            delay(6500)
            pager.animateScrollToPage((pager.currentPage + 1) % items.size)
        }
    }

    Box(Modifier.fillMaxWidth().height(470.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val movie = items[page]
            Box(Modifier.fillMaxSize()) {
                RemoteImage(
                    url = TmdbImages.backdrop(movie.backdropPath, 780),
                    contentDescription = movie.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                0.32f to Color.Transparent,
                                0.72f to Ink.copy(alpha = 0.85f),
                                1f to Ink,
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 34.dp),
                ) {
                    Text(
                        text = "FEATURED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = movie.displayTitle,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (movie.year.isNotBlank()) {
                            Text(
                                text = movie.year,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        if (movie.voteAverage > 0.0) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", movie.voteAverage),
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = movie.overview,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onPlay(movie) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onOpen(movie) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("More info", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(items.size) { index ->
                val active = index == pager.currentPage
                Box(
                    Modifier
                        .size(width = if (active) 18.dp else 5.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(if (active) Accent else Color.White.copy(alpha = 0.35f)),
                )
            }
        }
    }
}
