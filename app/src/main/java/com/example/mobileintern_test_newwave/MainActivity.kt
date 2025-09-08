package com.example.mobileintern_test_newwave

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableString
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.coroutineScope
import com.example.mobileintern_test_newwave.ui.theme.MobileIntern_Test_NewWaveTheme
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri


private val query = mutableStateOf("")
private val results = mutableStateOf<List<AutocompletePrediction>>(emptyList())
private val _searchTimer = 1000L          //Time before starting a new search
private var _searchJob: Job? = null
private lateinit var placesClient: PlacesClient
private var scope: CoroutineScope? = null
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
                    ResultItem_Mock()
                }
            }
        }
    }

    private fun init(){
        if (!Places.isInitialized()){
            val ai = packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            val key = bundle.getString("com.google.android.geo.API_KEY")
            if (key == null || key.isEmpty()) throw Exception("Key is missing!")
            Places.initialize(applicationContext, key)
        }

        placesClient = Places.createClient(this)
        scope = lifecycle.coroutineScope;
    }
}

@Composable
fun SearchBar(){
    var name by query
    TextField(
        value = name,
        onValueChange = { onSearchValueChanged(it) },
        label = { Text("Location")},
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 30.dp)
    )
}

@Composable
fun Result(){
    val offset = remember { mutableStateOf(0f) }
    LazyColumn(modifier = Modifier
//        .scrollable(
//        orientation = Orientation.Vertical,
//        // state for Scrollable, describes how consume scroll amount
//        state =
//            rememberScrollableState { delta ->
//                // use the scroll data and indicate how much this element consumed.
//                // unconsumed deltas will be propagated to nested scrollables (if present)
//                offset.value = offset.value + delta // update the state
//                delta // indicate that we consumed all the pixels available
//            },
//            )
    ) {
        results.value.forEachIndexed { count, value ->
            items(count = count) {
                ResultItem(value)
            }
        }
    }
}

@Composable
fun ResultItem(location: AutocompletePrediction){
    val text = buildAnnotatedString {
        append(HighlightKeyword(location.getPrimaryText(null), query.value))
        append(" ")
        append(HighlightKeyword(location.getSecondaryText(null), query.value, Color.Gray))
    }
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth(),
        onClick ={ OpenPlaceInMap(location.placeId, context) }) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .height(40.dp)
            .padding(3.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(painterResource(R.drawable.location_pin), "Location pin", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(5.dp))
            Text(text, modifier = Modifier.weight(8f), overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.width(5.dp))
            Image(painterResource(R.drawable.road_sign), "Road sign", modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
fun My_Preview(){
    MobileIntern_Test_NewWaveTheme {
        Column {
            SearchBar()
            Result()
            ResultItem_Mock()
        }
    }
}

@Composable
fun ResultItem_Mock(){
    val placeId = "ChIJwyFZi6WrNTER76sqY7OcMjE"
    val primaryText = SpannableString("Đền Quán Thánh")
    val secondaryText = SpannableString("190 P. Quán Thánh, Quán Thánh, Ba Đình, Hà Nội 118810, Việt Nam")
    val keyword = "Thánh"
    val context = LocalContext.current

    val text = buildAnnotatedString {
        append(HighlightKeyword(primaryText, keyword))
        append(" ")
        append(HighlightKeyword(secondaryText, keyword, Color.Gray))
    }
    Surface(modifier = Modifier.fillMaxWidth(),
        onClick ={ OpenPlaceInMap(placeId, context) }) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .height(40.dp)
            .padding(3.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(painterResource(R.drawable.location_pin), "Location pin", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(5.dp))
            Text(text, modifier = Modifier.weight(8f), overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.width(5.dp))
            Image(painterResource(R.drawable.road_sign), "Road sign", modifier = Modifier.weight(1f))
        }
    }
}

fun OpenPlaceInMap(placeId: String, context: Context){
    val mockUrl = "https://www.google.com/maps/place/%C4%90%C3%AA%CC%80n+Qua%CC%81n+Tha%CC%81nh/@21.0430206,105.8262455,15z/data=!4m6!3m5!1s0x3135aba58b5921c3:0x31329cb3632aabef!8m2!3d21.0430175!4d105.8365462!16s%2Fm%2F09gnzj1?entry=ttu"
    val mapsUrl = "https://www.google.com/maps/search/?api=1&query_place_id=$placeId"
    val intent = Intent(Intent.ACTION_VIEW, mockUrl.toUri())
    intent.setPackage("com.google.android.apps.maps")
    context.startActivity(intent)
}

private fun HighlightKeyword(text: SpannableString, keyword: String, color: Color? = null): AnnotatedString{
    if (text.isEmpty()) return buildAnnotatedString { append(text) }
    val name = "Long"
    var startIndex = 0

    val anno = buildAnnotatedString {
        while (true) {
            val index = text.indexOf(keyword, startIndex, ignoreCase = true)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }

            append(text.substring(startIndex, index))

            // Append highlighted keyword
            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                append(text.substring(index, index + keyword.length))
            }

            startIndex = index + keyword.length
        }
    }
    if (color != null) return buildAnnotatedString { withStyle(style = SpanStyle(color = color)){append(anno)} }

    return anno
}

private fun onSearchValueChanged(value: String){
    query.value = value

    if (!value.isEmpty()){
        WaitAndSearchGoogleMaps()
    }
    else {
        results.value = emptyList()
    }
}
private fun WaitAndSearchGoogleMaps(){
    _searchJob?.cancel()
    if (scope != null){
        _searchJob = scope?.launch {
            delay(_searchTimer)
            SearchGoogleMaps()
        }
    }
    else Log.e("SCOPE", "Coroutine Scope is not available")
}

private suspend fun SearchGoogleMaps(){
    try {
        val req = FindAutocompletePredictionsRequest.builder()
            .setQuery(query.value)
            .build()
        Log.i("REQUEST",req.query?: "")
        placesClient.findAutocompletePredictions(req)
            .addOnSuccessListener { res -> results.value = res.autocompletePredictions }
            .addOnFailureListener { e ->
                results.value = emptyList()
                Log.e("GOOGLE API","Failed to retrieve search results",e)
            }
            .await()
    }
    catch(e: Exception){
        Log.e("GOOGLE API", "Search throwing exception", e)
    }
}