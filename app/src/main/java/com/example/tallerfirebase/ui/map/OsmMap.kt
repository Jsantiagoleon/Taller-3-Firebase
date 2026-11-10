package com.example.tallerfirebase.ui.map

import android.content.Context
import androidx.annotation.ColorInt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tallerfirebase.R
import com.example.tallerfirebase.data.model.MapPoint
import com.example.tallerfirebase.data.model.User
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

private const val DEFAULT_ZOOM = 17.0
private const val ME_ID = "me"
private val DEFAULT_CENTER = GeoPoint(4.6286, -74.0646) // Pontificia Universidad Javeriana, Bogotá
private const val MAP_ATTRIBUTION = "Tiles © Esri"
private const val ESRI_BASE_URL = "https://server.arcgisonline.com/ArcGIS/rest/services/"

// Mapas de Esri (ArcGIS Online): gratis y sin API key. Uno claro y uno oscuro.
// Esri usa el orden zoom/y/x en la URL, por eso se sobrescribe getTileURLString.
private class EsriTileSource(name: String, service: String, maxZoom: Int) : OnlineTileSourceBase(
    name, 0, maxZoom, 256, "",
    arrayOf("$ESRI_BASE_URL$service/MapServer/tile/"),
    MAP_ATTRIBUTION
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex)
}

private val LIGHT_TILES = EsriTileSource("EsriStreets", "World_Street_Map", 19)
private val DARK_TILES = EsriTileSource("EsriDarkGray", "Canvas/World_Dark_Gray_Base", 16)

// Datos que necesita un marcador en el mapa
private data class MarkerData(
    val id: String,
    val point: MapPoint,
    val title: String,
    val label: String,
    @ColorInt val color: Int
)

@Composable
fun OsmMap(
    currentUser: User?,
    myLocation: MapPoint?,
    otherUsers: List<User>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val ownColor = MaterialTheme.colorScheme.primary.toArgb()
    val otherColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val meTitle = stringResource(R.string.marcador_tu)

    val mapView = remember { createMapView(context) }
    val markers = remember { mutableMapOf<String, Marker>() }
    var centeredOnUser by rememberSaveable { mutableStateOf(false) }

    // Ciclo de vida del MapView de osmdroid
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Tema claro/oscuro: cambia el estilo del mapa
    LaunchedEffect(isDark) {
        mapView.setTileSource(if (isDark) DARK_TILES else LIGHT_TILES)
        mapView.invalidate()
    }

    // Centra el mapa la primera vez que se obtiene la ubicación del usuario
    LaunchedEffect(myLocation) {
        if (myLocation != null && !centeredOnUser) {
            mapView.controller.animateTo(GeoPoint(myLocation.latitud, myLocation.longitud))
            centeredOnUser = true
        }
        if (myLocation == null) centeredOnUser = false
    }

    // Actualiza los marcadores cada vez que cambian las posiciones
    LaunchedEffect(currentUser, myLocation, otherUsers, ownColor, otherColor) {
        val data = buildList {
            if (myLocation != null) {
                add(
                    MarkerData(
                        id = ME_ID,
                        point = myLocation,
                        title = meTitle,
                        label = MarkerIcons.initialOf(currentUser?.nombre),
                        color = ownColor
                    )
                )
            }
            otherUsers.forEach { user ->
                add(
                    MarkerData(
                        id = user.uid,
                        point = MapPoint(user.latitud, user.longitud),
                        title = user.nombre,
                        label = MarkerIcons.initialOf(user.nombre),
                        color = otherColor
                    )
                )
            }
        }
        syncMarkers(context, mapView, markers, data)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = "TallerFirebasePUJ/1.0 (Android)"
    }
    return MapView(context).apply {
        setTileSource(LIGHT_TILES)
        overlays.add(CopyrightOverlay(context))
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        controller.setZoom(DEFAULT_ZOOM)
        controller.setCenter(DEFAULT_CENTER)
    }
}

// Agrega, mueve o elimina marcadores para que coincidan con la lista recibida
private fun syncMarkers(
    context: Context,
    map: MapView,
    markers: MutableMap<String, Marker>,
    data: List<MarkerData>
) {
    val ids = data.map { it.id }.toSet()

    markers.keys.filter { it !in ids }.forEach { id ->
        markers.remove(id)?.let { marker ->
            marker.closeInfoWindow()
            map.overlays.remove(marker)
        }
    }

    data.forEach { item ->
        val marker = markers.getOrPut(item.id) {
            Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(this)
            }
        }
        marker.position = GeoPoint(item.point.latitud, item.point.longitud)
        marker.title = item.title
        val iconKey = "${item.label}-${item.color}"
        if (marker.relatedObject != iconKey) {
            marker.icon = MarkerIcons.create(context, item.label, item.color)
            marker.relatedObject = iconKey
        }
    }

    map.invalidate()
}
