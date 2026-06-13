package ru.fpvladder.laps.trainer.settings

object IndividualRulePresets {
    val timeOptions: List<Pair<Int, String>> = listOf(
        60 to "1:00",
        90 to "1:30",
        120 to "2:00",
        150 to "2:30",
        180 to "3:00",
        240 to "4:00",
        300 to "5:00",
        Int.MAX_VALUE to "∞"
    )

    val lapOptions: List<Pair<Int, String>> = listOf(
        1 to "1",
        2 to "2",
        3 to "3",
        4 to "4",
        5 to "5",
        7 to "7",
        10 to "10",
        Int.MAX_VALUE to "∞"
    )
}

object TeamRulePresets {
    val timeOptions: List<Pair<Int, String>> = listOf(
        60 to "10:00",
        900 to "15:00",
        1200 to "20:00",
        1500 to "25:00",
        1800 to "30:00",
        2700 to "45:00",
        3600 to "60:00",
        Int.MAX_VALUE to "∞"
    )

    val lapOptions: List<Pair<Int, String>> = listOf(
        10 to "10×2",
        30 to "15×2",
        40 to "20×2",
        50 to "25×2",
        60 to "30×2",
        80 to "40×2",
        100 to "50×2",
        Int.MAX_VALUE to "∞"
    )
}
