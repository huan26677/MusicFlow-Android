package com.example.musicflow.core.media

/**
 * Stable source classification for future provider adapters.
 *
 * IMPORTANT: Source classification is NOT a migration. mediaId is the key
 * already persisted in playlists, history and queue snapshots; do not rewrite
 * it merely to introduce this contract.
 */
enum class MediaSourceKind {
    MEDIA_STORE, SAF_DOCUMENT, LICENSED_ONLINE, STORED_LEGACY
}

data class MediaSourceRef(
    val kind: MediaSourceKind,
    val mediaId: String
)

object MediaSourceContract {
    /** Retains both the legacy "device-id" fallback and explicit IDs. */
    fun device(id: Long, explicitMediaId: String?): MediaSourceRef =
        MediaSourceRef(
            kind = if (explicitMediaId?.startsWith("content://") == true ||
                explicitMediaId?.startsWith("file://") == true) {
                MediaSourceKind.SAF_DOCUMENT
            } else {
                MediaSourceKind.MEDIA_STORE
            },
            mediaId = explicitMediaId ?: "device-$id"
        )

    /** SAF picker IDs have always been the URI string itself. */
    fun picked(documentUri: String): MediaSourceRef {
        require(documentUri.startsWith("content://") ||
            documentUri.startsWith("file://")) {
            "Only document/file URIs are allowed for picked audio"
        }
        return MediaSourceRef(MediaSourceKind.SAF_DOCUMENT, documentUri)
    }

    /**
     * Keep online:id to avoid rekeying old data. A future multi-catalog
     * namespace needs an explicit migration rather than silently changing it.
     */
    fun online(trackId: String): MediaSourceRef {
        require(trackId.isNotBlank()) { "Online ID must not be empty" }
        return MediaSourceRef(MediaSourceKind.LICENSED_ONLINE, "online:$trackId")
    }

    /** Restored playlist/history/queue IDs must be byte-for-byte unchanged. */
    fun stored(mediaId: String): MediaSourceRef {
        val kind = when {
            mediaId.startsWith("online:") -> MediaSourceKind.LICENSED_ONLINE
            mediaId.startsWith("content://") ||
                mediaId.startsWith("file://") -> MediaSourceKind.SAF_DOCUMENT
            mediaId.startsWith("device-") &&
                mediaId.removePrefix("device-").toLongOrNull() != null ->
                MediaSourceKind.MEDIA_STORE
            else -> MediaSourceKind.STORED_LEGACY
        }
        return MediaSourceRef(kind, mediaId)
    }
}
