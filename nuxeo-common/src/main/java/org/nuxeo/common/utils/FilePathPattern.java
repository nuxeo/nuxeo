/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
        this (new Path(path));
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
