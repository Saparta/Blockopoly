package com.roomservice.util

import java.util.UUID

fun UUID.format(): String {
    return this.toString().replace("-","")
}