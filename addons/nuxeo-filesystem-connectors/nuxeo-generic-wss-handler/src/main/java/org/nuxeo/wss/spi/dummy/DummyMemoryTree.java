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

package org.nuxeo.wss.spi.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.wss.spi.WSSListItem;

public class DummyMemoryTree {

    protected WSSListItem root;
    protected Map<String, List<WSSListItem>> allitems = new HashMap<String, List<WSSListItem>>();

    public static final int DEPTH = 3;

    protected static DummyMemoryTree instance = null;

    public synchronized static DummyMemoryTree instance() {
        if (instance == null) {
            instance = new DummyMemoryTree();
            instance.init();
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    public WSSListItem getItem(String location) {
        // FIXME: '==' or '.equals' ?
        if (location == null || location == "" || location == "/") {
            return root;
        }
        if (location.startsWith("/")) {
            location = location.substring(1);
        }

        // Brutal search
        for (String key : allitems.keySet()) {
            for (WSSListItem item : allitems.get(key)) {
                if (item.getSubPath().equals(location)) {
                    return item;
                }
            }
        }
        return null;
    }

    public List<WSSListItem> listItems(String location) {

        List<WSSListItem> items = allitems.get(location);
        if (items == null) {
            if (location.startsWith("/")) {
                items = allitems.get(location.substring(1));
            }
        }
        return items;
    }

    protected void generateChildren(String basePath) {
        int depth = basePath.split("/").length;

        List<WSSListItem> items = new ArrayList<WSSListItem>();
        if (depth == 0) {
            for (int i = 0; i < 5; i++) {
                items.add(new DummyWSSListItem("DocLib" + i, "This is Dummy Document Library " + i, null));
            }
        } else {
            if (depth <= DEPTH) {
                for (int i = 0; i < 5; i++) {
                    items.add(new DummyWSSListItem("Workspace-" + depth + "-" + i, "This is Dummy Workspace " + i, basePath, null));
                }
            }
            for (int i = 0; i < 5; i++) {
                String data = "FakeContent" + i;
                //ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
                //InputStream is = DummyMemoryTree.class.getClassLoader().getResourceAsStream("sampledoc/hello.doc");
                //InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("sampledoc/hello.doc");
                DummyWSSListItem item = new DummyWSSListItem("Document" + "-" + depth + "-" + i + ".doc", "This is Dummy File " + i, basePath, null);
                item.setBinaryResourcePath("sampledoc/hello.doc");
                item.setSize(7680);
                items.add(item);
            }

            DummyWSSListItem item = new DummyWSSListItem("ShellError.txt", "Simple text file", basePath, null);
            item.setBinaryResourcePath("sampledoc/ShellError.txt");
            item.setSize(1123);
            items.add(item);

        }
        allitems.put(basePath, items);

        if (depth <= DEPTH) {
            for (WSSListItem item : items) {
                if (item.isFolderish()) {
                    generateChildren(item.getSubPath());
                }
            }
        }
    }

    protected void init() {
        root = new DummyWSSListItem("/", "Root node", null);
        generateChildren("/");
    }

}
