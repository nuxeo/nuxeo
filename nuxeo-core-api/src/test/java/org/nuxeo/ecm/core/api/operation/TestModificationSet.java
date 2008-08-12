package org.nuxeo.ecm.core.api.operation;

import java.util.Iterator;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.IdRef;

public class TestModificationSet extends TestCase {

    ModificationSet set;

    @Override
    protected void setUp() throws Exception {
        set = new ModificationSet();
    }

    public void test() {
        set.add(new IdRef("a"), Modification.CREATE);
        set.add(new IdRef("b"), Modification.REMOVE);
        set.add(new IdRef("c"), Modification.CONTENT);
        set.add(new IdRef("e"), Modification.SECURITY);
        set.add(new IdRef("d"), Modification.STATE);

        assertTrue(set.getModification(new IdRef("a")).isCreate());
        assertTrue(set.getModification(new IdRef("b")).isRemove());
        assertTrue(set.getModification(new IdRef("c")).isUpdateModification());
        assertTrue(set.getModification(new IdRef("e")).isSecurityUpdate());
        assertTrue(set.getModification(new IdRef("d")).isStateUpdate());

        assertFalse(set.getModification(new IdRef("a")).isRemove());
        assertFalse(set.getModification(new IdRef("b")).isCreate());

        Iterator<Modification> it = set.iterator();
        assertEquals("a [8]", it.next().toString());
        assertEquals("b [16]", it.next().toString());
        assertEquals("c [64]", it.next().toString());
        assertEquals("e [128]", it.next().toString());
        assertEquals("d [256]", it.next().toString());
        assertFalse(it.hasNext());

        set.add(new IdRef("c"), Modification.STATE | Modification.SECURITY);
        Iterator<Modification> it2 = set.iterator();
        assertEquals("a [8]", it2.next().toString());
        assertEquals("b [16]", it2.next().toString());
        assertEquals("c [448]", it2.next().toString());
        assertEquals("e [128]", it2.next().toString());
        assertEquals("d [256]", it2.next().toString());
        assertFalse(it2.hasNext());

        set.remove(2);
        Iterator<Modification> it3 = set.iterator();
        assertEquals("a [8]", it3.next().toString());
        assertEquals("b [16]", it3.next().toString());
        assertEquals("e [128]", it3.next().toString());
        assertEquals("d [256]", it3.next().toString());
        assertFalse(it3.hasNext());
    }

}
