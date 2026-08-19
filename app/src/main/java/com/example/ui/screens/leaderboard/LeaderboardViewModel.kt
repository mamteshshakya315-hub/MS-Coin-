package com.example.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {
    private val repository = UserRepository()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    
    init {
        loadLeaderboard()
    }
    
    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTopUsers(50).onSuccess {
                _users.value = it
            }.onFailure {
                // handle error
            }
            _isLoading.value = false
        }
    }
}
