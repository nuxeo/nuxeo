/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.fprpc.tests.fake;

import java.io.InputStream;
import java.util.List;

import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dummy.DummyMemoryTree;

import junit.framework.TestCase;

public class TestDummySpiTree extends TestCase {

    public void testTree() throws Exception{
        List<WSSListItem> items;

        DummyMemoryTree.resetInstance();
        DummyMemoryTree instance = DummyMemoryTree.instance();

        items = instance.listItems("/");
        assertNotNull(items);
        assertEquals(5, items.size());

        items = instance.listItems("DocLib0");
        assertNotNull(items);
        assertEquals(11, items.size());

        items = instance.listItems("/DocLib0");
        assertNotNull(items);
        assertEquals(11, items.size());

        items = instance.listItems("DoesNotExist");
        assertNull(items);

        items = instance.listItems("/DocLib0/Workspace-1-1");
        assertNotNull(items);
        assertEquals(11, items.size());

        WSSListItem item = instance.getItem("/");
        assertNotNull(item);
        assertEquals("Root node", item.getDescription());

        item = instance.getItem("/DocLib0");
        assertNotNull(item);
        assertEquals("This is Dummy Document Library 0", item.getDescription());

        item = instance.getItem("DocLib0");
        assertNotNull(item);
        assertEquals("This is Dummy Document Library 0", item.getDescription());

        item = instance.getItem("/DocLib0/Workspace-1-1");
        assertNotNull(item);
        assertTrue(item.isFolderish());
        assertEquals("This is Dummy Workspace 1", item.getDescription());

        item = instance.getItem("/DocLib0/Workspace-1-1/Document-2-1.doc");
        assertNotNull(item);
        assertFalse(item.isFolderish());
        assertEquals("This is Dummy File 1", item.getDescription());
        InputStream is = item.getStream();
        assertNotNull(is);
        byte[] buffer = new byte[1024 * 10];
        int read = is.read(buffer);
        assertTrue(read>0);
        assertEquals(7680, read);

        item = instance.getItem("/DocLib0/Workspace-1-1/toto.doc");
        assertNull(item);

    }
}
