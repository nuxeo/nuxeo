/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.update;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.FileMatcher;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JarUtils {

    // the r? is for supporting version like: caja-r1234
    public final static Pattern JAR_NAME = Pattern.compile("(.+)-(r?[0-9]+.*)\\.jar");

    public final static Pattern JAR_WITHOUT_VERSION_NAME = Pattern.compile("(.+)\\.jar");

    /**
     * Try to find the version part in the given JAR name. Return null if name is not containing a version, otherwise
     * return a match object with the name without the version part and the extension in the 'Match.object' field.
     *
     * @param name
     */
    public static Match<String> findJarVersion(String name) {
        Matcher m = JAR_NAME.matcher(name);
        if (m.matches()) {
            Match<String> result = new Match<>();
            result.object = m.group(1);
            result.version = m.group(2);
            return result;
        }
        // try to find without version
        m = JAR_WITHOUT_VERSION_NAME.matcher(name);
        if (m.matches()) {
            Match<String> result = new Match<>();
            result.object = m.group(1);
            result.version = UpdateManager.STUDIO_SNAPSHOT_VERSION;
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
            FileMatcher fm = FileMatcher.getMatcher(filePattern.getName().concat("-{v:r?[0-9]+.*}\\.jar"));
            String studioSnapshotName = filePattern.getName().concat(".jar");
            for (File f : files) {
                if (fm.match(f.getName())) {
                    Match<File> result = new Match<>();
                    result.version = fm.getValue();
                    result.object = f;
                    return result;
                }
                if (studioSnapshotName.equals(f.getName())) {
                    Match<File> result = new Match<>();
                    result.version = UpdateManager.STUDIO_SNAPSHOT_VERSION;
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
