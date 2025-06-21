package de.nvclas.flats.util;


import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocationConverterTest {

    private ServerMock serverMock;

    @BeforeEach
    void setUp() {
        serverMock = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @CsvSource({
            "world,1,2,3,4,5,6,'world:1,2,3;4,5,6'",
            "test_world,10,20,30,40,50,60,'test_world:10,20,30;40,50,60'"
    })
    void getStringFromLocations(String worldName, int x1, int y1, int z1, int x2, int y2, int z2, String expected) {
        WorldMock worldMock = new WorldMock();
        worldMock.setName(worldName);
        serverMock.addWorld(worldMock);

        Location location1 = new Location(worldMock, x1, y1, z1);
        Location location2 = new Location(worldMock, x2, y2, z2);

        String result = LocationConverter.getStringFromLocations(location1, location2);

        assertEquals(expected, result);
    }

    @Test
    void getStringFromLocationsNoWorld() {
        Location location1 = new Location(null, 1, 2, 3);
        WorldMock worldMock = new WorldMock();
        serverMock.addWorld(worldMock);
        Location location2 = new Location(worldMock, 4, 5, 6);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LocationConverter.getStringFromLocations(location1, location2));
        assertEquals("First position has no world reference", exception.getMessage());
    }
}
