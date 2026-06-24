package io.github.BrianVanB.SpeurtochtModule;

public enum SpeurtochtSessionGroupType {
    SINGLE_WORLD,
    MULTI_WORLD;

    public static SpeurtochtSessionGroupType fromConfig(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return SINGLE_WORLD;
        }

        try {
            return SpeurtochtSessionGroupType.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return SINGLE_WORLD;
        }
    }
}