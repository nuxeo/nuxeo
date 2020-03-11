/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Compare versions of files as they are usually set. Maven classifiers are not managed: a classifier will be considered
 * as being part of the version. Maven "SNAPSHOT" keyword is taken in account. Rule is: x-SNAPSHOT < x <
 * x-AnythingButSNAPSHOT < x.y-SNAPSHOT < x.y
 *
 * @since 5.5
 */
public class FileVersion implements Comparable<FileVersion> {

    protected static final String SNAPSHOT = "-SNAPSHOT";

    protected String version;

    protected boolean snapshot;

    protected boolean specialQualifier;

    protected Integer[] splitVersion;

    protected String qualifier = "";

    private String separator = "";

    private String tmpVersion;

    private static final Pattern SPECIAL_QUALIFIER = Pattern.compile("^-(((RC)|(rc)|(alpha)|(ALPHA)|(beta)|(BETA)\\d+)|([a-zA-Z][0-9]{8})).*$");

    public String getQualifier() {
        return qualifier;
    }

    public Integer[] getSplitVersion() {
        return splitVersion;
    }

    public FileVersion(String value) {
        this.version = value;
        this.snapshot = value.endsWith(SNAPSHOT);
        split(getVersionWithoutSnapshot());
        this.specialQualifier = SPECIAL_QUALIFIER.matcher(qualifier).matches();
    }

    public void split(String value) {
        if (value.startsWith("r")) {
            // special case for caja-r1234 versions
            value = value.substring(1);
        }
        List<Integer> versions = new ArrayList<>();
        this.tmpVersion = value;
        do {
            if (".".equals(separator)) {
                try {
                    versions.add(Integer.valueOf(tmpVersion));
                    break;
                } catch (NumberFormatException e) {
                }
            }
            if (splitWith(".", versions)) {
                continue;
            }
            if (splitWith("-", versions)) {
                continue;
            }
            if (splitWith("_", versions)) {
                continue;
            }
            qualifier = separator + tmpVersion;
            break;
        } while (true);
        splitVersion = versions.toArray(new Integer[0]);
    }

    private boolean splitWith(String token, List<Integer> versions) {
        try {
            int index = tmpVersion.indexOf(token);
            if (index > 0) {
                versions.add(Integer.valueOf(tmpVersion.substring(0, index)));
                separator = tmpVersion.substring(index, index + 1);
                tmpVersion = tmpVersion.substring(index + 1);
                return true;
            } else {
                // treat versions containing only major versions: "1-q", "2"
                // etc.
                if (versions.isEmpty()) {
                    versions.add(Integer.valueOf(tmpVersion));
                    return false;
                }
            }
        } catch (NumberFormatException e) {
        }
        return false;
    }

    @Override
    public int compareTo(FileVersion o) {
        if (snapshot && getVersionWithoutSnapshot().equals(o.getVersion()) || specialQualifier
                && getVersionWithoutQualifier().equals(o.getVersion())) {
            return -1;
        } else if (o.isSnapshot() && version.equals(o.getVersionWithoutSnapshot()) || o.hasSpecialQualifier()
                && version.equals(o.getVersionWithoutQualifier())) {
            return 1;
        }

        int index = 0, number, oNumber, result;
        do {
            if (splitVersion.length > index) {
                number = splitVersion[index];
            } else {
                number = 0;
            }
            if (o.getSplitVersion().length > index) {
                oNumber = o.getSplitVersion()[index];
            } else {
                oNumber = 0;
            }
            result = number - oNumber;
            index++;
        } while (result == 0 && (splitVersion.length > index || o.getSplitVersion().length > index));
        if (result == 0) {
            result = qualifier.compareTo(o.getQualifier());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o || o != null && (o instanceof FileVersion) && compareTo((FileVersion) o) == 0);
    }

    public String getVersion() {
        return version;
    }

    public String getVersionWithoutSnapshot() {
        if (snapshot) {
            return version.substring(0, version.lastIndexOf(SNAPSHOT));
        }
        return version;
    }

    public boolean greaterThan(FileVersion other) {
        return compareTo(other) > 0;
    }

    public boolean lessThan(FileVersion other) {
        return compareTo(other) < 0;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    /**
     * @since 5.6
     */
    public boolean hasSpecialQualifier() {
        return specialQualifier;
    }

    /**
     * @since 5.6
     */
    public String getVersionWithoutQualifier() {
        return version.substring(0, version.lastIndexOf(qualifier));
    }

    @Override
    public String toString() {
        return version;
    }

}
