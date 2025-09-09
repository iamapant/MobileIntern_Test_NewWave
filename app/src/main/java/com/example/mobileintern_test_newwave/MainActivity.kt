package com.example.mobileintern_test_newwave

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.coroutineScope
import com.example.mobileintern_test_newwave.ui.theme.MobileIntern_Test_NewWaveTheme
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.LanguageCode
import com.here.sdk.core.engine.AuthenticationMode
import com.here.sdk.core.engine.SDKNativeEngine
import com.here.sdk.core.engine.SDKOptions
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.search.SearchEngine
import com.here.sdk.search.SearchOptions
import com.here.sdk.search.Suggestion

private lateinit var viewModel: MainViewModel
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        init()
        setContent {
            MobileIntern_Test_NewWaveTheme {
                Column {
                    SearchBar()
                    Result()
                }
            }
        }
    }

    private fun init(){
        viewModel = MainViewModel()
//        initGoogleMaps()
        initHEREPlatform()
        retrieveUsersLocation()

        viewModel.scope = lifecycle.coroutineScope
    }

    private fun initHEREPlatform(){
        val ai = packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
        val bundle = ai.metaData

        val accessKeyID = bundle.getString("platform.here.ACCESS_KEY_ID") ?: ""
        val accessKeySecret = bundle.getString("platform.here.ACCESS_KEY_SECRET") ?: ""
        val authenticationMode = AuthenticationMode.withKeySecret(accessKeyID, accessKeySecret)
        val apiKey = bundle.getString("platform.here.API_KEY") ?: ""
        val appId = bundle.getString("platform.here.APP_ID") ?: ""
        viewModel.setApiKey(apiKey)
        viewModel.setHEREAppId(appId)

        val options = SDKOptions(authenticationMode)
        try {
            val context = this
            SDKNativeEngine.makeSharedInstance(context, options)
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization of HERE SDK failed: " + e.error.name)
        }

        val searchOptions = SearchOptions()
        searchOptions.languageCode = LanguageCode.VI_VN
        searchOptions.maxItems = 30
        viewModel.setHERESearchOptions(searchOptions)

        val searchEngine = SearchEngine()
        viewModel.setHereSearchEngine(searchEngine)
        viewModel.setCurrentGeoCoordinate(GeoCoordinates(0.0,0.0))
    }

    private fun initGoogleMaps(){
        viewModel = MainViewModel()
        if (!Places.isInitialized()){
            val ai = packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            val key = bundle.getString("com.google.android.geo.API_KEY")
            if (key == null || key.isEmpty()) throw Exception("Key is missing!")
            Places.initialize(applicationContext, key)
        }

        viewModel.placesClient = Places.createClient(this)
    }

    private fun retrieveUsersLocation(){
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1001
                )
                return
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onAddSuccess(location)
                    } else {
                        Log.e("GPS", "Fail to retrieve user location")
                        viewModel?.setCurrentGeoCoordinate(GeoCoordinates(0.0, 0.0))
                    }
                }
                .addOnFailureListener {
                    viewModel?.setCurrentGeoCoordinate(GeoCoordinates(0.0, 0.0))
                }
        }
        catch (e:Exception) {
            Log.e("GPS", "Error retrieving user location", e)
        }
    }

    private fun onAddSuccess(location: Location){
        viewModel?.setCurrentGeoCoordinate(GeoCoordinates(location.latitude, location.longitude))
    }


    private fun disposeHEREPlatform(){
        // Free HERE SDK resources before the application shuts down.
        // Usually, this should be called only on application termination.
        // Afterwards, the HERE SDK is no longer usable unless it is initialized again.
        SDKNativeEngine.getSharedInstance()?.dispose()
        // For safety reasons, we explicitly set the shared instance to null to avoid situations,
        // where a disposed instance is accidentally reused.
        SDKNativeEngine.setSharedInstance(null)
    }

    override fun onDestroy() {
        disposeHEREPlatform()
        super.onDestroy()
    }
}

