# Hard Round & Maximum Gain Boxing Bell

Boost the boxing ring bell with a thunderous, saturated ringside steel bell sound that cuts through noisy gyms, alongside a dedicated high-intensity "Hard Round" training preset and audio amplification controls.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed during interactive clarification and will guide this implementation:

- **Confirmed Decision 1**: Implement a **Thunderous Heavy Steel Ring Bell** featuring explosive strike transients, multi-harmonic saturated body resonance, and +6dB maximum gain boost with soft-knee limiting.
- **Confirmed Decision 2**: Introduce a dedicated **Hard Round Workout Preset** (5 rounds × 3:00 min work, 45s short rest, intensified 10s warning) with distinctive visual styling and one-tap quick activation.

---

## 1. Overview & Core Concept

- **What It Does**: Upgrades the audio synthesis engine to produce an authentic, ear-catching heavy steel ring bell at maximum gain, adding a high-gain gym boost option, while providing a dedicated "Hard Round" interval training mode designed for peak intensity bag work and sparring.
- **Target Audience / Persona**: Boxers, MMA fighters, martial artists, and HIIT athletes training in loud gyms with background music or sparring noise who need an unmistakable bell strike and demanding round intervals.
- **Key Value**: The timer sound will never be drowned out by gloves hitting bags, and boxers get a purpose-built "Hard Round" preset to test their conditioning.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Selecting the Hard Round Preset**:
   - The user opens the app or taps the new **"HARD ROUND"** quick chip on the main dashboard.
   - The timer instantly configures to 5 grueling 3-minute rounds with aggressive 45-second recovery intervals and an amplified round bell.
   - The circular progress dial and round badges take on an intense glowing crimson/molten aesthetic.

2. **Auditory Experience (Hard Bell & 10s Warning)**:
   - **Start of Round**: A thunderous, metallic steel hammer strike rings out with deep resonance and punchy presence, cutting through ambient noise.
   - **Last 10 Seconds**: The authentic wooden clapper fires 3 sharp, boosted knocks, while the screen pulses with a bold *"LAST 10s! FINISH HARD!"* banner.
   - **End of Round**: The thunderous bell sounds again to signal immediate rest.

3. **Audio Settings & Gym Boost**:
   - In Settings, the user can toggle **"Gym Audio Boost (+6dB)"** and test the *Thunderous Heavy Steel Bell* with a live preview button.

### Visual Identity & Theme
- **Theme Accents**: High-voltage crimson (`#FF1E27`), fiery orange warning (`#FF5722`), and championship gold (`#FFC107`) against OLED pure black (`#000000`).
- **Visual Intensity Indicator**: When in "Hard Round" mode, a flame/glove badge displays prominently with an animated pulse.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Audio Engine Saturation vs Raw Volume**
  - *Chosen Approach*: Implement acoustic non-linear wave shaping (`tanh` soft-saturation) combined with multi-harmonic transient reinforcement.
  - *Why*: Simply raising amplitude past 1.0 causes digital clipping distortion (crackling). Non-linear saturation creates perceived loudness and punch without harsh clipping, mimicking how real metal resonates at high acoustic energy.
  - *Alternative Considered*: External MP3 files (rejected because synthesized PCM is 100% offline, zero-latency, and requires no external asset downloading).

- **Decision 2: Preset Access Hierarchy**
  - *Chosen Approach*: Add the "Hard Round" preset directly to the top bar / hero chip row for immediate 1-tap activation, in addition to the full Presets Sheet.
  - *Why*: Boxers wearing wraps or gloves need immediate access without navigating deep menus.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity (Compose)                   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
┌──────────────────────────────┐ ┌───────────────────────────┐
│     BoxingTimerScreen        │ │       SettingsSheet       │
│  - Dynamic Dial & Digits     │ │  - Gym Audio Booster      │
│  - Hard Round Quick Badge    │ │  - Bell Style Selector    │
│  - Start / Pause / Restart   │ │  - Sound Test Previews    │
└──────────────┬───────────────┘ └─────────────┬─────────────┘
               │                               │
               └───────────────┬───────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       TimerViewModel                        │
│  - StateFlow<TimerUiState>, StateFlow<TimerConfig>          │
│  - High-precision ticker loop, 10s warning evaluation       │
└──────────────────────────────┬──────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     BoxingAudioEngine                       │
│  - Synthesized PCM: Thunderous Steel Bell (High Gain)       │
│  - Tanh Soft-Clipping Saturation (+6dB Headroom Limiter)    │
│  - AudioTrack Streaming with AudioAttributes.USAGE_ALARM    │
│  - Multi-harmonic Ringside Wooden Clapper                   │
│  - Synchronized Haptic Vibrator                             │
└─────────────────────────────────────────────────────────────┘
```

### Component & State Mapping

| Component | Responsibility | Action / State Change |
|---|---|---|
| `BoxingAudioEngine` | PCM synthesis with saturated loudness | Generates `THUNDEROUS_STEEL_BELL`, applies `tanh()` soft-knee compression, uses `USAGE_ALARM` attributes |
| `TimerConfig` | Configuration data model | Adds `gymBoostEnabled: Boolean`, `BellSoundStyle.THUNDEROUS_STEEL` |
| `TimerViewModel` | Timer business logic | Coordinates Hard Round preset loading, volume boosted bell playback |
| `BoxingTimerScreen` | Primary workout UI | Adds "Hard Round" one-tap badge, enhanced warning banner, and loud bell triggers |
| `SettingsSheet` | Audio & timer adjustments | Gym Boost toggle (+6dB), bell previews, and timing customizations |
