/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.10
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestPropertyBasedConfiguration {

    @Test
    @WithFrameworkProperty(name = "test.valid1", value = "1GiB")
    @WithFrameworkProperty(name = "test.invalid1", value = "3ZiB")
    public void testGetOptionalByteSize() {
        var props = new PropertyBasedConfiguration("test", Map.of("valid2", "512MiB", "invalid2", "d5"));
        assertEquals(ByteSize.ofGibibytes(1), props.getOptionalByteSizeProperty("valid1").orElseThrow());
        assertEquals(ByteSize.ofMebibytes(512), props.getOptionalByteSizeProperty("valid2").orElseThrow());
        assertTrue(props.getOptionalByteSizeProperty("invalid1").isEmpty());
        assertTrue(props.getOptionalByteSizeProperty("invalid2").isEmpty());
        assertThrows(NoSuchElementException.class, () -> props.getOptionalByteSizeProperty("unknown").orElseThrow());
    }

    @Test
    @WithFrameworkProperty(name = "test.valid1", value = "1h")
    @WithFrameworkProperty(name = "test.invalid1", value = "3")
    public void testGetOptionalDuration() {
        var props = new PropertyBasedConfiguration("test", Map.of("valid2", "2s", "invalid2", "d5"));
        assertEquals(Duration.ofHours(1), props.getOptionalDurationProperty("valid1").orElseThrow());
        assertEquals(Duration.ofSeconds(2), props.getOptionalDurationProperty("valid2").orElseThrow());
        assertTrue(props.getOptionalDurationProperty("invalid1").isEmpty());
        assertTrue(props.getOptionalDurationProperty("invalid2").isEmpty());
        assertThrows(NoSuchElementException.class, () -> props.getOptionalDurationProperty("unknown").orElseThrow());
    }

    @Test
    @WithFrameworkProperty(name = "test.valid1", value = "1")
    @WithFrameworkProperty(name = "test.invalid1", value = "x")
    public void testGetOptionalInteger() {
        var props = new PropertyBasedConfiguration("test", Map.of("valid2", "2", "invalid2", "z"));
        assertEquals(Integer.valueOf(1), props.getOptionalIntegerProperty("valid1").orElseThrow());
        assertEquals(Integer.valueOf(2), props.getOptionalIntegerProperty("valid2").orElseThrow());
        assertTrue(props.getOptionalIntegerProperty("invalid1").isEmpty());
        assertTrue(props.getOptionalIntegerProperty("invalid2").isEmpty());
        assertThrows(NoSuchElementException.class, () -> props.getOptionalIntegerProperty("unknown").orElseThrow());
    }

    @Test
    @WithFrameworkProperty(name = "test.valid1", value = "2147483647000")
    @WithFrameworkProperty(name = "test.invalid1", value = "1a0")
    public void testGetOptionalLong() {
        var props = new PropertyBasedConfiguration("test", Map.of("valid2", "2999986668888", "invalid2", "xyz"));
        assertEquals(Long.valueOf(2147483647000L), props.getOptionalLongProperty("valid1").orElseThrow());
        assertEquals(Long.valueOf(2999986668888L), props.getOptionalLongProperty("valid2").orElseThrow());
        assertTrue(props.getOptionalLongProperty("invalid1").isEmpty());
        assertTrue(props.getOptionalLongProperty("invalid2").isEmpty());
        assertThrows(NoSuchElementException.class, () -> props.getOptionalLongProperty("unknown").orElseThrow());
    }

    @Test
    @WithFrameworkProperty(name = "test.bar", value = "anotherValue")
    public void testGetOptionalProperty() {
        var props = new PropertyBasedConfiguration("test", Map.of("foo", "aValue"));
        assertEquals("aValue", props.getOptionalProperty("foo").orElseThrow());
        assertEquals("anotherValue", props.getOptionalProperty("bar").orElseThrow());
        assertThrows(NoSuchElementException.class, () -> props.getOptionalProperty("unknown").orElseThrow());
    }
}