@Composable
fun SearchBar(){
    val name = viewModel.query.value
    TextField(
        value = name,
        onValueChange = { onSearchValueChanged(it) },
        placeholder = { Text("Location")},
        singleLine = true,
        leadingIcon = { Image(painterResource(R.drawable.magnifier), "Search icon", contentScale = ContentScale.Fit, modifier = Modifier.padding(15.dp, 10.dp)) },
        shape = RoundedCornerShape(100.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,   // remove underline on focus
            unfocusedIndicatorColor = Color.Transparent, // remove underline when not focused
            disabledIndicatorColor = Color.Transparent,  // remove underline when disabled
            cursorColor = Color.Black
        ),
        modifier = Modifier.fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 20.dp, vertical = 30.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(100.dp), clip = false)
    )
}

@Composable
fun Result(){
    val results by viewModel.results
    val resultsHere by viewModel.resultsHERE
    LazyColumn {
        results.forEachIndexed { count, value ->
            items(count = count) {
                ResultItem(value)
            }
        }
        resultsHere.forEachIndexed { count, value ->
            items(count = count) {
                ResultItem(value)
            }
        }
    }
}

@Composable
fun ResultItem(location: AutocompletePrediction){
    val query = viewModel.query.value
    val text = buildAnnotatedString {
        append(viewModel.highlightKeyword(location.getPrimaryText(null).toString(), query))
        append(" ")
        append(viewModel.highlightKeyword(location.getSecondaryText(null).toString(), query, Color.Gray))
    }
    val context = LocalContext.current

    ResultItem(placeId = location.placeId, text = text, context = context)
}

@Composable
fun ResultItem(location: Suggestion){
    val query = viewModel.query.value
    val text = buildAnnotatedString {
        append(viewModel.highlightKeyword(location.title, query))
        append(" ")
        append(viewModel.highlightKeyword(location.place?.address?.addressText ?: "", query, Color.Gray))
    }
    val geoCoordinates = location.place?.geoCoordinates
    if (geoCoordinates == null) return
    val context = LocalContext.current

    ResultItem(geoCoordinates = geoCoordinates, text = text, context = context)
}

@Composable
fun ResultItem(placeId: String? = null, geoCoordinates: GeoCoordinates? = null, text: AnnotatedString, context: Context){
    if (placeId == null && geoCoordinates == null) {
        Log.e("PRINT RESULT", "Cannot retrieve location data")
        return
    }

    var onClick = {}

    if (placeId != null){
        onClick = { viewModel.openPlaceInMap(placeId, context) }
    }

    if (geoCoordinates != null){
        onClick = { viewModel.openPlaceInMap(geoCoordinates.latitude, geoCoordinates.longitude, context) }
    }

    Surface(modifier = Modifier.fillMaxWidth(),
        onClick = onClick) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .height(40.dp)
            .padding(3.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(painterResource(R.drawable.location_pin), "Location pin", contentScale = ContentScale.Fit, modifier = Modifier.padding(5.dp).weight(1f))
            Spacer(Modifier.width(5.dp))
            Text(text, modifier = Modifier.weight(8f), overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.width(5.dp))
            Image(painterResource(R.drawable.road_sign), "Road sign", contentScale = ContentScale.Fit, modifier = Modifier.padding(5.dp).weight(1f))
        }
    }
}

@Preview
@Composable
fun My_Preview(){
    viewModel = MainViewModel()
    MobileIntern_Test_NewWaveTheme {
        Column (modifier = Modifier.background(Color.LightGray)) {
            SearchBar()
            Result()
            ResultItem_Mock()
        }
    }
}

@Composable
fun ResultItem_Mock(){
    val placeId = "ChIJwyFZi6WrNTER76sqY7OcMjE"
    val primaryText = "Đền Quán Thánh"
    val secondaryText = "190 P. Quán Thánh, Quán Thánh, Ba Đình, Hà Nội 118810, Việt Nam"
    val keyword = "Thánh"
    val context = LocalContext.current

    val text = buildAnnotatedString {
        append(viewModel.highlightKeyword(primaryText, keyword))
        append(" ")
        append(viewModel.highlightKeyword(secondaryText, keyword, Color.Gray))
    }

    ResultItem(placeId = placeId, text = text, context = context)
}


private fun onSearchValueChanged(value: String){
    viewModel.setQuery(value)

    if (!value.isEmpty()){
        viewModel.waitAndSearchMaps()
    }
    else {
        viewModel.clearResults()
    }
}
