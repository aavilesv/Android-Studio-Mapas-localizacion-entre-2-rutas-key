package com.example.mapas
import android.content.Context
import android.graphics.Color
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.gson.Gson

class Mapa(mapa: GoogleMap,context: Context,var markerClickListener: GoogleMap.OnMarkerClickListener,var markerDragListener: GoogleMap.OnMarkerDragListener) {
    private var mMap:GoogleMap?=null
    private var context:Context? = null
    var miposcion:LatLng?=null
    private var rutaMarcada:Polyline?=null
    private var listaMarcadores:ArrayList<Marker>?=null
    private  var  marcadorGolden:Marker?=null
    private var marcadorpiramides:Marker?=null
    private var marcadorpisa:Marker?=null
    init {
        this.mMap =mapa
        this.context=context
    }
     fun dibujarlineas(){
        // esto para dibujar en linea
        val coordenadaspoligonos = PolygonOptions()
                .add(LatLng(-31.068617643648068,-64.57513086497784))
                .add(LatLng(-8.972811333190796,-63.10935895889997))
                .add(LatLng(-3.524296976080337,-42.34123349189759))
                .add(LatLng(0.14416522966940282,-57.55299601703883))
                .strokePattern(arrayListOf<PatternItem>(Dash(10f), Gap(20f)))
                .strokeColor(Color.BLUE)
                .fillColor(Color.GREEN)
                .strokeWidth(10f)


        val coordenadaslineas = PolylineOptions()
                .add(LatLng(-27.44284667443295,-65.82242075353861))
                .add(LatLng(-28.819809341698587,-66.19092911481857))
                .add(LatLng(-27.84994510940371,-65.11382471770048))
                .add(LatLng(-24.26762448309599,-60.69592874497175))
                .pattern(arrayListOf<PatternItem>(Dot(), Gap(10f)))
                .color(Color.GREEN)
                .width(30f)


        val coordenadas = CircleOptions()
                .center(LatLng(-24.431871972252402,-63.258707374334335))
                .radius(80000.0)
                .strokePattern(arrayListOf<PatternItem>(Dash(10f), Gap(20f)))
                .strokeColor(Color.BLUE)
                .fillColor(Color.YELLOW)
                .strokeWidth(15f)
        mMap?.addPolygon(coordenadaspoligonos)
        mMap?.addPolyline(coordenadaslineas)
        mMap?.addCircle(coordenadas)

    }
     fun cambiarestilomapa(){
        // esto hace para mostrar 5 tipo de mostración, puedes ponerle con action bar
        //mMap.mapType =GoogleMap.MAP_TYPE_HYBRID
        val exitoCambioMapa =mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(context,R.raw.estilo_mapa))

        if(!exitoCambioMapa!!){
            // huno un problema al cambiar el tipo de mapa
        }
    }
     fun crearlisteners(){
        mMap?.setOnMarkerClickListener(markerClickListener)
        mMap?.setOnMarkerDragListener(markerDragListener)
    }

     fun marcadoresestaticos(){
        //  golden gate :37.8199286,-122.4782551
        // piramides de giza: 29.9772962,31.1324955
        //torre de pisa: 43.722952, 10.396597
        val  golden_gate = LatLng(37.8199286,-122.4782551)
        val  piramides = LatLng(29.9772962,31.1324955)
        val torre_pisa = LatLng( 43.722952,10.396597)
        // .alpha(0.3F) es solo lo maximo a 1
        //snippet("Metro de san francisco") pequeña descripcion
        marcadorGolden =mMap?.addMarker(MarkerOptions().position(golden_gate).icon(BitmapDescriptorFactory
                .fromResource(R.drawable.icono_tren)).
        snippet("Metro de san francisco").alpha(0.9F).title("Golden Gate"))
        marcadorGolden?.tag=0

        marcadorpiramides =mMap?.addMarker(MarkerOptions().position(piramides).
        icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).alpha(0.3F).
        snippet("Metro de Piramides").title("Piramides"))
        marcadorpiramides?.tag=0

        marcadorpisa =mMap?.addMarker(MarkerOptions().position(torre_pisa).
        icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).alpha(0.6F).
        snippet("Torre de pisa").title("Torre de pisa"))
        marcadorpisa?.tag=0
    }
    // como estamos mapeando
     fun prepararMarcadores(){
        listaMarcadores= ArrayList()
        mMap?.setOnMapLongClickListener {
            location: LatLng? ->
            listaMarcadores?.add(
                    mMap?.addMarker(MarkerOptions().position(location!!).
                    icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).alpha(0.6F).
                    snippet("Torre de pisa").title("Torre de pisa"))!!
            )
            // esto arrastra ultimo marcador
            listaMarcadores?.last()!!.isDraggable=true

            val coordenadas = LatLng(listaMarcadores?.last()!!.position.latitude,listaMarcadores?.last()!!.position.longitude)
            val origen = "origin="+miposcion?.latitude+","+miposcion?.longitude+"&"
            val destino ="destination="+coordenadas.latitude+","+coordenadas.longitude+"&"
            val parametros =origen+destino+"sensor=false&mode=driving"
            val  YOUR_API_KEY =R.string.google_maps_key

            println("https://maps.googleapis.com/maps/api/directions/json?"+parametros+YOUR_API_KEY)
            solicitudvolley("https://maps.googleapis.com/maps/api/directions/json?"+parametros+"&key="+YOUR_API_KEY)
        }
    }


    private fun solicitudvolley(url: String){
        val queue= Volley.newRequestQueue(context)
        val solicitud= StringRequest(Request.Method.GET,url, Response.Listener<String>{
            response ->
            try {
                Log.d("Https",response)
                val coordenadas=obtenerCoordenas(response)
                dibujarRuta(coordenadas)

            }catch (e:Exception){
                println("Error brother")
            }
        }, Response.ErrorListener { })
        queue.add(solicitud)

    }
    private fun dibujarRuta(coordenadas:PolylineOptions){
        if(rutaMarcada!=null){
            rutaMarcada?.remove()
        }
        rutaMarcada= mMap?.addPolyline(coordenadas)
    }
    private fun obtenerCoordenas(respuesta:String):PolylineOptions{
        val gson = Gson()
        val res =gson.fromJson(respuesta,com.example.mapas.Response::class.java)
        val puntos=res.routes?.get(0)!!.legs?.get(0)!!.steps!!
        val coordenasas=PolylineOptions()
        for (i in puntos){
            coordenasas.add(i.start_location?.toLatLng())
            coordenasas.add(i.end_location?.toLatLng())

        }
        coordenasas.color(Color.CYAN).width(15f)
        return coordenasas
    }
     fun configurarMiubcacion(){
        mMap?.isMyLocationEnabled =true
        mMap?.uiSettings?.isMyLocationButtonEnabled =true
    }
    fun añadirmiposicion(){
        mMap?.addMarker(MarkerOptions().position(miposcion!!).title("Aquí estoy!"))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(miposcion))
    }
}