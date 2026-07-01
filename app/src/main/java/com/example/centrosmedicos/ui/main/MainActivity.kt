package com.example.centrosmedicos.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.centrosmedicos.R
import com.example.centrosmedicos.databinding.ActivityMainBinding
import com.example.centrosmedicos.model.CentroMedico
import com.example.centrosmedicos.model.Especialidad
import com.example.centrosmedicos.ui.centromedico.ListCentroMedicoActivity
import com.example.centrosmedicos.ui.especialidad.ListEspecialidadActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private var userLatLng: LatLng? = null

    private var routePolyline: Polyline? = null
    private val markerList = mutableListOf<Marker>()
    private var especialidades = listOf<Especialidad>()
    private var selectedEspecialidadId: String? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) obtenerUbicacion()
            else Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Al seleccionar especialidad, buscar ruta automáticamente
        binding.spinnerEspecialidad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (pos == 0) return
                selectedEspecialidadId = especialidades.getOrNull(pos - 1)?.id
                buscarRuta()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        viewModel.especialidades.observe(this) { lista ->
            especialidades = lista
            val nombres = mutableListOf("Selecciona especialidad") +
                    lista.map { "${it.icono} ${it.nombre}" }
            binding.spinnerEspecialidad.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
        }

        viewModel.allMarkers.observe(this) { centros ->
            if (::map.isInitialized) colocarMarkers(centros)
        }

        viewModel.routeCoordinates.observe(this) { coords ->
            if (::map.isInitialized) {
                routePolyline?.remove()
                if (coords != null) dibujarRuta(coords)
            }
        }

        viewModel.nearestCentro.observe(this) { centro ->
            if (centro != null) mostrarInfoCentro(centro)
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrBlank()) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

        checkLocationPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val title = item.title.toString()
            val spannable = android.text.SpannableString(title)
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
                0, spannable.length,
                android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            item.title = spannable
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_centros -> {
                startActivity(Intent(this, ListCentroMedicoActivity::class.java)); true
            }
            R.id.menu_especialidades -> {
                startActivity(Intent(this, ListEspecialidadActivity::class.java)); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-12.0464, -77.0428), 12f))
    }

    private fun colocarMarkers(centros: List<CentroMedico>) {
        markerList.forEach { it.remove() }
        markerList.clear()
        for (centro in centros) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(centro.latitud, centro.longitud))
                    .title(centro.nombre)
                    .snippet(centro.direccion)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            marker?.let { markerList.add(it) }
        }
    }

    private fun dibujarRuta(coords: List<List<Double>>) {
        val options = PolylineOptions()
            .width(14f)
            .color(Color.parseColor("#1976D2"))
            .jointType(JointType.ROUND)
            .startCap(RoundCap())
            .endCap(RoundCap())
        coords.forEach { options.add(LatLng(it[1], it[0])) }
        routePolyline = map.addPolyline(options)

        if (coords.isNotEmpty()) {
            val bounds = LatLngBounds.builder()
            coords.forEach { bounds.include(LatLng(it[1], it[0])) }
            userLatLng?.let { bounds.include(it) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 120), 2000, null)
        }
    }

    private fun mostrarInfoCentro(centro: CentroMedico) {
        binding.cardInfo.visibility = View.VISIBLE
        binding.tvNombreCentro.text = centro.nombre
        binding.tvDireccion.text = centro.direccion
        binding.tvTelefono.text = centro.telefono
        binding.tvHorario.text = if (centro.horarios.isEmpty()) "Sin horario registrado"
                                  else centro.horarios.joinToString("\n") { it.toString() }

        // Mostrar imagen de fachada desde Base64
        if (centro.imagenBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(centro.imagenBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivFachadaInfo.setImageBitmap(bmp)
                binding.ivFachadaInfo.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.ivFachadaInfo.visibility = View.GONE
            }
        } else {
            binding.ivFachadaInfo.visibility = View.GONE
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun obtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                userLatLng = LatLng(location.latitude, location.longitude)
                if (::map.isInitialized) {
                    map.addMarker(
                        MarkerOptions()
                            .position(userLatLng!!)
                            .title("Mi ubicación")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, 13f))
                }
                if (selectedEspecialidadId != null) buscarRuta()
            } else {
                Toast.makeText(this, "No se pudo obtener ubicación. Verifica el GPS.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buscarRuta() {
        val espId = selectedEspecialidadId ?: return
        val loc = userLatLng ?: run {
            Toast.makeText(this, "Esperando ubicación GPS…", Toast.LENGTH_SHORT).show(); return
        }
        binding.cardInfo.visibility = View.GONE
        routePolyline?.remove()
        viewModel.findNearestAndRoute(espId, loc)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEspecialidades()
    }
}
