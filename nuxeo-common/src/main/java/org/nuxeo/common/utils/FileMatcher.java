/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu, jcarsique
 */
package org.nuxeo.common.utils;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileMatcher {

    private static final Pattern VAR = Pattern.compile("(.*)?\\{(.+):(.+)\\}(.*)?");

    public static FileMatcher getMatcher(String path) {
        Matcher m = VAR.matcher(path);
        if (m.matches()) {
            String prefix = m.group(1);
            String key = m.group(2).trim();
            String value = m.group(3).trim();
            String suffix = m.group(4);
            Pattern pattern = null;
            if (prefix.length() == 0) {
                if (suffix.length() == 0) {
                    pattern = Pattern.compile("(" + value + ")");
                } else {
                    pattern = Pattern.compile("(" + value + ")" + suffix);
                }
            } else if (suffix.length() == 0) {
                pattern = Pattern.compile(prefix + "(" + value + ")");
            } else {
                pattern = Pattern.compile(prefix + "(" + value + ")" + suffix);
            }
            return new FileMatcher(pattern, key);
        }
        return new FileMatcher(null, path);
    }

    public static FileMatcher getMatcher(File file) {
        return getMatcher(file.getName());
    }

    /**
     * Look for a matching file for the given path.
     *
     * @param path Searched file path, optionally including the pattern.
     * @param map The pattern variable will be put in the given map if any.
     *            Since 5.5, map can be null.
     *
     * @return File found. Null if none.
     */
    public static File getMatchingFile(String path, Map<String, Object> map) {
        File file = new File(path);
        FileMatcher matcher = getMatcher(path);
        if (matcher.getPattern() == null) {
            return file;
        }
        // a pattern -> find the first file that match that pattern
        File dir = file.getParentFile();
        File[] list = dir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (matcher.match(f.getName())) {
                    if (map != null) {
                        map.put(matcher.getKey(), matcher.getValue());
                    }
                    return f;
                }
            }
        }
        return null;
    }

    protected final Pattern pattern;

    protected final String key;

    protected String value;

    public FileMatcher(Pattern pattern, String key) {
        this.pattern = pattern;
        this.key = key;
    }

    public String getKey() {
        return pattern != null ? key : null;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getValue() {
        return value;
    }

    public boolean match(String name) {
        value = null;
        if (pattern != null) {
            Matcher m = pattern.matcher(name);
            if (m.matches()) {
                value = m.group(1);
                return true;
            }
            return false;
        } else if (name.equals(key)) {
            return true;
        }
        return false;
    }

}
