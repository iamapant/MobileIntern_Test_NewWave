package com.example.mobileintern_test_newwave
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.search.SearchEngine
import com.here.sdk.search.SearchOptions
import com.here.sdk.search.SuggestCallback
import com.here.sdk.search.Suggestion
import com.here.sdk.search.TextQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.Collator
import java.util.Locale

class MainViewModel : ViewModel() {
    val SEARCH_TIMER = 1000L          //Time before starting a new search

    private val _query = mutableStateOf("")
    val query: State<String> get() = _query
    fun setQuery(value: String) { _query.value = value }
    private val _results = mutableStateOf<List<AutocompletePrediction>>(emptyList())
    val results: State<List<AutocompletePrediction>> get() = _results
    fun setResults(value: List<AutocompletePrediction>) { _results.value = value }
    fun clearResults() { setResults(emptyList()) }
    private val _resultsHERE = mutableStateOf<List<Suggestion>>(emptyList())
    val resultsHERE: State<List<Suggestion>> get() = _resultsHERE
    fun setHEREResults(value: List<Suggestion>) { _resultsHERE.value = value }
    fun clearHEREResults() { setHEREResults(emptyList()) }
    var searchJob: Job? = null
    lateinit var placesClient: PlacesClient
    lateinit var scope: CoroutineScope
    private var _apiKey: String = ""
    fun setApiKey(value: String) { _apiKey = value }
    private var _hereAppId: String = ""
    fun setHEREAppId(value: String) { _hereAppId = value }
    private var _searchOptions: SearchOptions? = null
    fun setHERESearchOptions(options: SearchOptions) { _searchOptions = options }
    private var _searchEngine: SearchEngine? = null
    fun setHereSearchEngine(engine: SearchEngine) { _searchEngine = engine }
    private lateinit var _geoCode: GeoCoordinates
    fun setCurrentGeoCoordinate(value: GeoCoordinates) { _geoCode = value }

    fun waitAndSearchMaps(){
        searchJob?.cancel()
        try {
            searchJob = scope.launch {
                delay(SEARCH_TIMER)
                searchMaps()
            }
        }
        catch (e: Exception) {
            Log.e("SCOPE", "Coroutine Scope is not available", e)
        }
    }
    private suspend fun searchMaps(){
        try {
            hereMapsSearch()
            //GoogleMapsSearch()
        }
        catch(e: Exception){
            Log.e("GOOGLE API", "Search throwing exception", e)
        }
    }

    private suspend fun googleMapsSearch(){
        if (query.value.isEmpty()){
            setResults(emptyList())
            return
        }

        val req = FindAutocompletePredictionsRequest.builder()
            .setQuery(query.value)
            .build()
        placesClient.findAutocompletePredictions(req)
            .addOnSuccessListener { res -> _results.value = res.autocompletePredictions }
            .addOnFailureListener { e ->
                _results.value = emptyList()
                Log.e("GOOGLE API","Failed to retrieve search results",e)
            }
            .await()
    }

    private suspend fun hereMapsSearch(){
        if (query.value.isEmpty()) {
            setHEREResults(emptyList())
            return
        }
        val suggestCallback = SuggestCallback{e, results ->
            if (e != null){
                Log.e("HERE API", e.name)
                setHEREResults(emptyList())
                return@SuggestCallback
            }

            setHEREResults(results?.filterNotNull() ?: emptyList())
        }
        val textQuery = TextQuery(query.value, TextQuery.Area(_geoCode))
        _searchEngine!!.suggestByText(textQuery, _searchOptions ?: SearchOptions(), suggestCallback)
    }

    fun openPlaceInMap(lat:Double, long:Double, context: Context) {
        val mapsUrl = "https://maps.google.com/?q=$lat, $long".toUri()
        val intent = Intent(Intent.ACTION_VIEW, mapsUrl)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, mapsUrl)
            context.startActivity(browserIntent)
        }
    }

    fun openPlaceInMap(placeId: String, context: Context){
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query_place_id=$placeId".toUri()
        val intent = Intent(Intent.ACTION_VIEW, mapsUrl)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, mapsUrl)
            context.startActivity(browserIntent)
        }
    }
    fun highlightKeyword(text: String, keyword: String, color: Color = Color.Black): AnnotatedString{
        if (text.isEmpty() || keyword.isEmpty()) return buildAnnotatedString { append(text) }
        var startIndex = getKeywordIndex(text, keyword)
        return buildAnnotatedString {
            withStyle(style = SpanStyle(color = color)) {
                append(text)
            }

            while (startIndex >= 0) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    startIndex,
                    startIndex + keyword.length
                )
                startIndex = getKeywordIndex(text, keyword, startIndex + keyword.length + 1)
            }
        }
    }

    fun getKeywordIndex(text: String, keyword: String, startIndex: Int = 0, locale: Locale = Locale.getDefault()): Int {
        if (startIndex > text.length) return -1

        val collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY // ignores case and accents
        }

        val t = text.substring(startIndex)

        for (i in 0..t.length - keyword.length) {
            val slice = t.substring(i, i + keyword.length)
            if (collator.compare(slice, keyword) == 0) {
                return i + startIndex
            }
        }
        return -1
    }
}