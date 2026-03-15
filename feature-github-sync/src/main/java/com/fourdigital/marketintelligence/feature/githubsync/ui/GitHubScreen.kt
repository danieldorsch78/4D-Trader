package com.fourdigital.marketintelligence.feature.githubsync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdigital.marketintelligence.feature.githubsync.api.*

private val TerminalGreen = Color(0xFF00E676)
private val TerminalBlack = Color(0xFF0D0D0D)
private val CardGray = Color(0xFF1A1A2E)
private val MutedText = Color(0xFF888888)
private val AccentBlue = Color(0xFF448AFF)
private val ErrorRed = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubScreen(onBack: () -> Unit = {}, viewModel: GitHubViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Intelligence", fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBlack,
                    titleContentColor = TerminalGreen,
                    navigationIconContentColor = AccentBlue
                )
            )
        },
        containerColor = TerminalBlack
    ) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Connection card
        item { ConnectionCard(state, viewModel) }

        // Error display
        if (state.error != null) {
            item {
                Text(
                    text = "⚠ ${state.error}",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Connected content
        if (state.isConnected) {
            // User info
            state.user?.let { user ->
                item { UserInfoCard(user) }
            }

            // AI Models status
            item { AIModelsStatusCard(state) }

            // Repo detail or Repo list
            val selectedRepo = state.selectedRepo
            if (selectedRepo != null) {
                item { RepoDetailHeader(selectedRepo, viewModel) }
                item { RepoTabs(state, viewModel) }

                when (state.selectedTab) {
                    0 -> {
                        if (state.issues.isEmpty()) {
                            item { EmptyState("No open issues") }
                        } else {
                            items(state.issues, key = { it.id }) { issue ->
                                IssueRow(issue)
                            }
                        }
                    }
                    1 -> {
                        if (state.pullRequests.isEmpty()) {
                            item { EmptyState("No open pull requests") }
                        } else {
                            items(state.pullRequests, key = { it.id }) { pr ->
                                PrRow(pr)
                            }
                        }
                    }
                    2 -> {
                        if (state.commits.isEmpty()) {
                            item { EmptyState("No commits") }
                        } else {
                            items(state.commits, key = { it.sha }) { commit ->
                                CommitRow(commit)
                            }
                        }
                    }
                }
            } else {
                // Repos list
                item {
                    Text(
                        text = "> REPOSITORIES (${state.repos.size})",
                        color = TerminalGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(state.repos, key = { it.id }) { repo ->
                    RepoRow(repo) { viewModel.selectRepo(repo) }
                }

                // Activity feed
                if (state.events.isNotEmpty()) {
                    item {
                        Text(
                            text = "> RECENT ACTIVITY",
                            color = TerminalGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    items(state.events.take(15), key = { it.id }) { event ->
                        EventRow(event)
                    }
                }

                // Code search
                item { CodeSearchSection(state, viewModel) }

                if (state.searchResults.isNotEmpty()) {
                    items(state.searchResults, key = { it.htmlUrl }) { item ->
                        SearchResultRow(item)
                    }
                }
            }
        }
    }
    } // Scaffold
}

@Composable
private fun ConnectionCard(state: GitHubState, viewModel: GitHubViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (state.isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (state.isConnected) TerminalGreen else ErrorRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = state.connectionStatus,
                        color = if (state.isConnected) TerminalGreen else MutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!state.isConnected) {
                        Text(
                            text = "Configure token in Settings > API Configuration",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            Button(
                onClick = { viewModel.refreshConnection() },
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Refresh", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun UserInfoCard(user: GitHubUser) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "> USER: ${user.login}",
                color = TerminalGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            user.name?.let {
                Text(text = it, color = Color.White, fontSize = 14.sp)
            }
            user.bio?.let {
                Text(text = it, color = MutedText, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip("Repos", user.publicRepos)
                StatChip("Followers", user.followers)
                StatChip("Following", user.following)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$count", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = MutedText, fontSize = 11.sp)
    }
}

@Composable
private fun AIModelsStatusCard(state: GitHubState) {
    val Purple = Color(0xFFAB47BC)
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = if (state.aiModelsAvailable) Purple else MutedText,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "> AI MODELS",
                    color = if (state.aiModelsAvailable) Purple else MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (state.aiModelsAvailable) TerminalGreen else ErrorRed))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (state.aiModelsAvailable)
                        "GPT-4o-mini: Connected via GitHub Models API"
                    else
                        "AI Models: Not available — verify GitHub token permissions",
                    color = if (state.aiModelsAvailable) Color.White else MutedText,
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace
                )
            }

            if (state.aiModelsAvailable) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Your GitHub PAT enables AI-powered market analysis via GPT-4o-mini. " +
                        "Access it from the AI Trading → AI Agent tab.",
                    color = MutedText, fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RepoRow(repo: GitHubRepo, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repo.name,
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (repo.private) {
                    Text(
                        text = "PRIVATE",
                        color = ErrorRed.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ErrorRed.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            repo.description?.let {
                Text(
                    text = it,
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repo.language?.let {
                    Text(text = "● $it", color = TerminalGreen, fontSize = 11.sp)
                }
                Text(text = "★ ${repo.stars}", color = MutedText, fontSize = 11.sp)
                Text(text = "⑂ ${repo.forks}", color = MutedText, fontSize = 11.sp)
                Text(text = "⚠ ${repo.openIssues}", color = MutedText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RepoDetailHeader(repo: GitHubRepo, viewModel: GitHubViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.clearSelectedRepo() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = repo.fullName,
                    color = AccentBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            repo.description?.let {
                Text(text = it, color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repo.language?.let { StatChip(it, 0) }
                StatChip("Stars", repo.stars)
                StatChip("Forks", repo.forks)
                StatChip("Issues", repo.openIssues)
            }
        }
    }
}

@Composable
private fun RepoTabs(state: GitHubState, viewModel: GitHubViewModel) {
    val tabs = listOf("Issues", "PRs", "Commits")
    TabRow(
        selectedTabIndex = state.selectedTab,
        containerColor = CardGray,
        contentColor = AccentBlue,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = state.selectedTab == index,
                onClick = { viewModel.setTab(index) },
                text = {
                    Text(
                        text = title,
                        color = if (state.selectedTab == index) AccentBlue else MutedText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            )
        }
    }
}

@Composable
private fun IssueRow(issue: GitHubIssue) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = if (issue.state == "open") TerminalGreen else ErrorRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "#${issue.number} ${issue.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                issue.user?.let {
                    Text(text = it.login, color = MutedText, fontSize = 11.sp)
                }
                Text(text = "💬 ${issue.comments}", color = MutedText, fontSize = 11.sp)
                issue.labels.take(3).forEach { label ->
                    Text(
                        text = label.name,
                        color = AccentBlue,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrRow(pr: GitHubPullRequest) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.MergeType,
                    contentDescription = null,
                    tint = when {
                        pr.mergedAt != null -> Color(0xFFAB47BC)
                        pr.state == "open" -> TerminalGreen
                        else -> ErrorRed
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "#${pr.number} ${pr.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (pr.draft) {
                    Text(
                        text = "DRAFT",
                        color = MutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MutedText.copy(alpha = 0.1f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            pr.user?.let {
                Text(
                    text = it.login,
                    color = MutedText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CommitRow(commit: GitHubCommit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = commit.sha.take(7),
                color = AccentBlue,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = commit.commit?.message?.lines()?.firstOrNull() ?: "",
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commit.commit?.author?.let {
                    Text(text = it.name, color = MutedText, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: GitHubEvent) {
    val eventLabel = when (event.type) {
        "PushEvent" -> "⬆ Push"
        "CreateEvent" -> "✨ Create"
        "DeleteEvent" -> "🗑 Delete"
        "PullRequestEvent" -> "↗ PR"
        "IssuesEvent" -> "🐛 Issue"
        "WatchEvent" -> "★ Star"
        "ForkEvent" -> "⑂ Fork"
        "IssueCommentEvent" -> "💬 Comment"
        else -> "● ${event.type.removeSuffix("Event")}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = eventLabel, color = TerminalGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(90.dp))
        Text(
            text = event.repo?.name ?: "",
            color = AccentBlue,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CodeSearchSection(state: GitHubState, viewModel: GitHubViewModel) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = "> CODE SEARCH",
            color = TerminalGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search code...", color = MutedText.copy(alpha = 0.5f)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchCode() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                    cursorColor = TerminalGreen
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.searchCode() }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentBlue)
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: GitHubSearchItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.path,
                color = AccentBlue,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.repository?.let {
                Text(
                    text = it.fullName,
                    color = MutedText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Text(
        text = "  $message",
        color = MutedText,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
