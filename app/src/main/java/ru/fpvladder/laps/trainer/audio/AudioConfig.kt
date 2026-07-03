package ru.fpvladder.laps.trainer.audio

/**
 * ВНИМАНИЕ: при замене звуковых файлов в res/raw/ необходимо обновить
 * соответствующие константы длительности ниже, иначе тайминги будут неверными.
 */

/** Длительность stage.mp3 (подготовительный писк), мс */
const val STAGE_DURATION_MS = 571L

/** Пауза между писками stage, мс */
const val STAGE_DELAY_MS = 206L

/** Длительность buzzer.mp3 (сигнал старт/стоп), мс */
const val BUZZER_DURATION_MS = 783L

/** Длительность gate.mp3 (пролёт ворот), мс */
const val GATE_DURATION_MS = 493L
