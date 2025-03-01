package de.nvclas.flats.commands;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a set of subcommands associated with the "flats" command context.
 * Each enumerated value corresponds to a specific subcommand action.
 */
@Getter
public enum FlatsSubCommand {
    SELECT("select"),
    ADD("add"),
    REMOVE("remove"),
    CLAIM("claim"),
    UNCLAIM("unclaim"),
    TRUST("trust"),
    UNTRUST("untrust"),
    INFO("info"),
    LIST("list"),
    SHOW("show"),
    UPDATE("update"),
    NONE("none");

    private final String subCommandName;

    FlatsSubCommand(String commandName) {
        this.subCommandName = commandName;
    }

    /**
     * Converts a given string to its corresponding {@link FlatsSubCommand} enum value.
     * If the input does not match any valid subcommand, {@link FlatsSubCommand#NONE} is returned.
     *
     * @param command the string representing a subcommand; expected to match one of the predefined
     *                {@code FlatsSubCommand} names (case insensitive).
     * @return the matching {@code FlatsSubCommand} enumeration value, or {@link FlatsSubCommand#NONE}
     * if no match is found.
     */
    public static @NotNull FlatsSubCommand fromString(String command) {
        try {
            return FlatsSubCommand.valueOf(command.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    /**
     * Constructs and returns the full command name by appending the subcommand name
     * to the base "flats" command.
     *
     * @return The full command name in the format "flats {subCommandName}", where
     * {@code subCommandName} is the specific subcommand assigned to this instance.
     */
    public String getFullCommandName() {
        return "flats " + subCommandName;
    }
}
