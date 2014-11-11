/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.common.utils;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FilePathPattern {

    private final FileNamePattern[] patterns;

    public FilePathPattern(String path) {
        this(new Path(path));
    }

    public FilePathPattern(Path path) {
        String[] segments = path.segments();
        patterns = new FileNamePattern[segments.length];
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.equals("**")) {
                patterns[i] = null; // match any segments
            } else {
                patterns[i] = new FileNamePattern(segment);
            }
        }
    }

    public boolean match(String text) {
        return match(new Path(text));
    }

    public boolean match(Path path) {
        int k = 0;
        String[] segments = path.segments();
        START: for (int i = 0; i < segments.length; i++) {
            if (k == patterns.length) {
                return false;
            }
            FileNamePattern  pattern = patterns[k];
            if (pattern == null) { // segment wildcard **
                k++;
                if (k == patterns.length) {
                    return true; // last pattern segment is a wildcard
                }
                pattern = patterns[k];
                while (i < segments.length) {
                    if (pattern.match(segments[i])) {
                        k++; continue START;
                    }
                    i++;
                }
                return false;
            } else if (!pattern.match(segments[i])) {
                return false;
            } else {
                k++;
            }
        }
        if (k < patterns.length) {
            return patterns.length == k + 1 && patterns[k] == null; // match only if last segment is **
        }
        return true;
    }

}
