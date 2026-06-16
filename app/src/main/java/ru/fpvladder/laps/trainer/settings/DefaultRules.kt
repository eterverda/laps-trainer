package ru.fpvladder.laps.trainer.settings

import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Rules
import java.util.EnumSet

object DefaultRules {
    val INDIVIDUAL: Rules.Individual = Rules.Individual(
        lapsLimit = Int.MAX_VALUE,
        timeLimitSeconds = 120,
        holeshotEnabled = true,
        showRecordKinds = EnumSet.of(
            Record.Kind.BEST_1,
            Record.Kind.BEST_3,
            Record.Kind.MOST
        )
    )

    val TEAM: Rules.Team = Rules.Team(
        lapsLimit = 50,
        timeLimitSeconds = 1800,
        holeshotEnabled = TEAM_HOLESHOT_ENABLED,
        changeMode = Rules.Team.ChangeMode.LAPS,
        swapMode = Rules.Team.SwapMode.STRAIGHT,
        showRecordKinds = EnumSet.of(
            Record.Kind.BEST_1,
            Record.Kind.MOST
        )
    )
}
