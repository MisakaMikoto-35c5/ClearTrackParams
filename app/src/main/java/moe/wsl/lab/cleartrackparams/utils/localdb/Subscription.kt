package moe.wsl.lab.cleartrackparams.utils.localdb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "subscription_name", index = true) val subscriptionName: String,
    @ColumnInfo(name = "subscription_url", index = true) val subscriptionURL: String,
    @ColumnInfo(name = "last_update_at") val lastUpdateAt: Long,
    @ColumnInfo(name = "content") val content: String?,
)
