package dev.laraib.khidki.data.repository

import dev.laraib.khidki.data.db.dao.DuplicateFingerprintDao
import dev.laraib.khidki.data.db.entities.DuplicateFingerprintEntity
import java.security.MessageDigest
import java.util.UUID

class RoomDuplicateFingerprintStore(
    private val duplicateFingerprintDao: DuplicateFingerprintDao,
) {
    suspend fun isDuplicate(
        sender: String,
        body: String,
        pduTimestampMillis: Long,
        subscriptionId: Int,
    ): Boolean {
        val digestHex = computeDigest(sender, body, pduTimestampMillis, subscriptionId)
        return duplicateFingerprintDao.exists(digestHex)
    }

    suspend fun record(
        sender: String,
        body: String,
        pduTimestampMillis: Long,
        subscriptionId: Int,
        sessionId: UUID,
        recordedAtMillis: Long,
    ): Boolean {
        val digestHex = computeDigest(sender, body, pduTimestampMillis, subscriptionId)
        val rowId = duplicateFingerprintDao.insert(
            DuplicateFingerprintEntity(
                digestHex = digestHex,
                sessionId = sessionId.toString(),
                recordedAtMillis = recordedAtMillis,
            ),
        )
        return rowId != -1L
    }

    suspend fun pruneOlderThan(cutoffMillis: Long) {
        duplicateFingerprintDao.deleteOlderThan(cutoffMillis)
    }

    private fun computeDigest(
        sender: String,
        body: String,
        pduTimestampMillis: Long,
        subscriptionId: Int,
    ): String {
        val material = "$sender\u0000$body\u0000$pduTimestampMillis\u0000$subscriptionId"
        val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
