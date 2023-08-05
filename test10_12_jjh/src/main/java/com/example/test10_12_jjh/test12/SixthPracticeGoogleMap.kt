package com.example.test10_12_jjh.test12

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.test10_12_jjh.R
import com.example.test10_12_jjh.adapter.APISlidingAdapter
import com.example.test10_12_jjh.databinding.ActivityGoogleMapBottomSheetDialogFragmentBinding
import com.example.test10_12_jjh.model.TempModel
import com.example.test10_12_jjh.model.TideModel
import com.example.test10_12_jjh.model.TidePreModel
import com.github.mikephil.charting.data.Entry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SixthPracticeGoogleMap : AppCompatActivity(), OnMapReadyCallback {
    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null
    lateinit var myadapter : APISlidingAdapter
    lateinit var binding : ActivityGoogleMapBottomSheetDialogFragmentBinding
    //lateinit var binding : ActivitySixthPracticeGoogleMapBinding

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    //private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private val defaultLocation = LatLng(35.2100, 129.0689)
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)

    data class MapLocation (
        var obscode : String,
        var obsname : String,
        var latitude : Double,
        var longtitude : Double)

    data class XYGrid (var x : Double, var y : Double)



    // [START maps_current_place_on_create]
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = ActivitySixthPracticeGoogleMapBinding.inflate(layoutInflater)
        // [START_EXCLUDE silent]
        // Retrieve location and camera position from saved instance state.
        // [START maps_current_place_on_create_save_instance_state]
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }
        // [END maps_current_place_on_create_save_instance_state]
        // [END_EXCLUDE]

