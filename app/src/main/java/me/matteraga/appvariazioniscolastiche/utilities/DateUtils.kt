package me.matteraga.appvariazioniscolastiche.utilities

import java.time.LocalDate

object DateUtils {
    // Se il giorno è domenica, aggiunge un giorno
    fun date(daysToAdd: Int): LocalDate {
        LocalDate.now().plusDays(daysToAdd.toLong()).let { date ->
            return when (date.dayOfWeek.value) {
                7 -> {
                    date.plusDays(1)
                }

                else -> {
                    date
                }
            }
        }
    }
}