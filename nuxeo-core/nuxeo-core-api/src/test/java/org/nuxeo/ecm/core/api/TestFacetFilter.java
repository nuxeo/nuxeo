/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
public class TestFacetFilter {

    @Test
    public void test() {
        Set<String> facets = new HashSet<String>();
        facets.add("A");
        facets.add("B");
        facets.add("C");
        DocumentModel model = new DocumentModelImpl("sid", "my type", "id", null, null, null, null, new String[0],
                facets, null, null);

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

        f1 = new FacetFilter(Collections.<String> emptyList(), Collections.<String> emptyList());
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

    protected void checkAnd(FacetFilter f1, FacetFilter f2, Set<String> req, Set<String> exc, Boolean sc) {
        FacetFilter f = new FacetFilter(f1, f2);
        assertEquals(req, f.required);
        assertEquals(exc, f.excluded);
        assertEquals(sc, f.shortcut);
    }

}