//        myadapter = APISlidingAdapter(this)
        binding = ActivityGoogleMapBottomSheetDialogFragmentBinding.inflate(layoutInflater)
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_sixth_practice_google_map)

        // [START_EXCLUDE silent]
        // Construct a PlacesClient
        Places.initialize(applicationContext, "BuildConfig.MAPS_API_KEY")
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Build the map.
        // [START maps_current_place_map_fragment]
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        // [END maps_current_place_map_fragment]
        // [END_EXCLUDE]

    }
    // [END maps_current_place_on_create]

    /**
     * Saves the state of the map when the activity is paused.
     */
    // [START maps_current_place_on_save_instance_state]
    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }
    // [END maps_current_place_on_save_instance_state]

    /**
     * Sets up the options menu.
     * @param menu The options menu.
     * @return Boolean.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.current_place_menu, menu)
        return true
    }

    /**
     * Handles a click on the menu option to get a place.
     * @param item The menu item to handle.
     * @return Boolean.
     */
    // [START maps_current_place_on_options_item_selected]
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.option_get_place) {
            showCurrentPlace()
        }
        return true
    }
    // [END maps_current_place_on_options_item_selected]

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */

    // [START maps_current_place_on_map_ready]
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMapReady(map: GoogleMap) {
        this.map = map
        // [START_EXCLUDE]
        // [START map_current_place_set_info_window_adapter]
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.map?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow = layoutInflater.inflate(R.layout.custom_info_contents,
                    findViewById<FrameLayout>(R.id.map), false)
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                snippet.text = marker.snippet
                return infoWindow
            }
        })
        // [END map_current_place_set_info_window_adapter]

        // Prompt the user for permission.
        getLocationPermission()
        // [END_EXCLUDE]

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()

        var locations = listOf(
            MapLocation("DT_0054", "진해", 35.147, 128.643),
            MapLocation("DT_0004", "제주", 33.527, 126.543),
            MapLocation("DT_0005", "부산", 35.096, 129.035),
            MapLocation("DT_0007", "목포", 34.779, 126.375),
            MapLocation("DT_0012", "속초", 38.207, 128.594),
            MapLocation("DT_0016", "여수", 34.747, 127.765),
            MapLocation("DT_0027", "완도", 34.315, 126.759),
            MapLocation("DT_0028", "진도", 34.377, 126.308),
            MapLocation("DT_0029", "거제도", 34.801, 128.699),
            MapLocation("DT_0031", "거문도", 34.028, 127.308),
            MapLocation("DT_0035", "흑산도", 34.684, 125.435),
            MapLocation("DT_0050", "태안", 36.913, 126.238),
            MapLocation("DT_0056", "부산항 신항", 35.077, 128.768)

        )

        val markers = mutableListOf<Marker>()

        locations.forEach {
            val marker = map?.addMarker(
                MarkerOptions()
                    .title(it.obsname)
                    .position(LatLng(it.latitude, it.longtitude)))
            marker?.tag = it
            Log.d("googel", marker?.tag.toString())
            markers.add(marker!!)
        }

        map.setOnCameraIdleListener {
            for(marker in markers) {
                if(map.projection.visibleRegion.latLngBounds.contains(marker.position))
                    marker.showInfoWindow()
                else
                    marker.hideInfoWindow()
            }
        }

        map.setOnMarkerClickListener {
            clickedMarker -> showMarkerInfo(clickedMarker)
            true
        }

    }
    // [END maps_current_place_on_map_ready]

    @RequiresApi(Build.VERSION_CODES.O)
    fun showMarkerInfo(marker : Marker) {
        Log.d("google22", marker.title!!)
        Log.d("google22", marker.position.latitude.toString())
        Log.d("google22", marker.position.longitude.toString())

        val apikey = "/FFdZti8UpV2Ku/EnEYvg=="
        //val obscode = "DT_0001"
        val resulttype = "json"
        val today = LocalDateTime.now()
        val firstDay = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val secondDay = today.plusDays(1L).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val thirdDay = today.plusDays(2L).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val fourthDay = today.plusDays(3L).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val fifthDay = today.plusDays(4L).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val sixthDay = today.plusDays(5L).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val seventhDay = today.plusDays(6L).format(DateTimeFormatter.ofPattern("yyyyMMdd"))


        val networkService = (applicationContext as APIApplication).networkService
        val tidelist = mutableListOf<TideModel>()
        //val myadapter = APIAdapter(this@SixPracticeGoogleMap, tidelist)
        //Log.d("google22", marker?.tag.toString())
        val mytag : MapLocation = (marker?.tag!! as MapLocation)
        val mytide1 = networkService.getTide(apikey, mytag.obscode, firstDay, resulttype)
        val mytide2 = networkService.getTide(apikey, mytag.obscode, secondDay, resulttype)
        val mytide3 = networkService.getTide(apikey, mytag.obscode, thirdDay, resulttype)
        val mytide4 = networkService.getTide(apikey, mytag.obscode, fourthDay, resulttype)
        val mytide5 = networkService.getTide(apikey, mytag.obscode, fifthDay, resulttype)
        val mytide6 = networkService.getTide(apikey, mytag.obscode, sixthDay, resulttype)
        val mytide7 = networkService.getTide(apikey, mytag.obscode, seventhDay, resulttype)
        val mytemp1 = networkService.getTemp(apikey, mytag.obscode, resulttype)
        val nowtide = networkService.getPreTide(apikey, mytag.obscode, firstDay, resulttype)

//        myadapter = APISlidingAdapter(this,tidelist)


        mytide1.enqueue(object : Callback<TideModel> {
            override fun onResponse(call: Call<TideModel>, response: Response<TideModel>) {
                tidelist.clear()
                Log.d("google22", "${tidelist.size}")
                val tide = response.body()
                Log.d("google22", "mytide1")
                tidelist.add(tide!!)
                mytide2.enqueue(object : Callback<TideModel> {
                    override fun onResponse(call : Call<TideModel>, response : Response<TideModel>) {
                        val tide = response.body()
                        tidelist.add(tide!!)
                        Log.d("google22", "mytide2")
                        mytide3.enqueue(object : Callback<TideModel> {
                            override fun onResponse(call : Call<TideModel>, response : Response<TideModel>) {
                                val tide = response.body()
                                tidelist.add(tide!!)
                                Log.d("google22", "mytide3")
                                mytide4.enqueue(object : Callback<TideModel> {
                                    override fun onResponse(call : Call<TideModel>, response : Response<TideModel>) {
                                        val tide = response.body()
                                        tidelist.add(tide!!)
                                        Log.d("google22", "mytide4")
                                        mytide5.enqueue(object : Callback<TideModel> {
                                            override fun onResponse(call : Call<TideModel>, response : Response<TideModel>) {
                                                val tide = response.body()
                                                tidelist.add(tide!!)
                                                Log.d("google22", "mytide5")
                                                mytide6.enqueue(object : Callback<TideModel> {
                                                    override fun onResponse(call : Call<TideModel>, response : Response<TideModel>) {
                                                        val tide = response.body()
                                                        tidelist.add(tide!!)
                                                        Log.d("google22", "mytide6")
                                                        mytide7.enqueue(object :
                                                            Callback<TideModel> {
                                                            override fun onResponse(call : Call<TideModel>, response : Response<TideModel>) {
                                                                val tide = response.body()
                                                                tidelist.add(tide!!)
                                                                Log.d("google22", "mytide7")
                                                                Log.d("google22", "$tidelist")
                                                                mytemp1.enqueue(object : Callback<TempModel> {
                                                                    override fun onResponse(call: Call<TempModel>, response: Response<TempModel>) {
                                                                        val temp = response.body()
                                                                        Log.d("google22", "$temp")
                                                                        var levels = mutableListOf<Entry>()
                                                                        nowtide.enqueue(object : Callback<TidePreModel> {
                                                                            override fun onResponse(call : Call<TidePreModel>, response: Response<TidePreModel>) {
                                                                                val pretide = response.body()
                                                                                Log.d("google22", "$pretide")
                                                                                var i = 0
                                                                                for(s in pretide?.result?.data!!) {
                                                                                    levels.add(Entry(i.toFloat(), s.tidelevel!!.toFloat()))
                                                                                    i++
                                                                                }
                                                                                val bottomsheetdialog = BottomSheetDialog(tidelist, levels)
                                                                                bottomsheetdialog.show(supportFragmentManager, "bottomsheetdialog")
                                                                            }

                                                                            override fun onFailure(call : Call<TidePreModel>, t : Throwable) {
                                                                                call.cancel()
                                                                            }
                                                                        })

                                                                    }
                                                                    override fun onFailure(call: Call<TempModel>, t: Throwable) {
                                                                        Log.d("google22", "failed")
                                                                        call.cancel()
                                                                    }
                                                                })
                                                            }
                                                            override fun onFailure(call: Call<TideModel>, t: Throwable) {
                                                                Log.d("apitest", "failed")
                                                                call.cancel()
                                                            }
                                                        })
                                                    }
                                                    override fun onFailure(call: Call<TideModel>, t: Throwable) {
                                                        Log.d("apitest", "failed")
                                                        call.cancel()
                                                    }
                                                })
                                            }
                                            override fun onFailure(call: Call<TideModel>, t: Throwable) {
                                                Log.d("apitest", "failed")
                                                call.cancel()
                                            }
                                        })
                                    }
                                    override fun onFailure(call: Call<TideModel>, t: Throwable) {
                                        Log.d("apitest", "failed")
                                        call.cancel()
                                    }
                                })
                            }
                            override fun onFailure(call: Call<TideModel>, t: Throwable) {
                                Log.d("apitest", "failed")
                                call.cancel()
                            }
                        })

                    }
                    override fun onFailure(call: Call<TideModel>, t: Throwable) {
                        Log.d("apitest", "failed")
                        call.cancel()
                    }
                })
            }
            override fun onFailure(call: Call<TideModel>, t: Throwable) {
                Log.d("apitest", "failed")
                call.cancel()
            }
        })
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */

    fun mapToGrid(lat : Double, lon : Double) : XYGrid{
        var RE = 6371.00877 // 지구 반경(km)
        var GRID = 5.0 // 격자 간격(km)
        var SLAT1 = 30.0 // 투영 위도1(degree)
        var SLAT2 = 60.0 // 투영 위도2(degree)
        var OLON = 126.0 // 기준점 경도(degree)
        var OLAT = 38.0 // 기준점 위도(degree)
        var XO = 43 // 기준점 X좌표(GRID)
        var YO = 136 // 기1준점 Y좌표(GRID)
        var DEGRAD = Math.PI / 180.0
        var RADDEG = 180.0 / Math.PI

        var re = RE / GRID
        var slat1 = SLAT1 * DEGRAD
        var slat2 = SLAT2 * DEGRAD
        var olon = OLON * DEGRAD
        var olat = OLAT * DEGRAD

        var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
        var ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
        ro = re * sf / Math.pow(ro, sn)

        var ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5)
        ra = re * sf / Math.pow(ra, sn)
        var theta = lon * DEGRAD - olon
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        val x = Math.floor(ra * Math.sin(theta) + XO + 0.5);
        val y = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return XYGrid(x, y)
    }

    // [START maps_current_place_get_device_location]
    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(
                            CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }
    // [END maps_current_place_get_device_location]

    /**
     * Prompts the user for permission to use the device location.
     */
    // [START maps_current_place_location_permission]
    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }
    // [END maps_current_place_location_permission]

    /**
     * Handles the result of the request for location permissions.
     */
    // [START maps_current_place_on_request_permissions_result]
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }
    // [END maps_current_place_on_request_permissions_result]

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    // [START maps_current_place_show_current_place]
    @SuppressLint("MissingPermission")
    private fun showCurrentPlace() {
        if (map == null) {
            return
        }
        if (locationPermissionGranted) {
            // Use fields to define the data types to return.
            val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)

            // Use the builder to create a FindCurrentPlaceRequest.
            val request = FindCurrentPlaceRequest.newInstance(placeFields)

            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            val placeResult = placesClient.findCurrentPlace(request)
            placeResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val likelyPlaces = task.result

                    // Set the count, handling cases where less than 5 entries are returned.
                    val count = if (likelyPlaces != null && likelyPlaces.placeLikelihoods.size < M_MAX_ENTRIES) {
                        likelyPlaces.placeLikelihoods.size
                    } else {
                        M_MAX_ENTRIES
                    }
                    var i = 0
                    likelyPlaceNames = arrayOfNulls(count)
                    likelyPlaceAddresses = arrayOfNulls(count)
                    likelyPlaceAttributions = arrayOfNulls<List<*>?>(count)
                    likelyPlaceLatLngs = arrayOfNulls(count)
                    for (placeLikelihood in likelyPlaces?.placeLikelihoods ?: emptyList()) {
                        // Build a list of likely places to show the user.
                        likelyPlaceNames[i] = placeLikelihood.place.name
                        likelyPlaceAddresses[i] = placeLikelihood.place.address
                        likelyPlaceAttributions[i] = placeLikelihood.place.attributions
                        likelyPlaceLatLngs[i] = placeLikelihood.place.latLng
                        i++
                        if (i > count - 1) {
                            break
                        }
                    }

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    openPlacesDialog()
                } else {
                    Log.e(TAG, "Exception: %s", task.exception)
                }
            }
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.")

            // Add a default marker, because the user hasn't selected a place.
            map?.addMarker(
                MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(defaultLocation)
                    .snippet(getString(R.string.default_info_snippet)))

            // Prompt the user for permission.
            getLocationPermission()
        }
    }
    // [END maps_current_place_show_current_place]

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    // [START maps_current_place_open_places_dialog]
    private fun openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        val listener = DialogInterface.OnClickListener { dialog, which -> // The "which" argument contains the position of the selected item.
            val markerLatLng = likelyPlaceLatLngs[which]
            var markerSnippet = likelyPlaceAddresses[which]
            if (likelyPlaceAttributions[which] != null) {
                markerSnippet = """
                    $markerSnippet
                    ${likelyPlaceAttributions[which]}
                    """.trimIndent()
            }

            if (markerLatLng == null) {
                return@OnClickListener
            }

            // Add a marker for the selected place, with an info window
            // showing information about that place.
            map?.addMarker(
                MarkerOptions()
                    .title(likelyPlaceNames[which])
                    .position(markerLatLng)
                    .snippet(markerSnippet))

            // Position the map's camera at the location of the marker.
            map?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(markerLatLng,
                    DEFAULT_ZOOM.toFloat()))
        }

        // Display the dialog.
        AlertDialog.Builder(this)
            .setTitle(R.string.pick_place)
            .setItems(likelyPlaceNames, listener)
            .show()
    }
    // [END maps_current_place_open_places_dialog]

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    // [START maps_current_place_update_location_ui]
    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }
    // [END maps_current_place_update_location_ui]

    companion object {
        private val TAG = SixthPracticeGoogleMap::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        // [END maps_current_place_state_keys]

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5
    }
}