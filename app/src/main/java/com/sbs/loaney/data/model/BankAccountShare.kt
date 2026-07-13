package com.sbs.loaney.data.model

import com.sbs.loaney.data.local.entity.BankAccountEntity

/** Permission level for a shared bank account or card. */
enum class SharePermission {
    /** Recipient can view account details only. */
    VIEW,
    /** Recipient can view and select the account when recording payments. */
    USE;

    companion object {
        fun fromRaw(value: String?): SharePermission =
            entries.find { it.name == value } ?: VIEW
    }
}

/** Lifecycle status of a share grant. */
enum class ShareStatus {
    PENDING,
    ACTIVE,
    REVOKED;

    companion object {
        fun fromRaw(value: String?): ShareStatus =
            entries.find { it.name == value } ?: PENDING
    }
}

/**
 * A share grant stored in Firestore under:
 *   users/{ownerUid}/bankAccountShares/{shareId}
 *   users/{recipientUid}/sharedBankAccounts/{shareId}
 */
data class BankAccountShare(
    val shareId: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    /** Local Room ID on the owner's device. */
    val accountLocalId: Long = 0L,
    val sharedWithUid: String = "",
    val sharedWithEmail: String = "",
    val sharedWithName: String = "",
    val permission: String = SharePermission.VIEW.name,
    val status: String = ShareStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val accountName: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val branchName: String? = null,
    val swiftCode: String? = null,
    val coverImageUri: String? = null,
    val isCard: Boolean = false,
    val isMfs: Boolean = false,
    val mfsProvider: String? = null,
    val qrCodeUri: String? = null
) {
    val permissionEnum: SharePermission get() = SharePermission.fromRaw(permission)
    val statusEnum: ShareStatus get() = ShareStatus.fromRaw(status)

    fun toEntity(forIncoming: Boolean = true): BankAccountEntity = BankAccountEntity(
        accountName = accountName,
        accountNumber = accountNumber,
        bankName = bankName,
        branchName = branchName,
        swiftCode = swiftCode,
        coverImageUri = coverImageUri,
        isCard = isCard,
        isMfs = isMfs,
        mfsProvider = mfsProvider,
        qrCodeUri = qrCodeUri,
        ownerUid = if (forIncoming) ownerUid else null,
        ownerName = if (forIncoming) ownerName else null,
        shareId = shareId,
        sharePermission = permission,
        isSharedIncoming = forIncoming
    )

    companion object {
        fun fromAccount(
            account: BankAccountEntity,
            shareId: String,
            ownerUid: String,
            ownerName: String,
            recipientUid: String,
            recipientEmail: String,
            recipientName: String,
            permission: SharePermission
        ): BankAccountShare = BankAccountShare(
            shareId = shareId,
            ownerUid = ownerUid,
            ownerName = ownerName,
            accountLocalId = account.id,
            sharedWithUid = recipientUid,
            sharedWithEmail = recipientEmail.lowercase(),
            sharedWithName = recipientName,
            permission = permission.name,
            status = ShareStatus.PENDING.name,
            createdAt = System.currentTimeMillis(),
            accountName = account.accountName,
            accountNumber = account.accountNumber,
            bankName = account.bankName,
            branchName = account.branchName,
            swiftCode = account.swiftCode,
            coverImageUri = account.coverImageUri,
            isCard = account.isCard,
            isMfs = account.isMfs,
            mfsProvider = account.mfsProvider,
            qrCodeUri = account.qrCodeUri
        )
    }
}
