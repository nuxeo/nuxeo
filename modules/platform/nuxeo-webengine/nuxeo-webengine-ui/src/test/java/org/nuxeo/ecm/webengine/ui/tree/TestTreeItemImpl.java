/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.tree;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestTreeItemImpl {

    @Test
    public void testEquals() {
        ContentProvider cp = new FakeContentProvider();

        TreeItem ti1 = new TreeItemImpl(cp, "foo");
        TreeItem ti2 = new TreeItemImpl(cp, "foo");
        TreeItem ti3 = new TreeItemImpl(cp, "bar");

        assertEquals(ti1, ti2);
        assertFalse(ti1.equals(ti3));
    }

}
