/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.CowList;
import org.nuxeo.ecm.core.storage.CowMap;

public class TestCow {

    @Test
    public void test() {
        List<Serializable> list = new ArrayList<Serializable>();
        list.add("a");
        list.add("b");

        CowList cl1 = new CowList(list);

        assertEquals(2, cl1.size());
        assertEquals("a", cl1.get(0));
        assertEquals("b", cl1.get(1));

        list.add("c");

        assertEquals(2, cl1.size());

        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("1", "111");
        map.put("list", (Serializable) list);

        CowMap cm1 = new CowMap(map);

        assertEquals(2, cm1.size());
        assertEquals("111", cm1.get("1"));
        assertEquals(3, ((List<?>) cm1.get("list")).size());

        map.put("3", "333");

        assertEquals(2, cm1.size());

        list.add("d");

        assertEquals(3, ((List<?>) cm1.get("list")).size());
    }

}
