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
     * Parses the given command string to match a corresponding enum constant in {@link FlatsSubCommand}.
     * If the string does not match any defined constants, {@link FlatsSubCommand#NONE} is returned.
     *
     * @param command the command string to be converted into a {@link FlatsSubCommand} enum constant;
     *                must not be null and should ideally match one of the enum constants, case-insensitively.
     * @return the corresponding {@link FlatsSubCommand} enum constant if a match is found;
     * otherwise, {@link FlatsSubCommand#NONE}.
     */
    public static @NotNull FlatsSubCommand fromString(String command) {
        try {
            return FlatsSubCommand.valueOf(command.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    /**
     * Constructs the full command name by prefixing the {@link #subCommandName} with "flats".
     *
     * @return A string representing the full command name, composed of "flats" followed by the sub-command name.
     */
    public String getFullCommandName() {
        return "flats " + subCommandName;
    }
}
