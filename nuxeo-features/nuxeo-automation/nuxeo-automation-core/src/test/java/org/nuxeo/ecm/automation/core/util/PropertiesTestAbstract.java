/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ronan DANIELLOU <rdaniellou@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.assertj.core.data.MapEntry;

/**
 * @since 8.2
 */
public class PropertiesTestAbstract {

    protected void loadProperties(boolean isValueTrimmed) throws IOException {

        final String newLine = "\n";
        final String newLineInterpretedInInputData = "\\\n";
        final String key1 = "schema:property";
        final String key2 = "key2";
        final String comment = "#This is a comment";

        String[] inputValues = { "line 1", " line 2", "", "line 3 ", "  line 5 ", "", "",
                "#this is not a comment because it follows a multi-line value" };

        // loops on input values, building a multi-line single value input

        String valueOut = "";
        String valueIn = "";
        String valueExpected = "";

        for (int lineNumber = 0; lineNumber < inputValues.length; lineNumber++) {
            if (lineNumber == 0) {
                valueIn = inputValues[lineNumber];
                valueOut = valueIn;
            } else {
                valueIn += newLineInterpretedInInputData + inputValues[lineNumber];
                valueOut += newLine + inputValues[lineNumber];
            }
            if (isValueTrimmed) {
                valueExpected = valueOut.trim();
            } else {
                valueExpected = valueOut;
            }
            StringReader strReader = new StringReader(key1 + "=" + valueIn);
            assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));
        }

        // a comment at the end is ignored

        StringReader strReader = new StringReader(key1 + "=" + valueIn + newLine + comment);
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        // empty lines at the end are ignored

        strReader = new StringReader(key1 + "=" + valueIn + newLine);
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        strReader = new StringReader(key1 + "=" + valueIn + newLine + newLine);
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        // 2 values separated by a comment

        strReader = new StringReader(key1 + "=" + valueIn + newLine + comment + newLine + key2 + "=" + valueIn);
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected),
                MapEntry.entry(key2, valueExpected));

        // comment, empty and blank lines at the beginning are ignored

        strReader = new StringReader(comment + newLine + " " + newLine + key1 + "=" + "line 1");
        valueExpected = "line 1";
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        // empty line at the beginning is ignored

        strReader = new StringReader(newLine + key1 + "=" + "line 1");
        valueExpected = "line 1";
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        strReader = new StringReader(newLine + key1 + " " + "=" + "line 1");
        valueExpected = "line 1";
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        // blank line at the beginning is ignored

        strReader = new StringReader("  " + newLine + key1 + "=" + "line 1");
        valueExpected = "line 1";
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        strReader = new StringReader("  " + newLine + key1 + " " + "=" + "line 1");
        valueExpected = "line 1";
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        // empty value is accepted

        strReader = new StringReader(key1 + "=");
        valueExpected = "";
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key1, valueExpected));

        // keys are trimmed, values it depends

        valueIn = "  myValue";
        String key = "keyTrimmed";
        strReader = new StringReader("  " + key + " =" + valueIn);
        if (isValueTrimmed) {
            valueExpected = valueIn.trim();
        } else {
            valueExpected = valueIn;
        }
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key, valueExpected));

        valueIn = " " + newLineInterpretedInInputData + " myValue";
        valueOut = " " + newLine + " myValue";
        key = "keyTrimmed";
        strReader = new StringReader("  " + key + " =" + valueIn);
        if (isValueTrimmed) {
            valueExpected = valueOut.trim();
        } else {
            valueExpected = valueOut;
        }
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key, valueExpected));

        valueIn = newLineInterpretedInInputData + "myValue";
        valueOut = newLine + "myValue";
        key = "keyTrimmed";
        strReader = new StringReader("  " + key + " =" + valueIn);
        if (isValueTrimmed) {
            valueExpected = valueOut.trim();
        } else {
            valueExpected = valueOut;
        }
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key, valueExpected));

        // value contains same pattern as key=value

        valueIn = "  myValue" + " " + key + "=" + " myValue";
        key = "keyTrimmed";
        strReader = new StringReader("  " + key + " =" + valueIn);
        if (isValueTrimmed) {
            valueExpected = valueIn.trim();
        } else {
            valueExpected = valueIn;
        }
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key, valueExpected));

        valueIn = "  myValue" + newLineInterpretedInInputData + key + "=" + " myValue";
        valueExpected = "  myValue" + newLine + key + "=" + " myValue";
        key = "keyTrimmed";
        strReader = new StringReader("  " + key + " =" + valueIn);
        if (isValueTrimmed) {
            valueExpected = valueExpected.trim();
        }
        assertThat(Properties.loadProperties(strReader)).containsOnly(MapEntry.entry(key, valueExpected));

        // test asked by the support team

        // foo = bar
        // titi=toto
        // desc = hello \\
        // I'm fine \\
        // see you later \\
        // bye
        // note=please enter your note \\
        // please sign at the end \\
        // thank you
        // nature = article
        // source=book

        valueIn = "foo = bar" + newLine + "titi=toto" + newLine + "desc = hello " + newLineInterpretedInInputData
                + "I'm fine " + newLineInterpretedInInputData + "see you later " + newLineInterpretedInInputData
                + "bye" + newLine + "note=please enter your note " + newLineInterpretedInInputData
                + "please sign at the end " + newLineInterpretedInInputData + "thank you" + newLine
                + "nature = article" + newLine + "source=book";
        key = "keyTrimmed";
        strReader = new StringReader(valueIn);
        Map<String, String> output = Properties.loadProperties(strReader);
        assertThat(output).hasSize(6);
        if (isValueTrimmed) {
            assertThat(output).containsOnly(
                    MapEntry.entry("foo", "bar"),
                    MapEntry.entry("titi", "toto"),
                    MapEntry.entry("desc", "hello " + newLine + "I'm fine " + newLine + "see you later " + newLine
                            + "bye"),
                    MapEntry.entry("note", "please enter your note " + newLine + "please sign at the end " + newLine
                            + "thank you"), MapEntry.entry("nature", "article"), MapEntry.entry("source", "book"));
        } else {
            assertThat(output).containsOnly(
                    MapEntry.entry("foo", " bar"),
                    MapEntry.entry("titi", "toto"),
                    MapEntry.entry("desc", " hello " + newLine + "I'm fine " + newLine + "see you later " + newLine
                            + "bye"),
                    MapEntry.entry("note", "please enter your note " + newLine + "please sign at the end " + newLine
                            + "thank you"), MapEntry.entry("nature", " article"), MapEntry.entry("source", "book"));

        }

    }
}
