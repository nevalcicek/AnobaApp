package com.neval.anoba.common.utils

import android.util.Patterns

class EmailValidatorImpl : EmailValidator {
    override fun isValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}