package com.myt.debug

import kotlinx.serialization.Serializable

@Serializable
data class PendingGitHubIssue(
    val id: String,
    val createdAtMs: Long,
    val kind: String, // crash | error
    val title: String,
    val body: String,
    val labels: List<String> = listOf("auto-reported"),
    val uploaded: Boolean = false,
    val githubIssueUrl: String? = null,
    val githubIssueNumber: Int? = null,
    val uploadError: String? = null,
)

interface PendingIssueStore {
    fun enqueue(issue: PendingGitHubIssue)
    fun listPending(): List<PendingGitHubIssue>
    fun markUploaded(id: String, url: String, number: Int?)
    fun markUploadFailed(id: String, error: String)
    fun delete(id: String)
}

object NoOpPendingIssueStore : PendingIssueStore {
    override fun enqueue(issue: PendingGitHubIssue) = Unit
    override fun listPending(): List<PendingGitHubIssue> = emptyList()
    override fun markUploaded(id: String, url: String, number: Int?) = Unit
    override fun markUploadFailed(id: String, error: String) = Unit
    override fun delete(id: String) = Unit
}
