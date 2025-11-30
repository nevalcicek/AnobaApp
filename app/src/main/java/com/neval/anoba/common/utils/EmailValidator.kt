package com.neval.anoba.common.utils

interface EmailValidator {
    fun isValid(email: String): Boolean
}