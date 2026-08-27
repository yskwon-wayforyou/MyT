package com.myt.phase3

/** M43 — Data import/export (Phase 3). */
interface DataPortability {
    suspend fun exportTripsCsv(): String
    suspend fun exportChargesCsv(): String
    suspend fun importFromTessieCsv(csvContent: String, defaultVin: String): Result<Int>
}
