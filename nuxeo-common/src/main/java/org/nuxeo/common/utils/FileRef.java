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
package org.nuxeo.common.utils;

import java.io.File;
import java.util.Map;

/**
 * A file reference that can handle file name patterns. A file pattern can use named variable that will be substituted
 * with the actual value of the file that matched the pattern.
 * <p>
 * Example: For a file pattern <code>nuxeo-automation-core-{v:.*}.jar</code> that will match a file named
 * <code>nuxeo-automation-core-5.3.2.jar</code> the pattern variable will be <code>v=5.3.2</code>.
 * <p>
 * Note that only one pattern variable is supported.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class FileRef {

    public static FileRef newFileRef(String path) {
        return newFileRef(new File(path));
    }

    public static FileRef newFileRef(File file) {
        if (file.getName().indexOf('{') > -1) {
            return new PatternFileRef(file);
        }
        return new ExactFileRef(file);
    }

    /**
     * Gets the file referenced by this object. If the FileRef could not be resolved then null is returned.
     *
     * @return the referred file or null if none was found.
     */
    public abstract File getFile();

    /**
     * Whether the referred file has a name pattern.
     */
    public abstract boolean hasPattern();

    /**
     * Fill the given map with pattern variables.
     */
    public abstract void fillPatternVariables(Map<String, Object> vars);

    public static class ExactFileRef extends FileRef {
        protected final File file;

        public ExactFileRef(String path) {
            this(new File(path));
        }

        public ExactFileRef(File file) {
            this.file = file;
        }

        @Override
        public File getFile() {
            return file;
        }

        @Override
        public boolean hasPattern() {
            return false;
        }

        @Override
        public void fillPatternVariables(Map<String, Object> vars) {
            // do nothing
        }
    }

    public static class PatternFileRef extends FileRef {
        protected File file;

        protected String key;

        protected String value;

        public PatternFileRef(String path) {
            this(new File(path));
        }

        public PatternFileRef(File file) {
            File dir = file.getParentFile();
            File[] files = dir.listFiles();
            if (files != null) {
                FileMatcher fm = FileMatcher.getMatcher(file);
                for (File f : files) {
                    if (fm.match(f.getName())) {
                        key = fm.getKey();
                        value = fm.getValue();
                        this.file = f;
                        break;
                    }
                }
            }
        }

        @Override
        public File getFile() {
            return file;
        }

        public String getValue() {
            return value;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean hasPattern() {
            return key != null;
        }

        @Override
        public void fillPatternVariables(Map<String, Object> vars) {
            if (key != null) {
                vars.put(key, value);
            }
        }
    }

}
