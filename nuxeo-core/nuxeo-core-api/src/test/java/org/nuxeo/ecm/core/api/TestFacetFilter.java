/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestFacetFilter extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
    }

    @Test
    public void test() {
        Set<String> facets = new HashSet<String>();
        facets.add("A");
        facets.add("B");
        facets.add("C");
        DocumentModel model = new DocumentModelImpl("sid", "my type", "id",
                null, null, null, null, null, facets, null, null);

        // One facet statements
        assertTrue(new FacetFilter("A", true).accept(model));
        assertTrue(new FacetFilter("B", true).accept(model));
        assertFalse(new FacetFilter("D", true).accept(model));

        assertFalse(new FacetFilter("A", false).accept(model));
        assertFalse(new FacetFilter("B", false).accept(model));
        assertTrue(new FacetFilter("D", false).accept(model));

        // More complicated statements
        List<String> required = Arrays.asList("A", "B");
        assertTrue(new FacetFilter(required, null).accept(model));
        List<String> excluded = Arrays.asList("C");
        assertFalse(new FacetFilter(required, excluded).accept(model));
        required = Arrays.asList("A", "D");
        excluded = Arrays.asList("E");
        assertFalse(new FacetFilter(required, excluded).accept(model));
        required = Arrays.asList("A", "C");
        assertTrue(new FacetFilter(required, excluded).accept(model));
        excluded = Arrays.asList("B", "E");
        assertFalse(new FacetFilter(required, excluded).accept(model));
    }

    @Test
    public void testAnd() {
        FacetFilter f1, f2;

        f1 = new FacetFilter(Collections.<String> emptyList(),
                Collections.<String> emptyList());
        assertEquals(Boolean.TRUE, f1.shortcut);

        f1 = new FacetFilter(Arrays.asList("B"), Arrays.asList("B"));
        assertEquals(Boolean.FALSE, f1.shortcut);

        f1 = new FacetFilter("A", true);
        f2 = new FacetFilter("B", true);
        checkAnd(f1, f2, set("A", "B"), set(), null);

        f1 = new FacetFilter("A", true);
        f2 = new FacetFilter("B", false);
        checkAnd(f1, f2, set("A"), set("B"), null);

        f1 = new FacetFilter("A", false);
        f2 = new FacetFilter("B", true);
        checkAnd(f1, f2, set("B"), set("A"), null);

        f1 = new FacetFilter("A", false);
        f2 = new FacetFilter("B", false);
        checkAnd(f1, f2, set(), set("A", "B"), null);

        f1 = new FacetFilter(Arrays.asList("A"), Arrays.asList("B"));
        f2 = new FacetFilter(Arrays.asList("C"), Arrays.asList("D"));
        checkAnd(f1, f2, set("A", "C"), set("B", "D"), null);

        f1 = new FacetFilter(Arrays.asList("A"), Arrays.asList("B"));
        f2 = new FacetFilter(Arrays.asList("C"), Arrays.asList("A"));
        checkAnd(f1, f2, set("A", "C"), set("B", "A"), Boolean.FALSE);

    }

    protected static Set<String> set(String... strings) {
        return new HashSet<String>(Arrays.asList(strings));
    }

    protected void checkAnd(FacetFilter f1, FacetFilter f2, Set<String> req,
            Set<String> exc, Boolean sc) {
        FacetFilter f = new FacetFilter(f1, f2);
        assertEquals(req, f.required);
        assertEquals(exc, f.excluded);
        assertEquals(sc, f.shortcut);
    }

}
