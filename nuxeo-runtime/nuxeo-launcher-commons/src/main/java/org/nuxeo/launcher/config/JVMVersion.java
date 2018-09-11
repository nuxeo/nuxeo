/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.launcher.config;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JVMVersion implements Comparable<JVMVersion> {

    public final int major;

    public final int update;

    public enum UpTo {
        MAJOR, UPDATE;
    }

    public JVMVersion(int major, int minor) {
        this.major = major;
        update = minor;
    }

    public int compareTo(JVMVersion o, UpTo upTo) {
        if (major != o.major) {
            return major - o.major;
        }
        if (upTo == UpTo.MAJOR) {
            return 0;
        }
        if (update != o.update) {
            return update - o.update;
        }
        return 0;
    }


    @Override
    public int compareTo(JVMVersion o) {
        return compareTo(o, UpTo.UPDATE);
    }

    @Override
    public String toString() {
        return String.format("%d.%d", major, update);
    }

    public static JVMVersion parse(String version) throws ParseException {
        if (version.startsWith("1.")) {
            return parsePreJdk9(version);
        } else {
            return parseJdk9(version);
        }
    }

    static final Pattern PreJDK9Pattern = Pattern.compile("1\\.(\\d)\\.\\d(?:_(\\d+))?(?:-.*)?");

    static final Pattern JDK9_PATTERN = Pattern.compile("(\\d+)(?:-ea)?(?:\\.(\\d+)(?:\\.(\\d+)(\\..*)?)?)?");

    public static JVMVersion parsePreJdk9(String version) throws ParseException {
        Matcher matcher = PreJDK9Pattern.matcher(version);
        if (!matcher.matches()) {
            throw new ParseException("Cannot parse " + version + " as a pre JVM 9 version", -1);
        }
        final int groupCount = matcher.groupCount();
        String major = matcher.group(1);
        String minor = groupCount >= 2 ? matcher.group(2) : null;
        return new JVMVersion(Integer.parseInt(major), minor == null ? 0 : Integer.parseInt(minor));
    }

    public static JVMVersion parseJdk9(String version) throws ParseException {
        Matcher matcher = JDK9_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new ParseException("Cannot parse " + version + " as a JDK 9+ version", -1);
        }
        final int groupCount = matcher.groupCount();
        String major = matcher.group(1);
        String minor = groupCount >= 2 ? matcher.group(2) : null;
        return new JVMVersion(Integer.parseInt(major), minor == null ? 0 : Integer.parseInt(minor));
    }

}
