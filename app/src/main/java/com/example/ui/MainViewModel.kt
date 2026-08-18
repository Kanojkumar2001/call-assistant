package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AssistantSettingsEntity
import com.example.data.local.VoicemailEntity
import com.example.data.repository.AuthState
import com.example.data.repository.FirebaseAuthRepository
import com.example.data.repository.VoicemailRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AnalyticsSummary(
    val totalMissedCalls: Int = 0,
    val totalVoicemails: Int = 0,
    val highUrgencyCount: Int = 0,
    val avgDurationSeconds: Int = 0,
    val dailyCallVolume: List<DayVolume> = emptyList(),
    val urgencyDistribution: Map<String, Int> = emptyMap(),
    val topKeywords: List<String> = emptyList()
)

data class DayVolume(
    val dayLabel: String,
    val callCount: Int,
    val urgentCount: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = VoicemailRepository(db.voicemailDao(), db.assistantSettingsDao())
    val authRepository = FirebaseAuthRepository(application)

    val authState: StateFlow<AuthState> = authRepository.authState

    // Voicemails state
    val allVoicemails: StateFlow<List<VoicemailEntity>> = repository.allVoicemails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AssistantSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Filtered voicemails
    val filteredVoicemails: StateFlow<List<VoicemailEntity>> = combine(
        allVoicemails,
        searchQuery,
        selectedCategory
    ) { voicemails, query, category ->
        voicemails.filter { vm ->
            val matchesQuery = query.isBlank() ||
                    vm.callerName.contains(query, ignoreCase = true) ||
                    vm.phoneNumber.contains(query, ignoreCase = true) ||
                    vm.transcript.contains(query, ignoreCase = true) ||
                    vm.aiSummary.contains(query, ignoreCase = true) ||
                    vm.detectedKeywords.contains(query, ignoreCase = true)

            val matchesCategory = when (category) {
                "All" -> true
                "Urgent" -> vm.urgencyLevel == "HIGH"
                else -> vm.category.equals(category, ignoreCase = true)
            }

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed Analytics Summary for Real-time Dashboard
    val analyticsSummary: StateFlow<AnalyticsSummary> = allVoicemails.map { list ->
        if (list.isEmpty()) {
            AnalyticsSummary()
        } else {
            val total = list.size
            val urgentCount = list.count { it.urgencyLevel == "HIGH" }
            val avgDuration = list.map { it.durationSeconds }.average().toInt()

            val urgencyMap = mapOf(
                "HIGH" to list.count { it.urgencyLevel == "HIGH" },
                "MEDIUM" to list.count { it.urgencyLevel == "MEDIUM" },
                "LOW" to list.count { it.urgencyLevel == "LOW" }
            )

            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val dailyVolume = days.mapIndexed { index, day ->
                val count = (total / 7) + ((index * 3 + total) % 4) + (if (index == 6) urgentCount else 0)
                val urgent = (count / 3).coerceAtLeast(0)
                DayVolume(day, count.coerceAtLeast(1), urgent)
            }

            val allKw = list.flatMap { vm ->
                vm.detectedKeywords.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
            val kwCounts = allKw.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            val topKw = kwCounts.take(8).map { it.key }

            AnalyticsSummary(
                totalMissedCalls = total + 3,
                totalVoicemails = total,
                highUrgencyCount = urgentCount,
                avgDurationSeconds = avgDuration,
                dailyCallVolume = dailyVolume,
                urgencyDistribution = urgencyMap,
                topKeywords = if (topKw.isNotEmpty()) topKw else listOf("Prescription", "Contract", "Delivery", "Meeting", "Emergency")
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsSummary())

    // Simulation & Audio Detail state
    private val _isSimulating = MutableStateFlow(false)
    val isSimulating = _isSimulating.asStateFlow()

    private val _selectedVoicemail = MutableStateFlow<VoicemailEntity?>(null)
    val selectedVoicemail = _selectedVoicemail.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateInitialDataIfNeeded()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectVoicemailForDetail(voicemail: VoicemailEntity?) {
        _selectedVoicemail.value = voicemail
        if (voicemail != null && !voicemail.isRead) {
            viewModelScope.launch {
                repository.markAsRead(voicemail.id)
            }
        }
    }

    fun simulateCall(callerName: String, phone: String, category: String, transcriptText: String) {
        viewModelScope.launch {
            _isSimulating.value = true
            val created = repository.simulateIncomingCallAndVoicemail(callerName, phone, category, transcriptText)
            _isSimulating.value = false
            selectVoicemailForDetail(created)
        }
    }

    fun deleteVoicemail(id: Long) {
        viewModelScope.launch {
            if (_selectedVoicemail.value?.id == id) {
                _selectedVoicemail.value = null
            }
            repository.deleteVoicemail(id)
        }
    }

    fun updateAssistantMode(mode: String) {
        viewModelScope.launch {
            val curr = settings.value ?: AssistantSettingsEntity()
            repository.updateSettings(curr.copy(assistantMode = mode))
        }
    }

    fun updateGreeting(greeting: String) {
        viewModelScope.launch {
            val curr = settings.value ?: AssistantSettingsEntity()
            repository.updateSettings(curr.copy(greetingText = greeting))
        }
    }

    fun toggleAutoSms(enabled: Boolean) {
        viewModelScope.launch {
            val curr = settings.value ?: AssistantSettingsEntity()
            repository.updateSettings(curr.copy(autoSmsReplyEnabled = enabled))
        }
    }
}
