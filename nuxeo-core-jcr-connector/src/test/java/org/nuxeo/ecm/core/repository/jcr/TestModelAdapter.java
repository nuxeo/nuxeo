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
 *     Florent Guillaume
 *
 * $Id: TestModelAdapter.java 22140 2007-07-07 23:04:44Z fguillaume $
 */

package org.nuxeo.ecm.core.repository.jcr;

import org.nuxeo.common.utils.Path;

import junit.framework.TestCase;

/**
 * @author Florent Guillaume
 *
 */
public class TestModelAdapter extends TestCase {

    public void testPath2Jcr() {
        Path path;
        path = new Path("");
        assertEquals("", ModelAdapter.path2Jcr(path));
        path = new Path("foo");
        assertEquals("ecm:children/foo", ModelAdapter.path2Jcr(path));
        path = new Path("foo/bar");
        assertEquals("ecm:children/foo/ecm:children/bar",
                ModelAdapter.path2Jcr(path));
        path = new Path("..");
        assertEquals("../..", ModelAdapter.path2Jcr(path));
        path = new Path("../bar");
        assertEquals("../../ecm:children/bar", ModelAdapter.path2Jcr(path));
        path = new Path("../../bar");
        assertEquals("../../../../ecm:children/bar", ModelAdapter.path2Jcr(path));
        path = new Path("foo/..");
        assertEquals("", ModelAdapter.path2Jcr(path));
        path = new Path("foo/../bar");
        assertEquals("ecm:children/bar", ModelAdapter.path2Jcr(path));
        path = new Path("foo/bar/..");
        assertEquals("ecm:children/foo", ModelAdapter.path2Jcr(path));
    }
}
