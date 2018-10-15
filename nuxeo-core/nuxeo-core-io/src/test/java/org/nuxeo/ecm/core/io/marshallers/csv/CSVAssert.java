/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.io.marshallers.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 10.3
 */
public class CSVAssert {

    private CSVFormat format = CSVFormat.DEFAULT.withHeader();

    private CSVParser parser;

    private List<CSVRecord> records;

    private CSVAssert(String csv) throws IOException {
        parser = CSVParser.parse(csv, format);
        records = parser.getRecords();
    }

    public static CSVAssert on(String csv) throws IOException {
        return new CSVAssert(csv);
    }

    public CSVAssert has(String path) throws IOException {
        assertTrue("no field " + path, parser.getHeaderMap().containsKey(path));
        int pos = parser.getHeaderMap().get(path);
        String value = format.format(records.get(0).get(pos));
        String csv = path + CSVFormat.DEFAULT.getRecordSeparator() + value;
        return new CSVAssert(csv);
    }

    public CSVAssert isEquals(String expected) {
        String value = records.get(0).get(0);
        assertEquals("expected : " + expected + " but was " + value, expected, value);
        return this;
    }

    public CSVAssert isTrue() {
        String value = records.get(0).get(0);
        assertTrue("is not true", Boolean.valueOf(value));
        return this;
    }

    public CSVAssert isFalse() {
        String value = records.get(0).get(0);
        assertFalse("is not false", Boolean.valueOf(value));
        return this;
    }

    public CSVAssert isNull() {
        assertTrue("value is not null", StringUtils.isBlank(records.get(0).get(0)));
        return this;
    }

    public CSVAssert length(int size) {
        assertEquals("expected : " + size + " but was " + records.size(), size, records.size());
        return this;
    }

    public CSVAssert childrenContains(String path, String... values) {
        int pos = parser.getHeaderMap().get(path);
        for (String value : values) {
            assertTrue("value " + value + "has not been found",
                    records.stream().anyMatch(record -> value.equals(record.get(pos))));
        }
        return this;
    }
}
