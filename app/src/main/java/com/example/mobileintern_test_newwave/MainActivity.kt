package com.example.mobileintern_test_newwave

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableString
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
                    ResultItem_Mock()
                }
            }
        }
    }

    private fun init(){
        viewModel = MainViewModel()
        if (!Places.isInitialized()){
            val ai = packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            val key = bundle.getString("com.google.android.geo.API_KEY")
            if (key == null || key.isEmpty()) throw Exception("Key is missing!")
            Places.initialize(applicationContext, key)
        }

        viewModel.placesClient = Places.createClient(this)
        viewModel.scope = lifecycle.coroutineScope
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
        results.forEachIndexed { count, value ->
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
        append(HighlightKeyword(location.getPrimaryText(null), query))
        append(" ")
        append(HighlightKeyword(location.getSecondaryText(null), query, Color.Gray))
    }
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth(),
        onClick ={ viewModel.OpenPlaceInMap(location.placeId, context) }) {
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
        onClick ={ viewModel.OpenPlaceInMap(placeId, context) }) {
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
    viewModel.setQuery(value)

    if (!value.isEmpty()){
        viewModel.WaitAndSearchMaps()
    }
    else {
        viewModel.clearResults()
    }
}
