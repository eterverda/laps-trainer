package ru.fpvladder.laps.trainer.model

/**
 * Feature flags for gradually rolling out or hiding functionality.
 *
 * These are compile-time constants so the unreachable code is still present
 * in the source and can be re-enabled by flipping the flag.
 */
val TEAM_HOLESHOT_ENABLED = "false".toBoolean()
val WIGGLE_ONCE_ENABLED = "false".toBoolean()
