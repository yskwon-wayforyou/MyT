package com.myt.phase3

/** M43 — Data import/export (Phase 3 stub). */
interface DataPortability {
    suspend fun exportTripsCsv(): String
    suspend fun importFromTessie(filePath: String): Result<Int>
}

class StubDataPortability : DataPortability {
    override suspend fun exportTripsCsv(): String = "id,start,end,distance_km\n"
    override suspend fun importFromTessie(filePath: String): Result<Int> =
        Result.failure(UnsupportedOperationException("Phase 3"))
}
