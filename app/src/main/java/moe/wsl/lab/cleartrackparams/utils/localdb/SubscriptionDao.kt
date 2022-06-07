package moe.wsl.lab.cleartrackparams.utils.localdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription")
    fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscription WHERE `id` IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<Subscription>

    @Query("SELECT * FROM subscription WHERE subscription_name LIKE :name LIMIT 1")
    fun findByName(name: String): Subscription

    @Query("SELECT COUNT(id) FROM subscription")
    fun getCount(): Int

    @Insert
    fun insertAll(vararg users: Subscription)

    @Delete
    fun delete(user: Subscription)

}