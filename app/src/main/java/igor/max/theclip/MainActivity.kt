package igor.max.theclip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import igor.max.theclip.ui.theme.TheClipTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            TheClipTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(viewModel)
                }
            }
        }
    }

}


@Composable
private fun Content(
    ip: String,
    startServer: () -> Unit = {},
    startClient: () -> Unit = {},
) {

    Column(Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            text = ip
        )
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            ClipButton("Start Server") {
                startServer()
            }
            ClipButton("Start Client") {
                startClient()
            }
        }

    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainContent(viewModel: MainViewModel) {
    val feed = viewModel.clipboardFeed.observeAsState()
    val mode = viewModel.mode.observeAsState()
    val service = viewModel.serverInfo.observeAsState()
    val ip = rememberSaveable { viewModel.getDeviceIp() }

    Column(Modifier.fillMaxWidth()) {
        service.value?.let {
            SuggestionChip(
                modifier = Modifier.wrapContentSize().padding(8.dp),
                onClick = { viewModel.startClient() },
                label = { Text("Server ${it.host}") },
                icon = {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = "Localized description",
                        Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
        }
        Content(
            ip = ip,
            startServer = { viewModel.startServer() },
            startClient = { viewModel.startClient() },
        )

        when (mode.value) {
            Mode.SERVER -> ServerLayout(feed.value.orEmpty())
            Mode.CLIENT -> ClientLayout { text ->
                viewModel.sendText(text)
            }

            Mode.UNSPECIFIED -> Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                text = "Start a server or a client.\nif a server is already present starting a server will divert to a client and connect to that server instead."
            )

            null -> throw IllegalStateException("Mode should never be null")
        }

    }


}

@Composable
fun ServerLayout(feed: List<String>) {
    TextField(
        value = feed.joinToString(separator = "\n"),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 20.dp),
        singleLine = false,
        enabled = false,
        onValueChange = {},
        label = {
            Text(text = "Shared content")
        })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClientLayout(callback: (String) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var text by rememberSaveable { mutableStateOf("") }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                onValueChange = { text = it },
                label = { Text("Type to send") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        callback(text)
                        text = ""
                    }
                )
            )
            Button(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                onClick = {
                    callback(text)
                    text = ""
                }) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun ClipButton(name: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    var enabled by rememberSaveable { mutableStateOf(true) }

    Button(
        colors = ButtonDefaults.buttonColors(),
        enabled = enabled,
        onClick = {
            onClick.invoke()
            enabled = false
        }) {
        Text(
            text = name,
            modifier = Modifier.wrapContentWidth()
        )
    }

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TheClipTheme {
        Content(ip = "192.168.1.25")
    }
}