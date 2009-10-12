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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.install.filters;

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;

/**
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractFilter implements PathFilter {

    protected final Path pattern;


    protected AbstractFilter(Path pattern) {
        this.pattern = pattern;
    }

    public boolean accept(Path path, boolean defaultPolicy) {
        // path should be in canonical form
        boolean match = segmentsMatch(pattern, path);
        return match ? !defaultPolicy : defaultPolicy;
    }

    protected static boolean segmentsMatch(Path pattern, Path path) {
        int patternLen = pattern.segmentCount();
        int k = 0;
        for (int i = 0, len = path.segmentCount(); i < len; i++) {
            if (k >= patternLen) {
                return false;
            }
            String segPattern = pattern.segment(k);
            String segment = path.segment(i);
            if (segPattern.equals("**")) {
                k++;
                if (k == patternLen) {
                    return true;
                }
                // skip non matching segments
                String match = pattern.segment(k);
                for (; i < len; i++) {
                    if (segmentMatch(match, path.segment(i))) {
                        k++;
                        break;
                    }
                }
            } else if (segmentMatch(segPattern, segment)) {
                k++;
            } else {
                return false;
            }
        }
        return k >= patternLen;
    }

    public static boolean segmentMatch(String pattern, String segment) {
        if (pattern.equals("*")) {
            return true;
        }
        int p = pattern.indexOf('*');
        if (p == -1) {
            return pattern.equals(segment);
        }
        if (p == 0) {
            if (!segment.endsWith(pattern.substring(1))) {
                return false;
            }
        } else if (p == pattern.length() - 1) {
            if (!segment.startsWith(pattern.substring(0, p))) {
                return false;
            }
        } else {
            String prefix = pattern.substring(0, p);
            String suffix = pattern.substring(p + 1);
            if (!segment.startsWith(prefix) || !segment.endsWith(suffix)) {
                return false;
            }
        }
        return true;
    }

}
