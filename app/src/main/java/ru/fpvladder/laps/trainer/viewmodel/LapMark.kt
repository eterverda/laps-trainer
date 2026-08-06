package ru.fpvladder.laps.trainer.viewmodel

/**
 * Transient mark left on a lap row by the ERROR/FIX buttons.
 * Exists only while the flight is running; never serialized.
 * Effective lap success is decided by the last mark: [ERROR] means failed,
 * [FIX] or no marks means successful.
 */
enum class LapMark { ERROR, FIX }
