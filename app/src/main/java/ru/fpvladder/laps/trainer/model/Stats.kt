package ru.fpvladder.laps.trainer.model

sealed class Stats {
    class Individual : Stats()
    class Team : Stats()
}
