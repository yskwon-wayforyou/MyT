package com.myt.data.fleet

/** Vehicle asleep / empty Fleet payload — wake or use cache; do not file as crash. */
class VehicleDataUnavailableException(message: String) : IllegalStateException(message)
