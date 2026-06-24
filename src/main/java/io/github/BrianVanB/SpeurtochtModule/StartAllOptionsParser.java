package io.github.BrianVanB.SpeurtochtModule;

import java.util.ArrayList;
import java.util.List;

public final class StartAllOptionsParser {

    private StartAllOptionsParser() {
        /*
         * Utility class.
         * Niet instantiëren.
         */
    }

    public static StartAllOptions parse(String[] args)
            throws StartAllOptionsParseException {

        TimerMode timerMode = TimerMode.COUNTDOWN;
        Integer configuredMinutes = null;
        String teamName = null;

        boolean hasCountdownFlag = false;
        boolean hasCountupFlag = false;

        int index = 0;

        while (index < args.length) {
            String arg = args[index];

            if (arg.equalsIgnoreCase("--countdown")) {
                hasCountdownFlag = true;
                timerMode = TimerMode.COUNTDOWN;
                index++;
                continue;
            }

            if (arg.equalsIgnoreCase("--countup")) {
                hasCountupFlag = true;
                timerMode = TimerMode.COUNTUP;
                index++;
                continue;
            }

            if (arg.equalsIgnoreCase("--team")) {
                TeamParseResult teamResult = parseTeamName(args, index + 1);
                teamName = teamResult.teamName();
                index = teamResult.nextIndex();
                continue;
            }

            if (arg.toLowerCase().startsWith("--team=")) {
                TeamParseResult teamResult = parseTeamNameFromEqualsSyntax(args, index);
                teamName = teamResult.teamName();
                index = teamResult.nextIndex();
                continue;
            }

            if (arg.startsWith("--")) {
                throw new StartAllOptionsParseException(
                        "Onbekende flag: " + arg
                );
            }

            if (isPositiveInteger(arg)) {
                if (configuredMinutes != null) {
                    throw new StartAllOptionsParseException(
                            "Je hebt meerdere tijden opgegeven. Gebruik maar één aantal minuten."
                    );
                }

                configuredMinutes = Integer.parseInt(arg);

                if (configuredMinutes <= 0) {
                    throw new StartAllOptionsParseException(
                            "De tijd moet groter zijn dan 0 minuten."
                    );
                }

                index++;
                continue;
            }

            throw new StartAllOptionsParseException(
                    "Onbekend argument: " + arg
            );
        }

        if (hasCountdownFlag && hasCountupFlag) {
            throw new StartAllOptionsParseException(
                    "Gebruik niet tegelijk --countdown en --countup."
            );
        }

        if (timerMode == TimerMode.COUNTDOWN && configuredMinutes == null) {
            throw new StartAllOptionsParseException(
                    "Geef bij een aflopende timer het aantal minuten op. Bijvoorbeeld: /startall 30"
            );
        }

        if (timerMode == TimerMode.COUNTUP && teamName == null) {
            throw new StartAllOptionsParseException(
                    "Geef bij een oplopende timer een teamnaam op. Bijvoorbeeld: /startall --countup --team \"Team Blauw\""
            );
        }

        if (teamName != null) {
            validateTeamName(teamName);
        }

        return new StartAllOptions(
                timerMode,
                configuredMinutes,
                teamName
        );
    }

    private static TeamParseResult parseTeamName(String[] args, int startIndex)
            throws StartAllOptionsParseException {

        if (startIndex >= args.length) {
            throw new StartAllOptionsParseException(
                    "Geef een teamnaam op na --team."
            );
        }

        List<String> parts = new ArrayList<>();
        int index = startIndex;

        while (index < args.length) {
            String current = args[index];

            if (current.startsWith("--")) {
                break;
            }

            parts.add(current);
            index++;
        }

        if (parts.isEmpty()) {
            throw new StartAllOptionsParseException(
                    "Geef een teamnaam op na --team."
            );
        }

        String teamName = normalizeTeamName(String.join(" ", parts));

        return new TeamParseResult(teamName, index);
    }

    private static TeamParseResult parseTeamNameFromEqualsSyntax(
            String[] args,
            int teamArgIndex
    ) throws StartAllOptionsParseException {

        String firstArg = args[teamArgIndex];

        String firstValuePart = firstArg.substring(firstArg.indexOf('=') + 1);

        if (firstValuePart.isBlank()) {
            throw new StartAllOptionsParseException(
                    "Geef een teamnaam op na --team=."
            );
        }

        List<String> parts = new ArrayList<>();
        parts.add(firstValuePart);

        int index = teamArgIndex + 1;

        while (index < args.length) {
            String current = args[index];

            if (current.startsWith("--")) {
                break;
            }

            /*
             * Hiermee ondersteunen we ook:
             *
             * /startall --countup --team="Team Blauw"
             *
             * Bukkit kan dat splitsen naar:
             * --team="Team
             * Blauw"
             */
            parts.add(current);
            index++;
        }

        String teamName = normalizeTeamName(String.join(" ", parts));

        return new TeamParseResult(teamName, index);
    }

    private static String normalizeTeamName(String rawTeamName) {
        String normalized = rawTeamName.trim();

        if (
                (normalized.startsWith("\"") && normalized.endsWith("\"")) ||
                        (normalized.startsWith("'") && normalized.endsWith("'"))
        ) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        return normalized.trim();
    }

    private static void validateTeamName(String teamName)
            throws StartAllOptionsParseException {

        if (teamName.isBlank()) {
            throw new StartAllOptionsParseException(
                    "Teamnaam mag niet leeg zijn."
            );
        }

        if (teamName.length() < 2) {
            throw new StartAllOptionsParseException(
                    "Teamnaam moet minimaal 2 tekens bevatten."
            );
        }

        if (teamName.length() > 48) {
            throw new StartAllOptionsParseException(
                    "Teamnaam mag maximaal 48 tekens bevatten."
            );
        }

        if (!teamName.matches("[\\p{L}\\p{N} ._\\-]+")) {
            throw new StartAllOptionsParseException(
                    "Teamnaam mag alleen letters, cijfers, spaties, punten, underscores en streepjes bevatten."
            );
        }
    }

    private static boolean isPositiveInteger(String value) {
        return value.matches("\\d+");
    }

    private record TeamParseResult(String teamName, int nextIndex) {
    }
}