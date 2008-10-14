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
