package com.cursokotlin.routemapexample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var btnCalculate: Button

    private lateinit var btnCenterLocation: Button

    private var start: String = ""
    private var end: String = ""

    //Dibujar ruta y marcadores de puntos
    var poly: Polyline? = null
    var markerStart: MarkerOptions? = null
    var markerEnd: MarkerOptions? = null

    //Alternar tipo de vista
    private var currentMapType: Int = GoogleMap.MAP_TYPE_NORMAL

    //Ubicacion en tiempo real
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocationCircle: Circle? = null

    //Multiples puntos
    private val selectedPoints: MutableList<LatLng> = mutableListOf()

    //Dibujar rutas
    private val drawnRoutes: MutableSet<Pair<LatLng, LatLng>> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        btnCalculate = findViewById(R.id.btnCalculateRoute)
        btnCenterLocation = findViewById(R.id.btnCenterLocation)
        // Solicitar los permisos de ubicación en el inicio
        requestLocationPermission()

        btnCalculate.setOnClickListener {
            // Eliminar marcadores y ruta anterior
            map.clear() // Esto eliminará todos los marcadores y rutas dibujadas
            selectedPoints.clear() // Limpiar los puntos
            start = ""
            end = ""
            poly?.remove() //borrar la linea de ruta
            poly = null
            Toast.makeText(this, "Selecciona punto de origen y final", Toast.LENGTH_SHORT).show()
            if (::map.isInitialized) {
                map.setOnMapClickListener { it ->
                    selectedPoints.add(it)

                    val markerOptions = MarkerOptions()
                        .position(it)
                        .title("Punto ${selectedPoints.size}")
                    map.addMarker(markerOptions)

                    if (selectedPoints.size >= 2) {
                        start = "${selectedPoints[0].longitude},${selectedPoints[0].latitude}"
                        end = "${selectedPoints[selectedPoints.size - 1].longitude},${selectedPoints[selectedPoints.size - 1].latitude}"

                        // Calcular el centro entre las coordenadas de inicio y fin
                        val centerLat = (selectedPoints[0].latitude + selectedPoints[selectedPoints.size - 1].latitude) / 2
                        val centerLng = (selectedPoints[0].longitude + selectedPoints[selectedPoints.size - 1].longitude) / 2
                        val centerLatLng = LatLng(centerLat, centerLng)

                        // Animación hacia el centro
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(centerLatLng, 10f),
                            4000,
                            null
                        )

                        // LLAMAR A LA CREACIÓN DE LA RUTA
                        createRoute()
                    }
                }

            }

        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnChangeMapType = findViewById<Button>(R.id.btnChangeMapType)
        btnChangeMapType.setOnClickListener {
            changeMapType()
        }

        // Inicialización de la ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Manejo del botón para centrar la ubicación en tiempo real
        btnCenterLocation.setOnClickListener {
            if (::map.isInitialized) {
                if (checkLocationPermission()) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val currentLocation = LatLng(location.latitude, location.longitude)
                            // Configurar el nivel de zoom (ajusta el valor según tus preferencias)
                            val zoomLevel = 15f

                            // Animar la cámara para centrarse en la ubicación actual con el zoom
                            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel)
                            map.animateCamera(cameraUpdate)

                            // Actualizar la posición del círculo alrededor de la ubicación del usuario
                            userLocationCircle?.center = currentLocation
                        } else {
                            Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    requestLocationPermission()
                }
            }
        }

    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }

    private fun changeMapType() {
        currentMapType = when (currentMapType) {
            GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
            GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_HYBRID
            GoogleMap.MAP_TYPE_HYBRID -> GoogleMap.MAP_TYPE_TERRAIN
            GoogleMap.MAP_TYPE_TERRAIN -> GoogleMap.MAP_TYPE_NORMAL
            else -> GoogleMap.MAP_TYPE_NORMAL
        }
        map.mapType = currentMapType
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        // Definir las coordenadas del punto de carga
        val puntoDeCarga = LatLng(19.982775014142295,-98.68548095226288) // Cambia estas coordenadas

        // Configurar la cámara del mapa para que esté centrada en el punto de carga
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(puntoDeCarga, 10f) // Cambia el nivel de zoom según tus preferencias
        map.moveCamera(cameraUpdate)

        // Configura el tipo de mapa inicial
        map.mapType = currentMapType

        // Inicializar el círculo alrededor de la ubicación actual del usuario
        val circleOptions = CircleOptions()
            .center(puntoDeCarga)
            .radius(20.0) // Cambia este valor según tus preferencias (en metros)
            .strokeColor(Color.parseColor("#2B6EFF"))
            .fillColor(Color.parseColor("#66E3FEFF")) // Color con transparencia
        userLocationCircle = map.addCircle(circleOptions)
    }

    private fun createRoute() {
        // Verificar si hay al menos dos puntos para crear rutas
        if (selectedPoints.size >= 2) {
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0 until selectedPoints.size - 1) {
                    val startPoint = "${selectedPoints[i].longitude},${selectedPoints[i].latitude}"
                    val endPoint = "${selectedPoints[i + 1].longitude},${selectedPoints[i + 1].latitude}"

                    val routePair = Pair(selectedPoints[i], selectedPoints[i + 1])
                    if (routePair !in drawnRoutes) {
                        val call = getRetrofit().create(ApiServiceMaps::class.java)
                            .getRoute("5b3ce3597851110001cf62487e318d4a23d745d58d4b17f3216581cc", startPoint, endPoint)
                        //.getRoute("TU_API_KEY", startPoint, endPoint)

                        if (call.isSuccessful) {
                            drawRoute(call.body())
                            drawnRoutes.add(routePair)
                        } else {
                            Log.i("MAPS", "Fallo")
                            runOnUiThread {
                                Toast.makeText(this@MapsActivity, "Error al crear la ruta, intente de nuevo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "Agrega al menos dos puntos para calcular rutas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {
            // Dibujar la nueva ruta
            polyLineOptions.color(Color.BLUE)
            poly = map.addPolyline(polyLineOptions)
        }
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}