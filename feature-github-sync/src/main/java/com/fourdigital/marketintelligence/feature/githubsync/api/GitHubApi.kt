package com.fourdigital.marketintelligence.feature.githubsync.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * GitHub REST API v3 — requires Personal Access Token (PAT).
 * Base URL: https://api.github.com/
 * Rate limit: 5000 requests/hour (authenticated), 60/hour (unauthenticated).
 */
interface GitHubApi {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") auth: String
    ): GitHubUser

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") auth: String,
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<GitHubRepo>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepo

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 20
    ): List<GitHubIssue>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 20
    ): List<GitHubPullRequest>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20
    ): List<GitHubCommit>

    @GET("repos/{owner}/{repo}/events")
    suspend fun getRepoEvents(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30
    ): List<GitHubEvent>

    @GET("user/events")
    suspend fun getUserEvents(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 30
    ): List<GitHubEvent>

    @GET("search/code")
    suspend fun searchCode(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 10
    ): GitHubSearchResult
}

// --- DTOs ---

@Serializable
data class GitHubUser(
    val login: String = "",
    val id: Long = 0,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("public_repos") val publicRepos: Int = 0,
    @SerialName("public_gists") val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null
)

@Serializable
data class GitHubRepo(
    val id: Long = 0,
    val name: String = "",
    @SerialName("full_name") val fullName: String = "",
    val description: String? = null,
    val private: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    val language: String? = null,
    @SerialName("stargazers_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    @SerialName("open_issues_count") val openIssues: Int = 0,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("pushed_at") val pushedAt: String? = null,
    val size: Int = 0,
    val archived: Boolean = false,
    val fork: Boolean = false
)

@Serializable
data class GitHubIssue(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    val state: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val user: GitHubSimpleUser? = null,
    val labels: List<GitHubLabel> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("closed_at") val closedAt: String? = null,
    val comments: Int = 0,
    @SerialName("pull_request") val pullRequest: GitHubPrRef? = null
)

@Serializable
data class GitHubPullRequest(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    val state: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val user: GitHubSimpleUser? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("merged_at") val mergedAt: String? = null,
    val draft: Boolean = false
)

@Serializable
data class GitHubCommit(
    val sha: String = "",
    val commit: GitHubCommitDetail? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val author: GitHubSimpleUser? = null
)

@Serializable
data class GitHubCommitDetail(
    val message: String = "",
    val author: GitHubCommitAuthor? = null
)

@Serializable
data class GitHubCommitAuthor(
    val name: String = "",
    val email: String = "",
    val date: String = ""
)

@Serializable
data class GitHubEvent(
    val id: String = "",
    val type: String = "",
    val repo: GitHubEventRepo? = null,
    @SerialName("created_at") val createdAt: String = "",
    val actor: GitHubSimpleUser? = null
)

@Serializable
data class GitHubEventRepo(
    val id: Long = 0,
    val name: String = "",
    val url: String = ""
)

@Serializable
data class GitHubSimpleUser(
    val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class GitHubLabel(
    val name: String = "",
    val color: String = ""
)

@Serializable
data class GitHubPrRef(
    val url: String? = null
)

@Serializable
data class GitHubSearchResult(
    @SerialName("total_count") val totalCount: Int = 0,
    val items: List<GitHubSearchItem> = emptyList()
)

@Serializable
data class GitHubSearchItem(
    val name: String = "",
    val path: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val repository: GitHubSearchRepo? = null
)

@Serializable
data class GitHubSearchRepo(
    @SerialName("full_name") val fullName: String = ""
)
