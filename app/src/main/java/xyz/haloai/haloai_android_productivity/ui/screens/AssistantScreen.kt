package xyz.haloai.haloai_android_productivity.xyz.haloai.haloai_android_productivity.data.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import xyz.haloai.haloai_android_productivity.HaloAI
import xyz.haloai.haloai_android_productivity.R
import xyz.haloai.haloai_android_productivity.data.ui.theme.HaloAI_Android_ProductivityTheme
import xyz.haloai.haloai_android_productivity.ui.viewmodel.AssistantModeFunctionsViewModel
import xyz.haloai.haloai_android_productivity.ui.viewmodel.VoiceTranscriptionViewModel

val conversation: StateFlow<List<ChatHistory.Message>>
    get() = _conversation

private val _conversation = MutableStateFlow(
    listOf(
        ChatHistory.Message.initConv
    )
)

@Composable
fun AssistantScreen(navController: NavController) {

    var isLoading by remember { mutableStateOf(false) }
    val assistantModeFunctionsViewModel: AssistantModeFunctionsViewModel = koinViewModel()
    val onUserInput = { msg: String ->
        _conversation.value += ChatHistory.Message(
            text = msg,
            isUserMessage = true
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val conversationState by _conversation.collectAsState()

    fun resetConversation() {
        _conversation.value = listOf(
            ChatHistory.Message.initConv
        )
    }

    LaunchedEffect(key1 = conversationState) {
        // Parse last message, if it is a user message, send it to the AI
        val lastMessage = conversationState.last()
        if (lastMessage.isFromMe) {
            isLoading = true
            coroutineScope.launch {
                val response = assistantModeFunctionsViewModel.ask_ai(conversation = conversationState)
                _conversation.value += ChatHistory.Message(
                    text = response,
                    isUserMessage = false
                )
                isLoading = false
            }
        }
    }

    // Use DisposableEffect to save the note when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            resetConversation()
        }
    }

    // Use OnLifecycleEvent to save the note when the lifecycle event occurs
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_ANY) {
                resetConversation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Add a listener to save the note when the user navigates away
    DisposableEffect(navController) {
        val callback = NavController.OnDestinationChangedListener { _, _, _ ->
            resetConversation()
        }
        navController.addOnDestinationChangedListener(callback)

        onDispose {
            navController.removeOnDestinationChangedListener(callback)
        }
    }

    HaloAI_Android_ProductivityTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ChatThread(
                    modifier = Modifier.fillMaxSize(),
                    model = ChatHistory(
                        messages = conversation.collectAsState().value
                    ),
                    onUserInput = onUserInput
                )
                if (isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun MessageBox(text: String, isFromMe: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = 48f,
                    topEnd = 48f,
                    bottomStart = if (isFromMe) 48f else 0f,
                    bottomEnd = if (isFromMe) 0f else 48f
                )
            )
            .background(
                if (isFromMe) MaterialTheme.colorScheme.primaryContainer else
                    MaterialTheme.colorScheme.tertiaryContainer
            )
            .padding(8.dp)
    ) {
        Text(text = text, color = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else
            MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

data class ChatHistory(
    val messages: List<Message>) {
    data class Message(
        val text: String,
        val isUserMessage: Boolean,
    ) {
        val isFromMe: Boolean
            get() = isUserMessage

        companion object {
            val initConv = Message(
                text = "Hey there. Tell me what you'd like me to take care of, I'm here to help!",
                isUserMessage = false
            )
            val initConvResp = Message(
                text = "I'm doing great, how about you?",
                isUserMessage = true
            )
        }
    }
}

@Composable
fun ChatThread(modifier: Modifier, model: ChatHistory, onUserInput: (String) -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {

        LazyColumn {
            items(model.messages) { item ->
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)) {
                    if (item.isUserMessage)
                        MessageBox(item.text, item.isUserMessage, modifier = Modifier.align(Alignment.CenterEnd))
                    else
                        MessageBox(item.text, item.isUserMessage, modifier = Modifier.align(Alignment.CenterStart))
                    // MessageBox(item)
                }
            }
        }

        InputBar(modifier = Modifier.align(Alignment.BottomCenter), onUserInput = onUserInput)
    }
}

@Composable
fun InputBar(modifier: Modifier, onUserInput: (String) -> Unit) {
    var searchState by remember { mutableStateOf(TextFieldValue("")) }
    val voiceTranscriptionViewModel: VoiceTranscriptionViewModel = koinInject { parametersOf(HaloAI.activityResultReg) }
    val voiceInput by voiceTranscriptionViewModel.voiceInput.collectAsState()

    LaunchedEffect(voiceInput) {
        if (voiceInput.isNotEmpty()) {
            onUserInput(voiceInput)
            voiceTranscriptionViewModel.updateVoiceInput("") // Clear after processing
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.haloai_logo),
                contentDescription = "Search Icon",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
            )
            BasicTextField(
                value = searchState,
                onValueChange = { searchState = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                singleLine = true
            )
            IconButton(
                onClick = {
                    voiceTranscriptionViewModel.startListening()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_mic_24),
                    contentDescription = "Voice Icon",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(30.dp)
                )
            }
            IconButton(
                onClick = {
                              onUserInput(searchState.text)
                              searchState = TextFieldValue("")
                          },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Arrow Icon",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(30.dp)
                )
            }
        }
    }
}

