package top.fifthlight.armorstand.util

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

fun ResultSet.skipToInitialRow() = require(next()) { "No initial row" }

inline fun <T> Connection.transaction(crossinline block: Connection.() -> T): T {
    val originalAutoCommit = autoCommit
    try {
        autoCommit = false
        val result = block()
        try {
            commit()
        } catch (ex: Throwable) {
            // will be caught by outer try block
            throw ex
        }
        return result
    } catch (ex: Throwable) {
        try {
            rollback()
        } catch (rollbackEx: Throwable) {
            ex.addSuppressed(rollbackEx)
        }
        throw ex
    } finally {
        autoCommit = originalAutoCommit
    }
}

fun Connection.execute(statement: String) = createStatement().use { it.execute(statement) }

fun Connection.query(statement: String): ResultSet = createStatement().executeQuery(statement)

inline fun Connection.prepare(statement: String, crossinline block: PreparedStatement.() -> Unit) =
    prepareStatement(statement).use {
        block(it)
        it.execute()
    }

inline fun Connection.prepareQuery(statement: String, crossinline block: PreparedStatement.() -> Unit): ResultSet =
    prepareStatement(statement).let {
        block(it)
        it.executeQuery()
    }
