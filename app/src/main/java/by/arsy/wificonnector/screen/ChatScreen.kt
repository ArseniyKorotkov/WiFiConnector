package by.arsy.wificonnector.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.arsy.wificonnector.MainViewModel
import by.arsy.wificonnector.model.Message

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val chatList by viewModel.chatList.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    val dynamicBottomPadding = WindowInsets.navigationBars
        .union(WindowInsets.ime)
        .asPaddingValues()

    BackHandler {
        viewModel.destroyEndpoint()
        onNavigate()
    }

    LaunchedEffect(chatList.size) {
        if (chatList.isNotEmpty()) {
            listState.animateScrollToItem(chatList.lastIndex)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(bottom = dynamicBottomPadding.calculateBottomPadding())
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.Bottom,
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(chatList) {
                MessageItem(
                    message = it,
                    owner = viewModel.isOwnerId(it.id),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        TextField(
            value = text,
            onValueChange = { text = it },
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "",
                    modifier = Modifier.clickable(
                        onClick = {
                            if (!text.isBlank()) {
                                viewModel.sendMessage(text)
                                text = ""
                            }
                        }
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MessageItem(
    message: Message,
    owner: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = if (owner) Alignment.CenterStart else Alignment.CenterEnd,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = if (owner) Alignment.Start else Alignment.End,
            modifier = Modifier
                .padding(4.dp)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(4.dp)
        ) {
            Text(
                text = message.username,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message.text,
                fontSize = 10.sp,
                lineHeight = 10.sp
            )
        }
    }
}