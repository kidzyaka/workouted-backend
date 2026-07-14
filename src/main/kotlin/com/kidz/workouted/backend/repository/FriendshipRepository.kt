package com.kidz.workouted.backend.repository

import com.kidz.workouted.backend.model.Friendship
import com.kidz.workouted.backend.model.FriendshipStatus
import com.kidz.workouted.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface FriendshipRepository : JpaRepository<Friendship, Long> {
    fun findByRequesterAndAddressee(requester: User, addressee: User): Friendship?
    fun findByAddresseeAndStatus(addressee: User, status: FriendshipStatus): List<Friendship>
    fun findByRequesterOrAddressee(requester: User, addressee: User): List<Friendship>
    
    // Find all accepted friendships for a user
    fun findByRequesterAndStatusOrAddresseeAndStatus(
        requester: User, status1: FriendshipStatus, 
        addressee: User, status2: FriendshipStatus
    ): List<Friendship>
}
