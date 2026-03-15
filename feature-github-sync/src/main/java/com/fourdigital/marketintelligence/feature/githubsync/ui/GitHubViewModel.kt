package com.fourdigital.marketintelligence.feature.githubsync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.core.network.api.ModelInfo
import com.fourdigital.marketintelligence.data.provider.ApiKeyManager
import com.fourdigital.marketintelligence.data.provider.GitHubAIAnalyst
import com.fourdigital.marketintelligence.feature.githubsync.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class GitHubState(
    val token: String = "",
    val isConnected: Boolean = false,
    val connectionStatus: String = "Not connected",
    val user: GitHubUser? = null,
    val repos: List<GitHubRepo> = emptyList(),
    val selectedRepo: GitHubRepo? = null,
    val issues: List<GitHubIssue> = emptyList(),
    val pullRequests: List<GitHubPullRequest> = emptyList(),
    val commits: List<GitHubCommit> = emptyList(),
    val events: List<GitHubEvent> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<GitHubSearchItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTab: Int = 0,
    val error: String? = null,
    // AI Models
    val aiModelsAvailable: Boolean = false,
    val availableModels: List<ModelInfo> = emptyList(),
    val selectedModelId: String = "gpt-4o-mini",
    val knownModels: List<GitHubAIAnalyst.AIModelOption> = GitHubAIAnalyst.AVAILABLE_MODELS,
    val isLoadingModels: Boolean = false
)

@HiltViewModel
class GitHubViewModel @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val apiKeyManager: ApiKeyManager,
    private val gitHubAI: GitHubAIAnalyst
) : ViewModel() {

    private val _state = MutableStateFlow(GitHubState())
    val state: StateFlow<GitHubState> = _state.asStateFlow()

    private val authHeader: String
        get() = "token ${_state.value.token}"

    init {
        loadSavedToken()
    }

    private fun loadSavedToken() {
        viewModelScope.launch {
            try {
                val saved = apiKeyManager.getKey(GitHubAIAnalyst.GITHUB_KEY)
                if (!saved.isNullOrBlank()) {
                    _state.value = _state.value.copy(token = saved)
                    testConnection()
                } else {
                    _state.value = _state.value.copy(
                        connectionStatus = "No GitHub token — configure in Settings > API Configuration"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    connectionStatus = "Error loading token: ${e.message?.take(50)}"
                )
            }
        }
    }

    fun refreshConnection() {
        viewModelScope.launch {
            try {
                val saved = apiKeyManager.getKey(GitHubAIAnalyst.GITHUB_KEY)
                if (!saved.isNullOrBlank()) {
                    _state.value = _state.value.copy(token = saved)
                    testConnection()
                } else {
                    _state.value = _state.value.copy(
                        isConnected = false,
                        connectionStatus = "No GitHub token — configure in Settings > API Configuration"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isConnected = false,
                    connectionStatus = "Error: ${e.message?.take(50)}"
                )
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, connectionStatus = "Testing...", error = null)
            try {
                val user = gitHubApi.getAuthenticatedUser(authHeader)
                _state.value = _state.value.copy(
                    isConnected = true,
                    connectionStatus = "Connected as ${user.login}",
                    user = user,
                    isLoading = false
                )
                loadRepos()
                loadActivity()
                loadAIModels()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isConnected = false,
                    connectionStatus = "Error: ${e.message?.take(80) ?: "Connection failed"}",
                    user = null,
                    isLoading = false
                )
            }
        }
    }

    fun loadRepos() {
        if (!_state.value.isConnected) return
        viewModelScope.launch {
            try {
                val repos = gitHubApi.getUserRepos(authHeader)
                _state.value = _state.value.copy(repos = repos, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to load repos: ${e.message?.take(60)}")
            }
        }
    }

    fun selectRepo(repo: GitHubRepo) {
        _state.value = _state.value.copy(selectedRepo = repo, selectedTab = 0)
        loadRepoDetails(repo)
    }

    fun clearSelectedRepo() {
        _state.value = _state.value.copy(
            selectedRepo = null, issues = emptyList(),
            pullRequests = emptyList(), commits = emptyList()
        )
    }

    private fun loadRepoDetails(repo: GitHubRepo) {
        val parts = repo.fullName.split("/")
        if (parts.size != 2) return
        val (owner, name) = parts
        viewModelScope.launch {
            try {
                val issues = gitHubApi.getIssues(authHeader, owner, name)
                val prs = gitHubApi.getPullRequests(authHeader, owner, name)
                val commits = gitHubApi.getCommits(authHeader, owner, name)
                _state.value = _state.value.copy(
                    issues = issues, pullRequests = prs, commits = commits, error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to load repo details: ${e.message?.take(60)}")
            }
        }
    }

    fun loadActivity() {
        if (!_state.value.isConnected) return
        viewModelScope.launch {
            try {
                val events = gitHubApi.getUserEvents(authHeader)
                _state.value = _state.value.copy(events = events, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to load activity: ${e.message?.take(60)}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun searchCode() {
        val q = _state.value.searchQuery.trim()
        if (q.isBlank() || !_state.value.isConnected) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = gitHubApi.searchCode(authHeader, q)
                _state.value = _state.value.copy(
                    searchResults = result.items, isLoading = false, error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Search failed: ${e.message?.take(60)}"
                )
            }
        }
    }

    fun setTab(index: Int) {
        _state.value = _state.value.copy(selectedTab = index)
    }

    // --- AI Models ---

    fun loadAIModels() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingModels = true)
            try {
                val models = gitHubAI.listAvailableModels()
                val available = gitHubAI.isAvailable()
                _state.value = _state.value.copy(
                    aiModelsAvailable = available,
                    availableModels = models,
                    selectedModelId = gitHubAI.getSelectedModel(),
                    isLoadingModels = false
                )
                Timber.d("Loaded ${models.size} AI models from GitHub")
            } catch (e: Exception) {
                Timber.w(e, "Failed to load AI models")
                _state.value = _state.value.copy(
                    aiModelsAvailable = gitHubAI.isAvailable(),
                    isLoadingModels = false
                )
            }
        }
    }

    fun selectAIModel(modelId: String) {
        gitHubAI.setSelectedModel(modelId)
        _state.value = _state.value.copy(selectedModelId = modelId)
    }
}
