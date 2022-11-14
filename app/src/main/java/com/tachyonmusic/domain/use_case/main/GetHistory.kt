package com.tachyonmusic.domain.use_case.main

import com.tachyonmusic.user.domain.UserRepository

class GetHistory(
    private val userRepository: UserRepository
) {
    operator fun invoke() = userRepository.history
}