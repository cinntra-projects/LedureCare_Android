package com.cinntra.ledurecare.feature.auth.data

import com.cinntra.ledurecare.feature.auth.data.local.UserEntity
import com.cinntra.ledurecare.feature.auth.data.remote.LoginDataDto
import com.cinntra.ledurecare.feature.auth.domain.User

fun LoginDataDto.toEntity() = UserEntity(
    id = employee.id.toString(),
    name = employee.name,
    email = employee.email,
    phone = employee.phone.orEmpty(),
    roleName = employee.role?.name.orEmpty(),
    roleCode = employee.role?.code.orEmpty(),
    mustChangePassword = mustChangePassword,
)

fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    email = email,
    phone = phone,
    roleName = roleName,
    roleCode = roleCode,
    mustChangePassword = mustChangePassword,
)
