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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @since 2025.11
 */
public class TestByteSize {

    @Test
    public void testParse() {
        assertEquals(ByteSize.ofBytes(123), ByteSize.parse("123"));
        assertEquals(ByteSize.ofBytes(123), ByteSize.parse("123b"));
        assertEquals(ByteSize.ofBytes(123), ByteSize.parse("123B"));

        assertEquals(ByteSize.ofKibibytes(8), ByteSize.parse("8192"));
        assertEquals(ByteSize.ofKibibytes(8), ByteSize.parse("8k"));
        assertEquals(ByteSize.ofKibibytes(8), ByteSize.parse("8K"));
        assertEquals(ByteSize.ofKibibytes(8), ByteSize.parse("8KB"));
        assertEquals(ByteSize.ofKibibytes(8), ByteSize.parse("8KiB"));

        assertEquals(ByteSize.ofMebibytes(512), ByteSize.parse("536870912"));
        assertEquals(ByteSize.ofMebibytes(512), ByteSize.parse("512m"));
        assertEquals(ByteSize.ofMebibytes(512), ByteSize.parse("512M"));
        assertEquals(ByteSize.ofMebibytes(512), ByteSize.parse("512MB"));
        assertEquals(ByteSize.ofMebibytes(512), ByteSize.parse("512MiB"));

        assertEquals(ByteSize.ofGibibytes(2), ByteSize.parse("2147483648"));
        assertEquals(ByteSize.ofGibibytes(2), ByteSize.parse("2g"));
        assertEquals(ByteSize.ofGibibytes(2), ByteSize.parse("2G"));
        assertEquals(ByteSize.ofGibibytes(2), ByteSize.parse("2GB"));
        assertEquals(ByteSize.ofGibibytes(2), ByteSize.parse("2GiB"));

        assertEquals(ByteSize.ofTebibytes(1), ByteSize.parse("1099511627776"));
        assertEquals(ByteSize.ofTebibytes(1), ByteSize.parse("1t"));
        assertEquals(ByteSize.ofTebibytes(1), ByteSize.parse("1T"));
        assertEquals(ByteSize.ofTebibytes(1), ByteSize.parse("1TB"));
        assertEquals(ByteSize.ofTebibytes(1), ByteSize.parse("1TiB"));

        assertEquals(ByteSize.ofPebibytes(1), ByteSize.parse("1125899906842624"));
        assertEquals(ByteSize.ofPebibytes(1), ByteSize.parse("1p"));
        assertEquals(ByteSize.ofPebibytes(1), ByteSize.parse("1P"));
        assertEquals(ByteSize.ofPebibytes(1), ByteSize.parse("1PB"));
        assertEquals(ByteSize.ofPebibytes(1), ByteSize.parse("1PiB"));
    }

    /** @deprecated since 2025.11 */
    @Test
    @Deprecated(since = "2025.11", forRemoval = true)
    public void testParseDeprecatedSizeUtilsMechanism() {
        assertEquals(ByteSize.ofBytes(123), ByteSize.parse("123 B"));
        assertEquals(ByteSize.ofKibibytes(8), ByteSize.parse("8 KB"));
        assertEquals(ByteSize.ofMebibytes(512), ByteSize.parse("512 MB"));
        assertEquals(ByteSize.ofGibibytes(2), ByteSize.parse("2 GB"));
        assertEquals(ByteSize.ofTebibytes(1), ByteSize.parse("1 TB"));
        assertEquals(ByteSize.ofPebibytes(1), ByteSize.parse("1 PB"));

    }

    @Test
    public void testToStringInternalSystemUnit() {
        assertEquals("123456789", ByteSize.Formatter.INTERNATIONAL_SYSTEM_UNIT.format(ByteSize.ofBytes(123456789)));
        assertEquals("8KiB", ByteSize.Formatter.INTERNATIONAL_SYSTEM_UNIT.format(ByteSize.ofKibibytes(8)));
        assertEquals("512MiB", ByteSize.Formatter.INTERNATIONAL_SYSTEM_UNIT.format(ByteSize.ofMebibytes(512)));
        assertEquals("2GiB", ByteSize.Formatter.INTERNATIONAL_SYSTEM_UNIT.format(ByteSize.ofGibibytes(2)));
        assertEquals("1TiB", ByteSize.Formatter.INTERNATIONAL_SYSTEM_UNIT.format(ByteSize.ofTebibytes(1)));
        assertEquals("1PiB", ByteSize.Formatter.INTERNATIONAL_SYSTEM_UNIT.format(ByteSize.ofPebibytes(1)));
    }

    @Test
    public void testToStringJvmHeapSize() {
        assertEquals("123456789", ByteSize.Formatter.JVM_HEAP_SIZE.format(ByteSize.ofBytes(123456789)));
        assertEquals("8k", ByteSize.Formatter.JVM_HEAP_SIZE.format(ByteSize.ofKibibytes(8)));
        assertEquals("512m", ByteSize.Formatter.JVM_HEAP_SIZE.format(ByteSize.ofMebibytes(512)));
        assertEquals("2g", ByteSize.Formatter.JVM_HEAP_SIZE.format(ByteSize.ofGibibytes(2)));
        assertEquals("1t", ByteSize.Formatter.JVM_HEAP_SIZE.format(ByteSize.ofTebibytes(1)));
        assertEquals("1p", ByteSize.Formatter.JVM_HEAP_SIZE.format(ByteSize.ofPebibytes(1)));
    }
}
