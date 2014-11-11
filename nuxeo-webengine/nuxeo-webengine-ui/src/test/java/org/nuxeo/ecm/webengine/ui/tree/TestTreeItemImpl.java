/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.ui.tree;

import junit.framework.TestCase;

public class TestTreeItemImpl extends TestCase {

    public void testEquals() {
        ContentProvider cp = new FakeContentProvider();

        TreeItem ti1 = new TreeItemImpl(cp, "foo");
        TreeItem ti2 = new TreeItemImpl(cp, "foo");
        TreeItem ti3 = new TreeItemImpl(cp, "bar");

        assertEquals(ti1, ti2);
        assertFalse(ti1.equals(ti3));
    }

}
