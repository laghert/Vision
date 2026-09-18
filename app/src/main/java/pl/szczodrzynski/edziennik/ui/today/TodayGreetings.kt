package pl.szczodrzynski.edziennik.ui.today

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TodayGreetings {

    fun getGreeting(fullName: String?, now: Long, locale: Locale): String {
        val firstName = extractFirstName(fullName)
        val cal = Calendar.getInstance(locale).apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        val hasName = firstName.isNotBlank()
        val name = firstName.ifBlank { "" }

        // Seed ensures greeting stays stable within an hour but varies across hours and days
        val seed = dayOfMonth * 13 + hour

        val variants: List<String> = when {
            // Late night (23:00 - 04:59)
            hour >= 23 || hour < 5 -> listOf(
                if (hasName) "Jeszcze nie śpisz, $name? 🦉" else "Jeszcze nie śpisz? 🦉",
                if (hasName) "Pora powoli lądować w łóżku, $name 😴" else "Pora powoli lądować w łóżku 😴",
                if (hasName) "Dobrej i spokojnej nocy, $name 💤" else "Dobrej i spokojnej nocy 💤",
                if (hasName) "Nocny marek z Ciebie, $name 🌠" else "Nocny marek z Ciebie 🌠",
            )
            // Saturday & Sunday (Weekend)
            dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY -> listOf(
                if (hasName) "Udanego weekendu, $name! 🥳" else "Udanego weekendu! 🥳",
                if (hasName) "Ładuj baterie na nowy tydzień, $name 🔋" else "Ładuj baterie na nowy tydzień 🔋",
                if (hasName) "Spokojnego weekendu, $name ☕" else "Spokojnego weekendu ☕",
                if (hasName) "Odpoczywaj i korzystaj z wolnego, $name 🎮" else "Odpoczywaj i korzystaj z wolnego 🎮",
            )
            // Thursday (Czwartek - specifically requested!)
            dayOfWeek == Calendar.THURSDAY -> when {
                hour in 5..11 -> listOf(
                    if (hasName) "Dzień dobry w czwartek, $name! 🌅" else "Dzień dobry w czwartek! 🌅",
                    if (hasName) "Czwartek to już prawie piątek, $name! 🚀" else "Czwartek to już prawie piątek! 🚀",
                    if (hasName) "Dobrego czwartku w szkole, $name! 🎒" else "Dobrego czwartku w szkole! 🎒",
                    if (hasName) "Dobrej energii w ten czwartek, $name! ⚡" else "Dobrej energii w ten czwartek! ⚡",
                )
                hour in 12..17 -> listOf(
                    if (hasName) "Dobrego czwartkowego popołudnia, $name 🌤️" else "Dobrego czwartkowego popołudnia 🌤️",
                    if (hasName) "Czwartek mija, weekend tuż-tuż, $name! ⏳" else "Czwartek mija, weekend tuż-tuż! ⏳",
                    if (hasName) "Udanego czwartku, $name! 🎯" else "Udanego czwartku! 🎯",
                )
                else -> listOf(
                    if (hasName) "Spokojnego czwartkowego wieczoru, $name 🌙" else "Spokojnego czwartkowego wieczoru 🌙",
                    if (hasName) "Jutro już piątek, $name! 🎒" else "Jutro już piątek! 🎒",
                    if (hasName) "Dobry wieczór w czwartek, $name 🕯️" else "Dobry wieczór w czwartek 🕯️",
                )
            }
            // Friday (Piątek)
            dayOfWeek == Calendar.FRIDAY -> when {
                hour < 14 -> listOf(
                    if (hasName) "Dzień dobry w piątek, $name! 🌅" else "Dzień dobry w piątek! 🌅",
                    if (hasName) "Piątek – ostatnia prosta przed weekendem, $name! 🎒" else "Piątek – ostatnia prosta przed weekendem! 🎒",
                    if (hasName) "Świetnego piątku, $name! 🎉" else "Świetnego piątku! 🎉",
                )
                else -> listOf(
                    if (hasName) "Piątek, weekend blisko, $name! 🎉" else "Piątek, weekend blisko! 🎉",
                    if (hasName) "Weekendowy nastrój, $name? 🍕" else "Weekendowy nastrój? 🍕",
                    if (hasName) "Lekcje z głowy, miłego weekendu, $name! 🥳" else "Lekcje z głowy, miłego weekendu! 🥳",
                    if (hasName) "Czas na odpoczynek po całym tygodniu, $name 🌿" else "Czas na odpoczynek po całym tygodniu 🌿",
                )
            }
            // Monday (Poniedziałek)
            dayOfWeek == Calendar.MONDAY -> when {
                hour in 5..11 -> listOf(
                    if (hasName) "Dzień dobry w poniedziałek, $name! 🌅" else "Dzień dobry w poniedziałek! 🌅",
                    if (hasName) "Dobrego startu w nowy tydzień, $name! 🚀" else "Dobrego startu w nowy tydzień! 🚀",
                    if (hasName) "Świeża energia na poniedziałek, $name ⚡" else "Świeża energia na poniedziałek ⚡",
                )
                hour in 12..17 -> listOf(
                    if (hasName) "Dobrego poniedziałkowego popołudnia, $name 🌤️" else "Dobrego poniedziałkowego popołudnia 🌤️",
                    if (hasName) "Poniedziałek powoli z głowy, $name! 🎯" else "Poniedziałek powoli z głowy! 🎯",
                )
                else -> listOf(
                    if (hasName) "Dobry wieczór w poniedziałek, $name" else "Dobry wieczór w poniedziałek",
                    if (hasName) "Poniedziałek dobiega końca, $name" else "Poniedziałek dobiega końca",
                    if (hasName) "Spokojnego poniedziałkowego wieczoru, $name" else "Spokojnego poniedziałkowego wieczoru",
                )
            }
            // Tuesday (Wtorek)
            dayOfWeek == Calendar.TUESDAY -> when {
                hour in 5..11 -> listOf(
                    if (hasName) "Dzień dobry we wtorek, $name! 🌅" else "Dzień dobry we wtorek! 🌅",
                    if (hasName) "Udanego wtorku w szkole, $name! 🎒" else "Udanego wtorku w szkole! 🎒",
                    if (hasName) "Dobrej energii we wtorek, $name! ⚡" else "Dobrej energii we wtorek! ⚡",
                )
                hour in 12..17 -> listOf(
                    if (hasName) "Dobrego wtorkowego popołudnia, $name 🌤️" else "Dobrego wtorkowego popołudnia 🌤️",
                    if (hasName) "Wtorek nabiera tempa, $name! 🎯" else "Wtorek nabiera tempa! 🎯",
                )
                else -> listOf(
                    if (hasName) "Spokojnego wtorkowego wieczoru, $name 🌙" else "Spokojnego wtorkowego wieczoru 🌙",
                    if (hasName) "Wtorkowy relaks zasłużony, $name 🛋️" else "Wtorkowy relaks zasłużony 🛋️",
                )
            }
            // Wednesday (Środa)
            dayOfWeek == Calendar.WEDNESDAY -> when {
                hour in 5..11 -> listOf(
                    if (hasName) "Dzień dobry w środę, $name! 🌅" else "Dzień dobry w środę! 🌅",
                    if (hasName) "Środa – półmetek tygodnia, $name! 🐪" else "Środa – półmetek tygodnia! 🐪",
                    if (hasName) "Dobrej energii w środę, $name! ⚡" else "Dobrej energii w środę! ⚡",
                )
                hour in 12..17 -> listOf(
                    if (hasName) "Półmetek tygodnia za Tobą, $name! 🚀" else "Półmetek tygodnia za Tobą! 🚀",
                    if (hasName) "Dobrego środowego popołudnia, $name 🌤️" else "Dobrego środowego popołudnia 🌤️",
                )
                else -> listOf(
                    if (hasName) "Spokojnej środy, weekend coraz bliżej, $name! 🌙" else "Spokojnej środy, weekend coraz bliżej! 🌙",
                )
            }
            // Early morning fallback
            hour in 5..7 -> listOf(
                if (hasName) "Dzień dobry, $name! 🌅" else "Dzień dobry! 🌅",
                if (hasName) "Wczesny ptaszek z Ciebie, $name 🐣" else "Wczesny ptaszek z Ciebie 🐣",
                if (hasName) "Pora na poranny rozruch, $name ☕" else "Pora na poranny rozruch ☕",
                if (hasName) "Świetnego poranka, $name ☀️" else "Świetnego poranka ☀️",
            )
            // Morning fallback
            hour in 8..11 -> listOf(
                if (hasName) "Witaj, $name 👋" else "Witaj! 👋",
                if (hasName) "Owocnego dnia w szkole, $name! 🎒" else "Owocnego dnia w szkole! 🎒",
                if (hasName) "Dobrej energii na lekcjach, $name ⚡" else "Dobrej energii na lekcjach ⚡",
                if (hasName) "Powodzenia dzisiaj, $name 🎯" else "Powodzenia dzisiaj 🎯",
            )
            // Midday fallback
            hour in 12..15 -> listOf(
                if (hasName) "Dobrego popołudnia, $name 🌤️" else "Dobrego popołudnia 🌤️",
                if (hasName) "Półmetek lekcji za Tobą, $name 🚀" else "Półmetek lekcji za Tobą 🚀",
                if (hasName) "Jeszcze chwila i dzwonek na wolność, $name ⏳" else "Jeszcze chwila i dzwonek na wolność ⏳",
            )
            // Afternoon fallback
            hour in 16..18 -> listOf(
                if (hasName) "Pora na zasłużony odpoczynek, $name 🛋️" else "Pora na zasłużony odpoczynek 🛋️",
                if (hasName) "Lekcje z głowy, $name! 🎮" else "Lekcje z głowy! 🎮",
                if (hasName) "Spokojnego popołudnia, $name 🌿" else "Spokojnego popołudnia 🌿",
            )
            // Evening fallback
            else -> listOf(
                if (hasName) "Dobry wieczór, $name 🌙" else "Dobry wieczór 🌙",
                if (hasName) "Spokojnego wieczoru, $name 🕯️" else "Spokojnego wieczoru 🕯️",
                if (hasName) "Czas powoli zwolnić tempo, $name 🌌" else "Czas powoli zwolnić tempo 🌌",
            )
        }

        val index = Math.floorMod(seed, variants.size)
        return variants[index]
    }

    fun formatHeroDate(now: Long, locale: Locale): String {
        val cal = Calendar.getInstance(locale).apply { timeInMillis = now }
        val dayOfWeek = SimpleDateFormat("EEEE", locale).format(cal.time)
        val dayAndMonth = SimpleDateFormat("d MMMM", locale).format(cal.time)
        return "Dzisiaj mamy $dayOfWeek, $dayAndMonth"
    }

    private fun extractFirstName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val token = raw.trim()
            .split(" ", "•", "-")
            .firstOrNull { it.isNotBlank() }
            ?.replace(".", "")
            ?.trim()
            ?: return ""
        return token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
