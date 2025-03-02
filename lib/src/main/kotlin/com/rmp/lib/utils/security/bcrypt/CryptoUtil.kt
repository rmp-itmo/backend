package com.rmp.lib.utils.security.bcrypt

import org.mindrot.jbcrypt.BCrypt

object CryptoUtil {
    fun hash(password: String) = BCrypt.hashpw(password, BCrypt.gensalt())

    fun compare(password: String, hash: String) = BCrypt.checkpw(password, hash)
}