package com.plop.plopmessenger.presentation.viewmodel

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plop.plopmessenger.domain.model.*
import com.plop.plopmessenger.domain.repository.SocketRepository
import com.plop.plopmessenger.domain.repository.UserRepository
import com.plop.plopmessenger.domain.usecase.chatroom.ChatRoomUseCase
import com.plop.plopmessenger.domain.usecase.message.MessageUseCase
import com.plop.plopmessenger.domain.usecase.socket.sendMessageUseCase
import com.plop.plopmessenger.domain.usecase.socket.subscribeUseCase
import com.plop.plopmessenger.domain.util.Resource
import com.plop.plopmessenger.presentation.model.MediaStoreImage
import com.plop.plopmessenger.presentation.navigation.DestinationID
import com.plop.plopmessenger.presentation.screen.main.InputSelector
import com.plop.plopmessenger.util.getChatRoomTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageUseCase: MessageUseCase,
    private val userRepository: UserRepository,
    private val chatRoomUseCase: ChatRoomUseCase,
    private val sendMessageUseCase: sendMessageUseCase,
    private val subscribeUseCase: subscribeUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    var chatState = MutableStateFlow(ChatState(chatroomId = savedStateHandle.get<String>(
        DestinationID.CHAT_ID)?: null))
        private set

    init {
        if(!chatState.value.chatroomId.isNullOrBlank()){
            viewModelScope.launch {
                launch { getChatroomInfo() }.join()
                launch { getChatRoomNewMessage() }.join()
                getFirstMessage()
                getMessageList()
            }
        }
        getUserId()
        loadImage()
    }

    suspend fun getDmChatRoomNewId(people: People) {
        val result = chatRoomUseCase.createDmChatRoomUseCase(people)
        when(result) {
            is Resource.Success -> {
                chatState.update { it.copy(chatroomId = result.data) }
                subscribeUseCase(result.data!!)
                getMessageList()
                getFirstMessage()
                getChatroomInfo()
            } else -> {
            Log.d("GetDmChatRoomNewId", result.message.toString())
            }
        }
    }

    suspend fun getGroupChatRoomNewId(people: List<People>) {
        chatRoomUseCase.createGroupChatRoomUseCase(people).collect() { result ->
            when(result) {
                is Resource.Success -> {
                    chatState.update { it.copy(chatroomId = result.data) }
                    subscribeUseCase(result.data!!)
                    getMessageList()
                    getFirstMessage()
                    getChatroomInfo()
                } else -> {
                Log.d("GetGroupChatRoomNewId", result.message.toString())
                }
            }
        }
    }

     private suspend fun getChatRoomNewMessage() {
         when(chatRoomUseCase.getNewChatRoomMessageUseCase(
             chatState.value.chatroomId!!,
             chatState.value.members["1234"]?.readMessage?: "")) {
             is Resource.Success -> {
                 Log.d("GetNewChatRoomNewId", "성공...성공이오..")
             }
             else -> {
                 Log.d("GetNewChatRoomNewId", "실패..실패요..")
             }
         }
    }

    fun getMessageList() {
        viewModelScope.launch {
            messageUseCase.getLocalMessageListUseCase(chatState.value.chatroomId!!, chatState.value.page)
                .collect() { result ->
                when (result) {
                    is Resource.Success -> {
                        if(result.data.isNullOrEmpty()){
                            chatState.update {
                                it.copy(
                                    isLoading = false,
                                    endReached = true
                                )
                            }
                        } else {
                            chatState.update {
                                it.copy(
                                    isLoading = false,
                                    messages = it.messages + result.data,
                                    page = it.page + 1
                                )
                            }
                        }

                    }
                    is Resource.Loading -> {
                        chatState.update {
                            it.copy(isLoading = true)
                        }
                    }
                    is Resource.Error -> {
                        chatState.update {
                            it.copy(isLoading = false)
                        }
                    }
                }

            }
        }
    }

    private fun getFirstMessage() {
        viewModelScope.launch {
            messageUseCase.getFirstMessageUseCase(chatState.value.chatroomId!!).collect() { result ->
                when (result) {
                    is Resource.Success -> {
                        if(result.data != null && !chatState.value.messages.contains(result.data) ) {
                            chatState.update {
                                it.copy(
                                    messages = listOf(result.data) + it.messages
                                )
                            }
                        }else{
                            chatState.update {
                                it.copy(
                                    isLoading = false
                                )
                            }
                        }
                    }
                    is Resource.Loading -> {
                        chatState.update {
                            it.copy(isLoading = true)
                        }
                    }
                    is Resource.Error -> {
                        chatState.update {
                            it.copy(isLoading = false)
                        }
                    }
                }
            }
        }
    }

    private suspend fun getChatroomInfo() {
        val result = chatRoomUseCase.getChatRoomInfoUseCase(chatState.value.chatroomId!!)
        when (result) {
            is Resource.Success -> {
                Log.d("가희", "getChatRoomInfo 실행 ${result.data?.members.toString()}")
                chatState.update {
                    it.copy(
                        chatRoomType = result.data?.type ?: ChatRoomType.DM,
                        isLoading = false,
                        members = result.data?.members?.map { it.memberId to it }?.toMap() ?: mapOf(),
                        title = result.data?.title ?: "n"
                    )
                }
            }
            is Resource.Loading -> {
                chatState.update {
                    it.copy(isLoading = true)
                }
            }
            is Resource.Error -> {
                chatState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun setQuery(query: TextFieldValue) {
        chatState.update {
            it.copy(query = query)
        }
    }

    fun setFocusState(isFocus: Boolean) {
        chatState.update {
            it.copy(textFieldFocusState = isFocus)
        }
    }

    fun setInputSelector(inputSelector: InputSelector) {
        chatState.update {
            it.copy(currentInputSelector = inputSelector)
        }
    }

    private fun getUserId() {
        viewModelScope.launch {
            userRepository.getUserId().collect() { userId ->
                chatState.update {
                    it.copy(userId = userId)
                }
            }
        }
    }

    fun getMember(people: List<People>) {
        if(chatState.value.chatroomId.isNullOrBlank()) {
            if(people.size == 1) {
                viewModelScope.launch {
                    val result = chatRoomUseCase.getChatRoomIdByPeopleIdUseCase(people.first().peopleId)
                    if(result.data.isNullOrBlank()) {
                        chatState.update { it.copy(
                            title = people.first().nickname,
                            members = mapOf(people.first().peopleId to people.first().toMember()),
                            chatRoomType = ChatRoomType.DM
                        )
                        }
                        getDmChatRoomNewId(people.first())

                    }else {
                        chatState.update { it.copy(chatroomId = result.data) }
                        launch { getChatRoomNewMessage() }.join()
                        getMessageList()
                        getChatroomInfo()
                        getFirstMessage()
                    }
                }
            } else {
                viewModelScope.launch{
                    getGroupChatRoomNewId(people)
                    chatState.update {
                        it.copy(
                            title = getChatRoomTitle(people.map { it.nickname }),
                            members = people.map { it.peopleId to it.toMember() }.toMap(),
                            chatRoomType = ChatRoomType.GROUP
                        )
                    }
                }
            }
        }else {
            chatState.update {
                var result = it.members.toMutableMap()
                people.forEach { people -> result[people.peopleId] = people.toMember() }
                it.copy(
                    members = result,
                    chatRoomType = ChatRoomType.GROUP
                )
            }
        }
    }

    fun setImage(uri: Uri?) {
        chatState.update {
            it.copy(selectedImage = if(chatState.value.selectedImage == uri) null else uri)
        }
    }

    fun loadImage() {
        viewModelScope.launch {
            chatState.update {
                it.copy(images = queryImages())
            }
        }
    }

    private suspend fun queryImages(): List<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

            val selectionArgs = arrayOf(
                // Release day of the G1. :)
                dateToTimestamp(day = 22, month = 10, year = 1).toString()
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {

                    // Here we'll use the column indexs that we found above.
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val image = MediaStoreImage(id, displayName, dateModified, contentUri)
                    images += image
                }
            }
        }
        return images
    }

    fun setCurrentInputSelector(inputSelector: InputSelector) {
        chatState.update {
            it.copy(
                currentInputSelector = if(inputSelector == it.currentInputSelector) InputSelector.NONE  else inputSelector
            )
        }
    }

    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }

    fun sendMessage() {
        if(chatState.value.query != TextFieldValue("")) {
            viewModelScope.launch {
                sendMessageUseCase(
                    roomId =  chatState.value.chatroomId!!,
                    content = chatState.value.query.text,
                    userId = chatState.value.userId
                )
            }
            chatState.update { it.copy(query = TextFieldValue("")) }
        }
    }

    fun sendImage() {

    }
}

data class ChatState(
    val members: Map<String, Member> = mapOf(),
    val messages: List<Message> = emptyList(),
    var chatroomId: String? = null,
    val title: String = "",
    var query: TextFieldValue = TextFieldValue(""),
    val textFieldFocusState: Boolean = false,
    val currentInputSelector: InputSelector = InputSelector.NONE,
    val chatRoomType: ChatRoomType = ChatRoomType.DM,
    val userId: String = "",
    val isLoading: Boolean = false,
    val images: List<MediaStoreImage> = emptyList(),
    val selectedImage: Uri? = null,
    val page: Int = 0,
    val error: String? = null,
    val endReached: Boolean = false
)
