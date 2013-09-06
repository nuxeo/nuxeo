/*
* (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
* Thierry Martins
*/

package org.nuxeo.common.utils;

import java.util.Arrays;
import java.util.Comparator;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestPathComparator {

    @Test
    public void testOrderedPath() {
        String[] paths = new String[16];
        paths[0] = "/";
        paths[1] = "/foo";
        paths[2] = "/foo.123";
        paths[3] = "/foo bar";
        paths[4] = null;
        paths[5] = " ";
        paths[6] = "";
        paths[7] = "/foo/bar";
        paths[8] = "/foo/bar.123";
        paths[9] = "/foo.123/bar";
        paths[10] = "/foo.123/bar.123";
        paths[11] = "/bar";
        paths[12] = "foo";
        paths[13] = "bar";
        paths[14] = "/foo bar.123";
        paths[15] = "/foo bar/tuck";

        String[] orderedPaths = new String[16];
        orderedPaths[0] = null;
        orderedPaths[1] = "";
        orderedPaths[2] = "/";
        orderedPaths[3] = "/bar";
        orderedPaths[4] = "/foo";
        orderedPaths[5] = "/foo/bar";
        orderedPaths[6] = "/foo/bar.123";
        orderedPaths[7] = "/foo bar";
        orderedPaths[8] = "/foo bar/tuck";
        orderedPaths[9] = "/foo bar.123";
        orderedPaths[10] = "/foo.123";
        orderedPaths[11] = "/foo.123/bar";
        orderedPaths[12] = "/foo.123/bar.123";
        orderedPaths[13] = " ";
        orderedPaths[14] = "bar";
        orderedPaths[15] = "foo";

        Arrays.sort(paths, new PathComparator());
        System.out.println(Arrays.toString(paths));
        System.out.println(Arrays.toString(orderedPaths));

        assertArrayEquals(orderedPaths, paths);

    }

    public class PathComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else {
                String path1 = o1.replace("/", "\u0001");
                String path2 = o2.replace("/", "\u0001");
                return path1.compareTo(path2);

            }
        }
    }

}
