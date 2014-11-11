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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;


public class TestFacetFilter extends TestCase {

    public void test() {
        Set<String> facets = new HashSet<String>();
        facets.add("A");
        facets.add("B");
        facets.add("C");
        DocumentModel model = new DocumentModelImpl("sid", "my type", "id",
                null, "lock", null, null, null, facets, null, null);

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
}
