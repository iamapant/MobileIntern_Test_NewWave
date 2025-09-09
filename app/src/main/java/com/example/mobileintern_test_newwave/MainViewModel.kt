package com.example.mobileintern_test_newwave
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {
    val SEARCH_TIMER = 1000L          //Time before starting a new search

    private val _query = mutableStateOf("")
    val query: State<String> get() = _query
    fun setQuery(value: String) { _query.value = value }
    private val _results = mutableStateOf<List<AutocompletePrediction>>(emptyList())
    val results: State<List<AutocompletePrediction>> get() = _results
    fun setResults(value: List<AutocompletePrediction>) { _results.value = value }
    fun clearResults() { setResults(emptyList()) }
    var searchJob: Job? = null
    lateinit var placesClient: PlacesClient
    lateinit var scope: CoroutineScope
    private var _apiKey: String = ""
    fun SetApiKey(value: String) { _apiKey = value }

    fun WaitAndSearchMaps(){
        searchJob?.cancel()
        try {
            searchJob = scope.launch {
                delay(SEARCH_TIMER)
                SearchMaps()
            }
        }
        catch (e: Exception) {
            Log.e("SCOPE", "Coroutine Scope is not available", e)
        }
    }
    private suspend fun SearchMaps(){
        try {
            val req = FindAutocompletePredictionsRequest.builder()
                .setQuery(query.value)
                .build()
            Log.i("REQUEST",req.query?: "")
            placesClient.findAutocompletePredictions(req)
                .addOnSuccessListener { res -> _results.value = res.autocompletePredictions }
                .addOnFailureListener { e ->
                    _results.value = emptyList()
                    Log.e("GOOGLE API","Failed to retrieve search results",e)
                }
                .await()
        }
        catch(e: Exception){
            Log.e("GOOGLE API", "Search throwing exception", e)
        }
    }


    fun OpenPlaceInMap(placeId: String, context: Context){
        val mockUrl = "https://www.google.com/maps/place/%C4%90%C3%AA%CC%80n+Qua%CC%81n+Tha%CC%81nh/@21.0430206,105.8262455,15z/data=!4m6!3m5!1s0x3135aba58b5921c3:0x31329cb3632aabef!8m2!3d21.0430175!4d105.8365462!16s%2Fm%2F09gnzj1?entry=ttu"
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query_place_id=$placeId"
        val intent = Intent(Intent.ACTION_VIEW, mapsUrl.toUri())
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    }
}