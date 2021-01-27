/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.ComparisonFailure;
import org.nuxeo.common.utils.Vars;

public class LogTracingHelper {

    private LogTracingHelper() {
        // utility class
    }

    public static List<String> readTrace(String filename) throws IOException {
        URL url = LogTracingHelper.class.getClassLoader().getResource(filename);
        if (url == null) {
            throw new IOException(filename);
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(url.toURI()));
        } catch (URISyntaxException e) {
            throw new IOException(filename, e);
        }
        for (ListIterator<String> it = lines.listIterator(); it.hasNext();) {
            String line = it.next();
            if (line.startsWith(" ") || line.startsWith("\t")) {
                line = line.trim();
                it.set(line);
            }
            // skip @startuml, @enduml and ' comments, and plantuml statements
            if (line.startsWith("@") || line.startsWith("'") || line.startsWith("participant ")) {
                it.remove();
            }
        }
        return lines;
    }

    public static void assertEqualsLists(List<String> expectedList, List<String> actualList,
            Map<String, String> context) {
        int size = Math.min(expectedList.size(), actualList.size());
        for (int i = 0; i < size; i++) {
            String expected = expectedList.get(i);
            String actual = actualList.get(i);
            matchAndCaptureVars(expected, actual, context, i + 1);
        }
        if (expectedList.size() > size) {
            fail("at line " + (size + 1) + ": Missing line: " + expectedList.get(size));
        }
        if (actualList.size() > size) {
            fail("at line " + (size + 1) + ": Unexpected line: " + actualList.get(size));
        }
    }

    // the possible names for vars
    protected static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([0-9a-zA-Z_.]+)}");

    // the possible values that a captured var is allowed to match
    protected static final String VAR_MATCHED = "[0-9a-zA-Z-_.]+";

    protected static void matchAndCaptureVars(String expected, String actual, Map<String, String> context, int line) {
        // extract var names to capture and turn into regex
        List<String> vars = new ArrayList<>();
        Matcher varMatcher = VAR_PATTERN.matcher(expected);
        // build a regex where everything is quoted except for holes to match vars
        StringBuffer regexBuilder = new StringBuffer("\\Q");
        while (varMatcher.find()) {
            String var = varMatcher.group(1);
            String value = context.get(var);
            String replacement;
            if (value == null) {
                // create a non-quoted hole and put a regex with a group to match a var
                // (in appendReplacement backslashes need to be escaped)
                replacement = "\\\\E(" + VAR_MATCHED + ")\\\\Q";
                vars.add(var);
            } else {
                replacement = value;
            }
            varMatcher.appendReplacement(regexBuilder, replacement);
        }
        varMatcher.appendTail(regexBuilder);
        regexBuilder.append("\\E");
        String regex = regexBuilder.toString();
        Matcher m = Pattern.compile(regex).matcher(actual);
        if (!m.matches()) {
            if (vars.isEmpty()) {
                throw new ComparisonFailure("at line " + line, Vars.expand(expected, context), actual);
            } else {
                throw new ComparisonFailure("at line " + line + ": Could not match", expected, actual);
            }
        }
        // collect captured vars
        for (int i = 0; i < m.groupCount(); i++) {
            String var = vars.get(i);
            String value = m.group(i + 1);
            context.put(var, value);
        }
    }

}
