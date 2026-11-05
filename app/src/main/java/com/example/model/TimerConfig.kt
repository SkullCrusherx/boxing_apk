package com.example.model

enum class BellSoundStyle(val displayName: String, val description: String) {
    BOXING_RING_BELL("Boxing Ring Bell (Official)", "Authentic heavy ringside bell from uploaded audio"),
    THUNDEROUS_STEEL("Thunderous Steel Bell (Max)", "Explosive, high-gain heavyweight ring bell"),
    HEAVY_RING_BELL("Heavy Bronze Bell", "Classic authentic ringside bronze bell"),
    DOUBLE_CLANG("Double Ring Clang", "Two rapid heavy bell strikes (Ding-Ding)"),
    TRIPLE_MATCH_BELL("Triple Match Bell", "Three traditional fight strikes (Ding-Ding-Ding)"),
    DEEP_BRONZE_GONG("Heavyweight Gong", "Deep resonant low metallic gong"),
    ARENA_AIR_HORN("Arena Buzzer", "Sharp loud digital horn for noisy gyms")
}

enum class WarningSoundStyle(val displayName: String, val description: String) {
    BOXING_WARNING_4S("4s Boxing Warning Bell", "Official 4-second ringside warning sound"),
    WOODEN_CLAPPER("Wooden Clapper (Boosted)", "Authentic 3-strike ringside woodblock clack"),
    WARNING_BEEPS("3-Pulse Beeps", "High pitch warning pips"),
    SHARP_WOOD_KNOCK("Single Wood Knock", "Sharp ringside canvas knock"),
    MUTED("Muted / Vibrate Only", "No audible sound, vibration only")
}

enum class DarkModeOption(val displayName: String) {
    OLED_DARK("OLED Black"),
    CARBON_DARK("Carbon Dark"),
    LIGHT("Light"),
    SYSTEM("System Default")
}

enum class PauseActionPreference(val displayName: String, val description: String) {
    RESUME_PRIMARY("Resume by default", "Tapping paused button resumes, with quick restart option"),
    RESTART_PRIMARY("Restart Round by default", "Tapping paused button restarts current round immediately")
}

enum class TimerPhase(val displayName: String) {
    IDLE("READY"),
    PREPARE("GET READY"),
    ROUND("FIGHT!"),
    REST("REST"),
    COMPLETE("WORKOUT COMPLETE")
}

data class TimerPreset(
    val name: String,
    val subtitle: String,
    val rounds: Int,
    val roundDurationSec: Int,
    val restDurationSec: Int,
    val warningSec: Int = 4,
    val prepSec: Int = 5,
    val isHardRound: Boolean = false
)

data class TimerConfig(
    val totalRounds: Int = 3,
    val roundDurationSeconds: Int = 180, // 3 minutes
    val restDurationSeconds: Int = 60,   // 1 minute
    val prepDurationSeconds: Int = 5,    // 5 seconds
    val warningSeconds: Int = 4,         // 4 seconds warning time
    val warningSoundEnabled: Boolean = true,
    val bellSoundStyle: BellSoundStyle = BellSoundStyle.BOXING_RING_BELL,
    val warningSoundStyle: WarningSoundStyle = WarningSoundStyle.BOXING_WARNING_4S,
    val soundVolume: Float = 1.0f,
    val gymAudioBoost: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val darkModeOption: DarkModeOption = DarkModeOption.OLED_DARK,
    val pauseActionPreference: PauseActionPreference = PauseActionPreference.RESUME_PRIMARY,
    val keepScreenAwake: Boolean = true,
    val isHardRoundMode: Boolean = false
) {
    companion object {
        val PRESETS = listOf(
            TimerPreset("Hard Round Championship", "5 rounds x 3 min, 45s rest, 4s warning", 5, 180, 45, 4, 5, isHardRound = true),
            TimerPreset("Classic Boxing", "3 rounds x 3 min, 1 min rest, 4s warning", 3, 180, 60, 4, 5),
            TimerPreset("Amateur Bout", "3 rounds x 2 min, 1 min rest, 4s warning", 3, 120, 60, 4, 5),
            TimerPreset("Pro Championship", "12 rounds x 3 min, 1 min rest, 4s warning", 12, 180, 60, 4, 5),
            TimerPreset("MMA Title Fight", "5 rounds x 5 min, 1 min rest, 4s warning", 5, 300, 60, 4, 5),
            TimerPreset("Shadowboxing / Speed", "5 rounds x 2 min, 30 sec rest, 4s warning", 5, 120, 30, 4, 5),
            TimerPreset("HIIT Bag Tabata", "8 rounds x 20 sec, 10 sec rest, 4s warning", 8, 20, 10, 4, 5)
        )
    }
}
