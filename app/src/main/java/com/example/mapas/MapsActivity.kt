package com.example.mapas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Debug
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnSuccessListener
import com.google.gson.Gson

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMarkerClickListener,GoogleMap.OnMarkerDragListener {
    // ese enlace es para personalizar el maps
    //https://mapstyle.withgoogle.com/
    private val permisoFinelocation=android.Manifest.permission.ACCESS_FINE_LOCATION
    private val permisoCoarselocation=android.Manifest.permission.ACCESS_COARSE_LOCATION
    private val codigo_solictud_permiso =100

    // datos de la ubicación
    //longuitud y latitud
    var fusedLocationCliente:FusedLocationProviderClient?=null
    var locationRequest: LocationRequest?=null
    var callback:LocationCallback?= null
    //
    private var mapa:Mapa?=null
//
    private val markerListener =this
    private val dragListener=this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationCliente =FusedLocationProviderClient(this)
        inicializarlocationrequest()

        callback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                if(mapa !=null){
                    // me detecte mi ubicación
                    mapa?.configurarMiubcacion()
                    // le comente porque te lleva a la misma pantalla
                    for(ubcación in locationResult?.locations!!){
                        mapa?.miposcion = LatLng(ubcación.latitude, ubcación.longitude)
                        mapa?.añadirmiposicion()
                    }
                }
            }
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        mapa=Mapa(googleMap,applicationContext,markerListener,dragListener)
        mapa?.cambiarestilomapa()
        mapa?.marcadoresestaticos()
        mapa?.crearlisteners()
        mapa?.prepararMarcadores()
        mapa?.dibujarlineas()
    }

    // son eventos que arrastra en la interfaz
    override fun onMarkerDragEnd(marcador: Marker?) {
        Toast.makeText(this,"Acabo el evento drag y drop",Toast.LENGTH_SHORT).show()
        Log.d("Marcador final ",marcador?.position?.latitude.toString()+","+marcador?.position?.longitude.toString())
    }

    override fun onMarkerDragStart(marcador: Marker?) {
        Toast.makeText(this,"Empezando a mover el marcador",Toast.LENGTH_SHORT).show()
        Log.d("Marcador inical ",marcador?.position?.latitude.toString()+","+marcador?.position?.longitude.toString())
    }
    // mapear la ubicacion donde se esta moviendo el marcador
    override fun onMarkerDrag(marcador: Marker?) {
        title = marcador?.position?.latitude.toString()+","+marcador?.position?.longitude.toString()

    }

    // esto es para lo marcadores
    override fun onMarkerClick(marcador: Marker?): Boolean {
        var numeroclicks =marcador?.tag as? Int
        if(numeroclicks !=null){
            numeroclicks++
            marcador?.tag=numeroclicks
            Toast.makeText(applicationContext,"se han dado"+numeroclicks.toString(),Toast.LENGTH_SHORT).show()

        }
        return false
    }
    private fun inicializarlocationrequest(){
        locationRequest =LocationRequest()
        // cada cuanto tiempo
        locationRequest?.interval =10000 // milisegundos 10 segundos
        locationRequest?.fastestInterval =5000
        // mas alto
        locationRequest?.priority =LocationRequest.PRIORITY_HIGH_ACCURACY
    }


    private fun validarPermisoUbicacion():Boolean {
        val hayubicacionpremisa=ActivityCompat.checkSelfPermission(this,permisoFinelocation)== PackageManager.PERMISSION_GRANTED
        val hayubicacionordinaria =ActivityCompat.checkSelfPermission(this,permisoCoarselocation)== PackageManager.PERMISSION_GRANTED
        return hayubicacionpremisa &&  hayubicacionordinaria
    }
    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion(){

        fusedLocationCliente?.requestLocationUpdates(locationRequest,callback,null)
    }
    private fun pedirpermisos(){
        val deboproveercontexto = ActivityCompat.shouldShowRequestPermissionRationale(this,permisoFinelocation)
        if(deboproveercontexto){
            // mandar un mensaje con explicacion adicional
            solicitudPermiso()
        } else{
            solicitudPermiso()
        }
    }
    private fun solicitudPermiso(){
        requestPermissions(arrayOf(permisoFinelocation,permisoCoarselocation),codigo_solictud_permiso)
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,
                                            grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        when(requestCode){
// se mapeo mi permiso
            codigo_solictud_permiso->{
                if(grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
// ya puedo obtener la ubicaicion
                    obtenerUbicacion()
                }else{
                    // de lo contratio no tiene el permiso
                    Toast.makeText(this,"No diste permiso para tener una ubicación",Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
    private fun deteneractualizacionubicacion(){
        fusedLocationCliente?.removeLocationUpdates(callback)
    }
    override fun onStart() {
        super.onStart()

        if(validarPermisoUbicacion()){
            obtenerUbicacion()
        }else{
            pedirpermisos()
        }
    }
    override fun onPause(){
        super.onPause()
        deteneractualizacionubicacion()
    }


}