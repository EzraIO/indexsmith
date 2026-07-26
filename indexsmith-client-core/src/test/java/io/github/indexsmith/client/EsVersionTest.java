package io.github.indexsmith.client;

import io.github.indexsmith.client.observation.*;
import io.github.indexsmith.client.policy.*;
import io.github.indexsmith.client.request.*;
import io.github.indexsmith.client.response.*;
import io.github.indexsmith.client.spi.*;
import io.github.indexsmith.client.transport.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EsVersionTest {
    @Test
    public void parsesFullVersionWithQualifier() {
        EsVersion version = EsVersion.parse("8.17.3-SNAPSHOT");

        assertEquals(8, version.getMajor());
        assertEquals(17, version.getMinor());
        assertEquals(3, version.getPatch());
    }

    @Test
    public void defaultsMissingPatchToZero() {
        assertEquals("7.17.0", EsVersion.parse("7.17").toString());
    }

    @Test
    public void comparesMajorMinorAndPatch() {
        assertTrue(EsVersion.parse("8.0.0").compareTo(EsVersion.parse("7.17.99")) > 0);
        assertTrue(EsVersion.parse("8.17.0").compareTo(EsVersion.parse("8.16.9")) > 0);
        assertTrue(EsVersion.parse("8.17.3").compareTo(EsVersion.parse("8.17.2")) > 0);
    }

    @Test
    public void behavesAsAValueObject() {
        EsVersion first = EsVersion.parse("8.17.3");
        EsVersion second = EsVersion.parse("8.17.3-SNAPSHOT");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidVersion() {
        EsVersion.parse("latest");
    }
}
