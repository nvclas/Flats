package de.nvclas.flats.commands.flats;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface SubCommand {

    /**
     * Executes the respective subcommand logic based on the provided arguments.
     * This method is invoked by the parent command handler when a subcommand is matched.
     *
     * @param player The player who executed the command. Must be non-null.
     * @param args The arguments passed to the command, including the subcommand itself. Must be non-null and non-null elements.
     */
    void execute(@NotNull Player player, @NotNull String @NotNull [] args);

}
