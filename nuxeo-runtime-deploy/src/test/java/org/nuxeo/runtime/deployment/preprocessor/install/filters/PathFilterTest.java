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

import org.junit.Test;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilterSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PathFilterTest {

    @Test
    public void testExclude() {
        PathFilterSet filters = new PathFilterSet();
        filters.add(new ExcludeFilter("x"));
        filters.add(new ExcludeFilter("a/b/c"));
        filters.add(new ExcludeFilter("/c/d"));
        filters.add(new ExcludeFilter("e/f"));

        assertFalse(filters.accept(new Path("x")));
        assertFalse(filters.accept(new Path("/x")));
        assertTrue(filters.accept(new Path("y/x")));
        assertTrue(filters.accept(new Path("x/y")));
        assertTrue(filters.accept(new Path("y")));

        assertFalse(filters.accept(new Path("a/b/c")));
        assertFalse(filters.accept(new Path("/a/b/c")));
        assertTrue(filters.accept(new Path("z/a/b/c")));
        assertTrue(filters.accept(new Path("z/a/b/c/z")));
        assertTrue(filters.accept(new Path("a/b/c/z")));

        assertFalse(filters.accept(new Path("/c/d")));
        assertFalse(filters.accept(new Path("c/d")));
        assertTrue(filters.accept(new Path("z/c/d"))); // abs. patterns should match entire path

        assertFalse(filters.accept(new Path("e/f")));
        assertFalse(filters.accept(new Path("/e/f/")));
        assertTrue(filters.accept(new Path("z/e/f")));
        assertTrue(filters.accept(new Path("e/f/z")));
        assertTrue(filters.accept(new Path("w/e/f/z")));
    }

    @Test
    public void testWildcard() {
        ExcludeFilter filter = new ExcludeFilter("/META-INF");

        //assertTrue(filter.accept(new Path("a/b")));
        //assertFalse(filter.accept(new Path("/META-INF")));
        assertFalse(filter.accept(new Path("/META-INF")));
        assertTrue(filter.accept(new Path("/META-INF/notexcluded")));

        filter = new ExcludeFilter("/META-INF/**");
        assertTrue(filter.accept(new Path("/META-INF")));
        assertFalse(filter.accept(new Path("/META-INF/excluded")));

        filter = new ExcludeFilter("/META-INF/*");
        assertTrue(filter.accept(new Path("/META-INF")));
        assertFalse(filter.accept(new Path("/META-INF/excluded")));
        assertTrue(filter.accept(new Path("/META-INF/excluded/notexcluded")));

        filter = new ExcludeFilter("/META-INF/**/test");
        assertTrue(filter.accept(new Path("/META-INF")));
        assertTrue(filter.accept(new Path("/META-INF/excluded")));
        assertFalse(filter.accept(new Path("/META-INF/excluded1/excluded2/test")));

        filter = new ExcludeFilter("/META-INF/*/test");
        assertTrue(filter.accept(new Path("/META-INF")));
        assertTrue(filter.accept(new Path("/META-INF/excluded")));
        assertFalse(filter.accept(new Path("/META-INF/excluded/test")));
        assertTrue(filter.accept(new Path("/META-INF/excluded/test/accepted")));

        filter = new ExcludeFilter("/META-INF/*/test/**");
        assertTrue(filter.accept(new Path("/META-INF")));
        assertTrue(filter.accept(new Path("/META-INF/excluded")));
        assertTrue(filter.accept(new Path("/META-INF/excluded/test")));
        assertFalse(filter.accept(new Path("/META-INF/excluded/test/not/accepted")));

        filter = new ExcludeFilter("**/META-INF");
        assertFalse(filter.accept(new Path("/META-INF")));
        assertTrue(filter.accept(new Path("/META-INF/accepted")));
        assertFalse(filter.accept(new Path("excluded/META-INF/")));
        assertTrue(filter.accept(new Path("accepted/META-INF/accepted")));

        filter = new ExcludeFilter("**");
        assertFalse(filter.accept(new Path("/META-INF")));
        assertFalse(filter.accept(new Path("/META-INF/exclude")));

        filter = new ExcludeFilter("*");
        assertFalse(filter.accept(new Path("/META-INF")));
        assertTrue(filter.accept(new Path("/META-INF/exclude")));

        filter = new ExcludeFilter("*abc");
        assertTrue(filter.accept(new Path("abcd")));
        assertFalse(filter.accept(new Path("test_abc")));

        filter = new ExcludeFilter("abc*");
        assertTrue(filter.accept(new Path("test_abc_test")));
        assertFalse(filter.accept(new Path("abc_test")));

        filter = new ExcludeFilter("a*bc");
        assertTrue(filter.accept(new Path("a_bc_")));
        assertFalse(filter.accept(new Path("a_bc")));

        filter = new ExcludeFilter("**/*.xml");
        assertTrue(filter.accept(new Path("test.html")));
        assertTrue(filter.accept(new Path("test/test.html")));
        assertFalse(filter.accept(new Path("test.xml")));
        assertFalse(filter.accept(new Path("test/test.xml")));
    }

    //TODO: fix include -> multiple include should be evealuated using OR
    @Test
    public void testInclude() {
        PathFilterSet filters = new PathFilterSet();
        filters.add(new IncludeFilter("x"));
        filters.add(new IncludeFilter("a/b/c"));
        filters.add(new IncludeFilter("/c/d"));
        filters.add(new IncludeFilter("e/f"));

        assertTrue(filters.accept(new Path("x")));
        assertTrue(filters.accept(new Path("/x")));
        assertFalse(filters.accept(new Path("y/x")));
        assertFalse(filters.accept(new Path("x/y")));
        assertFalse(filters.accept(new Path("y")));

        assertTrue(filters.accept(new Path("a/b/c")));
        assertTrue(filters.accept(new Path("/a/b/c")));
        assertFalse(filters.accept(new Path("z/a/b/c")));
        assertFalse(filters.accept(new Path("z/a/b/c/z")));
        assertFalse(filters.accept(new Path("a/b/c/z")));

        assertTrue(filters.accept(new Path("/c/d")));
        assertTrue(filters.accept(new Path("c/d")));
        assertFalse(filters.accept(new Path("z/c/d"))); // abs. pattterns should match entire path

        assertTrue(filters.accept(new Path("e/f")));
        assertTrue(filters.accept(new Path("/e/f/")));
        assertFalse(filters.accept(new Path("z/e/f")));
        assertFalse(filters.accept(new Path("e/f/z")));
        assertFalse(filters.accept(new Path("w/e/f/z")));
    }

    @Test
    public void testMixIncludeExclude() {
        PathFilterSet filters = new PathFilterSet();
        filters.add(new IncludeFilter("org/nuxeo/**"));
        filters.add(new IncludeFilter("com/**"));
        filters.add(new ExcludeFilter("**/*.xml"));

        assertFalse(filters.accept(new Path("net/nuxeo")));
        assertTrue(filters.accept(new Path("org/nuxeo/ecm")));
        assertFalse(filters.accept(new Path("test.xml")));
        assertFalse(filters.accept(new Path("org/nuxeo/ecm/test.xml")));
        assertTrue(filters.accept(new Path("org/nuxeo/ecm/test.java")));
        assertTrue(filters.accept(new Path("com/test.java")));
        assertFalse(filters.accept(new Path("com/test.xml")));
    }

}
