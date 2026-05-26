package com.example.desktopapplication.models

data class UserIngestDto(
    val name: String,
    val email: String,
    val password: String? = null,
    val isAdmin: Boolean = false
)

data class UserResponseDto(
    val _id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val isAdmin: Boolean? = null
)

object UserMapper {

    fun toIngest(user: User, password: String? = null): UserIngestDto = UserIngestDto(
        name = user.name,
        email = user.email,
        password = password?.takeIf { it.isNotBlank() },
        isAdmin = user.isAdmin
    )

    fun fromResponse(dto: UserResponseDto, localId: Int): User = User(
        id = localId,
        apiId = dto._id,
        name = dto.name ?: "",
        email = dto.email ?: "",
        isAdmin = dto.isAdmin == true
    )
}
