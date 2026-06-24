package io.github.BrianVanB.SpeurtochtModule;

public final class StartAllOptions {

    private final TimerMode timerMode;

    /*
     * Bij COUNTDOWN:
     * - configuredMinutes = duur van de timer.
     *
     * Bij COUNTUP:
     * - configuredMinutes = optionele maximale tijd.
     * - null betekent: countup zonder limiet.
     */
    private final Integer configuredMinutes;

    private final String teamName;

    public StartAllOptions(
            TimerMode timerMode,
            Integer configuredMinutes,
            String teamName
    ) {
        this.timerMode = timerMode;
        this.configuredMinutes = configuredMinutes;
        this.teamName = teamName;
    }

    public TimerMode getTimerMode() {
        return timerMode;
    }

    public Integer getConfiguredMinutes() {
        return configuredMinutes;
    }

    public boolean hasConfiguredMinutes() {
        return configuredMinutes != null;
    }

    public String getTeamName() {
        return teamName;
    }

    public boolean hasTeamName() {
        return teamName != null && !teamName.isBlank();
    }

    public boolean isCountdown() {
        return timerMode == TimerMode.COUNTDOWN;
    }

    public boolean isCountup() {
        return timerMode == TimerMode.COUNTUP;
    }
}