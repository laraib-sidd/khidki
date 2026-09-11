package dev.laraib.khidki.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object KhidkiMigrations {
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE sessions
                    ADD COLUMN origin TEXT NOT NULL DEFAULT 'REQUEST'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE sessions
                    ADD COLUMN forwardCount INTEGER NOT NULL DEFAULT 0
                    """.trimIndent(),
                )
            }
        }
}
