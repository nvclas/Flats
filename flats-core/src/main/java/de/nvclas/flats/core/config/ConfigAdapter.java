package de.nvclas.flats.core.config;

import org.bukkit.GameMode;
import org.jetbrains.annotations.NotNull;

public interface ConfigAdapter {

    @NotNull String getLanguage();

    int getMaxFlatSize();

    int getMaxClaimableFlats();

    boolean isAdvancedPermissionsEnabled();

    boolean isAutoGamemodeEnabled();

    @NotNull GameMode getInsideGamemode();

    @NotNull GameMode getOutsideGamemode();

    @NotNull String getSqliteFileName();
}
