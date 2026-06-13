package ru.fpvladder.laps.trainer.settings

/**
 * Feature flags for gradually rolling out or hiding functionality.
 *
 * These are compile-time constants so the unreachable code is still present
 * in the source and can be re-enabled by flipping the flag.
 */
val TEAM_HOLESHOT_ENABLED = "false".toBoolean()
val WIGGLE_ONCE_ENABLED = "false".toBoolean()
val IMMEDIATE_START_ENABLED = "true".toBoolean()
