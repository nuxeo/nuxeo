/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.service.ComplexItemsDescriptor;
import org.nuxeo.ecm.diff.service.DocumentDiffDisplayService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests DocumentDiffDisplayService.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.diff:OSGI-INF/document-diff-display-service.xml",
        "org.nuxeo.diff.test:OSGI-INF/test-document-diff-display-contrib.xml" })
public class TestDocumentDiffDisplayService extends TestCase {

    @Inject
    protected DocumentDiffDisplayService docDiffDisplayService;

    /**
     * Test document diff display contrib.
     * 
     * @throws Exception the exception
     */
    @Test
    public void testDocumentDiffDisplayService() {

        // Check service
        assertNotNull(docDiffDisplayService);

        // Check contribs
        Map<String, ComplexItemsDescriptor> contribs = docDiffDisplayService.getContributions();
        assertNotNull(contribs);
        assertEquals(1, contribs.size());

        // Check a specific contrib
        List<String> complexItems = docDiffDisplayService.getComplexItems(
                "complextypes", "complex");
        List<String> expectedComplexItems = new ArrayList<String>();
        expectedComplexItems.add("integerItem");
        expectedComplexItems.add("dateItem");
        expectedComplexItems.add("stringItem");

        assertEquals(expectedComplexItems, complexItems);

        // Check that order is taken into account
        expectedComplexItems.remove("integerItem");
        expectedComplexItems.add("integerItem");

        assertFalse(expectedComplexItems.equals(complexItems));
    }

    /**
     * Test apply complex items order.
     */
    @Test
    public void testApplyComplexItemsOrder() {

        List<String> complexItems = new ArrayList<String>();
        complexItems.add("stringItem");
        complexItems.add("booleanItem");
        complexItems.add("integerItem");
        complexItems.add("dateItem");

        docDiffDisplayService.applyComplexItemsOrder("complextypes", "complex",
                complexItems);

        List<String> expectedComplexItems = new ArrayList<String>();
        expectedComplexItems.add("integerItem");
        expectedComplexItems.add("dateItem");
        expectedComplexItems.add("stringItem");
        expectedComplexItems.add("booleanItem");

        assertEquals(expectedComplexItems, complexItems);
    }

}
