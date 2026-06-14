package ru.fpvladder.laps.trainer.settings

enum class StartSignal(val displayName: String, val description: String) {
    FIXED("Фиксированный", "Через 1.5 секунды"),
    RANDOM("Случайный", "Через 1–3 секунды"),
    MANUAL("Ручной", "По нажатию на кнопку")
}