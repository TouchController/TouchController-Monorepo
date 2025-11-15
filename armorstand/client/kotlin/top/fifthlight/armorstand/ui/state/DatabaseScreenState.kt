package top.fifthlight.armorstand.ui.state

import kotlin.time.Duration

data class DatabaseScreenState(
    val query: String = "",
    val state: QueryState = QueryState.Empty,
) {
    sealed class QueryState {
        data object Empty : QueryState()
        data object Loading : QueryState()
        data class Failed(val error: String?) : QueryState()
        data class Updated(
            val duration: Duration,
            val updateCount: Int
        ) : QueryState()
        data class Result(
            val duration: Duration,
            val headers: List<String> = emptyList(),
            val rows: List<List<String>> = emptyList(),
        ) : QueryState()
    }
}
