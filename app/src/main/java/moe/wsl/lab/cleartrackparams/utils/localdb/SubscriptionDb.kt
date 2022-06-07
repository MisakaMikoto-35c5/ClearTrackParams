package moe.wsl.lab.cleartrackparams.utils.localdb

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Subscription::class], version = 1)
abstract class SubscriptionDb : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
}
