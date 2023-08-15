package com.cursokotlin.routemapexample

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
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

    private var start: String = ""
    private var end: String = ""

    var poly: Polyline? = null
    var markerStart: MarkerOptions? = null
    var markerEnd: MarkerOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        btnCalculate = findViewById(R.id.btnCalculateRoute)
        btnCalculate.setOnClickListener {
            // Eliminar marcadores y ruta anterior
            map.clear() // Esto eliminará todos los marcadores y rutas dibujadas
            start = ""
            end = ""
            poly?.remove() //borrar la linea de ruta
            poly = null
            Toast.makeText(this, "Selecciona punto de origen y final", Toast.LENGTH_SHORT).show()
            if (::map.isInitialized) {
                map.setOnMapClickListener { it ->
                    if (start.isEmpty()) {
                        // Coordenada 1
                        start = "${it.longitude},${it.latitude}"
                        markerStart = MarkerOptions()
                            .position(it)
                            .title("Inicio")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        map.addMarker(markerStart!!)

                        println("Punto Inicio:"+it)

                    } else if (end.isEmpty()) {
                        // Coordenada 2
                        end = "${it.longitude},${it.latitude}"
                        markerEnd = MarkerOptions()
                            .position(it)
                            .title("Fin")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        map.addMarker(markerEnd!!)

                        println("Punto Final:"+it)

                        // Calcular el centro entre las coordenadas de inicio y fin
                        val centerLat = (it.latitude + markerStart?.position!!.latitude) / 2
                        val centerLng = (it.longitude + markerStart?.position!!.longitude) / 2
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
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        // Definir las coordenadas del punto de carga
        val puntoDeCarga = LatLng(19.982775014142295,-98.68548095226288) // Cambia estas coordenadas

        // Configurar la cámara del mapa para que esté centrada en el punto de carga
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(puntoDeCarga, 10f) // Cambia el nivel de zoom según tus preferencias
        map.moveCamera(cameraUpdate)
    }

    private fun createRoute() {
        //Clics en el mapa
        CoroutineScope(Dispatchers.IO).launch {
            val call = getRetrofit().create(ApiService::class.java)
                .getRoute("5b3ce3597851110001cf62487e318d4a23d745d58d4b17f3216581cc", start, end)
                //.getRoute("TU_API_KEY", start, end)
            if (call.isSuccessful) {
                drawRoute(call.body())
            } else {
                Log.i("MAPS", "Fallo")
                runOnUiThread {
                    Toast.makeText(this@MapsActivity, "Error al crear la ruta, intente de nuevo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {//VOLVER AL HILO PRINCIPAL
            //Dibujar la ruta
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