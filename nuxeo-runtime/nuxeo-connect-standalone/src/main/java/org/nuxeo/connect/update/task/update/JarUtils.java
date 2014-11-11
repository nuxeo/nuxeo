/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.update;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.FileMatcher;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JarUtils {

    // the r? is for supporting version like: caja-r1234
    public final static Pattern JAR_NAME = Pattern.compile("(.+)-(r?[0-9]+.*)\\.jar");

    /**
     * Try to find the version part in the given JAR name. Return null if name
     * is not containing a version, otherwise return a match object with the
     * name without the version part and the extension in the 'Match.object'
     * field.
     *
     * @param name
     */
    public static Match<String> findJarVersion(String name) {
        Matcher m = JAR_NAME.matcher(name);
        if (m.matches()) {
            Match<String> result = new Match<String>();
            result.object = m.group(1);
            result.version = m.group(2);
            return result;
        }
        return null;
    }

    public static Match<File> findJar(File root, String key) {
        return find(new File(root, key));
    }

    public static Match<File> find(File filePattern) {
        File dir = filePattern.getParentFile();
        File[] files = dir.listFiles();
        if (files != null) {
            FileMatcher fm = FileMatcher.getMatcher(filePattern.getName().concat("-{v:[0-9]+.*}\\.jar"));
            for (File f : files) {
                if (fm.match(f.getName())) {
                    Match<File> result = new Match<File>();
                    result.version = fm.getValue();
                    result.object = f;
                    return result;
                }
            }
        }
        return null;
    }

    static class Match<T> {

        public T object;

        public String version;
    }

}
