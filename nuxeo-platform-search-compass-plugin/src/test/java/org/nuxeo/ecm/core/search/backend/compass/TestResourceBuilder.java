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

package org.nuxeo.ecm.core.search.backend.compass;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.compass.core.CompassSession;
import org.compass.core.Property;
import org.compass.core.Resource;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedDataImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.FieldConstants;

public class TestResourceBuilder extends TestCase {

    private ResourceBuilder builder;
    private CompassSession session;
    private IntrospectableCompassBackend backend;
    private Map<String, FakeIndexableResourceDataDescriptor> dataConfs;

    @Override
    public void setUp() {
        backend = new IntrospectableCompassBackend("/testcompass.cfg.xml");
        session = backend.openSession();
        builder = new ResourceBuilder(session, "nxdoc", "nxdoc_id");
    }

   /**
     * Registers a data conf to the search service.
     *
     * Main use is in result items construction
     */
    private void addDataConf(String name, String analyzer,
            String type, boolean multiple, boolean sortable) {
        backend.getSearchService().dataConfs.put(name,
                new FakeIndexableResourceDataDescriptor(
                        name, analyzer, type, multiple, sortable));
    }

    /**
     * The marker system used for empty list must be transparent.
     * @throws Exception
     */
    public void testEmptyMarkerEscaping() throws Exception {
        try {
            builder.addProperty("barcode", Util.EMPTY_MARKER, "Keyword",
                        false, true, false, false, new HashMap<String, Serializable>(), null);
            addDataConf("barcode", null, "Keyword", false, false);

            ResultItem item = backend.buildResultItem(builder.toResource());
            assertEquals(Util.EMPTY_MARKER, item.get("barcode"));
        } finally {
            session.close();
        }
    }

    public void testaddPathProperty() throws Exception {
        try {
            builder.addPathProperty("pathprop", new Path("some/path"));
            assertEquals(Arrays.asList("some", "some/path"),
                    ResourceHelper.getListProperty(
                            builder.toResource(), "pathprop"));
        } finally {
            session.close();
        }
    }

    // driven by NXP-1412
    public void testNullPathProperty() throws Exception {
        try {
           builder.addProperty("pathprop", new String[0], "path",
                   true, true, false, false, new HashMap<String, Serializable>(), null);
        } finally {
            session.close();
        }
    }

    public void testMultiplePathProperty() throws Exception {
        try {
            builder.addProperty("pathprop", new String[0], "path",
                    true, true, true, false, new HashMap<String, Serializable>(), null);
            assertEquals(Util.EMPTY_MARKER,
                        builder.toResource().getProperty(
                                "pathprop").getStringValue());

            builder.addProperty("pathprop2", null, "path",
                    true, true, true, false, new HashMap<String, Serializable>(), null);
            assertEquals(Util.NULL_MARKER,
                        builder.toResource().getProperty(
                                "pathprop2").getStringValue());

            String[] sPaths = {"a/b", "c/d"};
            builder.addProperty("pathprop3", sPaths, "path",
                    true, true, true, false, new HashMap<String, Serializable>(), null);
            assertEquals(Arrays.asList("a", "a/b", "c", "c/d"),
                    ResourceHelper.getListProperty(
                            builder.toResource(), "pathprop3"));
        } finally {
            session.close();
        }
    }

    public void testaddStringPathProperty() throws Exception {
        try {
            Map<String, Serializable> properties = new HashMap<String, Serializable>();
            ResolvedData resolvedData = new ResolvedDataImpl(
                    "spathprop", null, "Path", "some/path",
                    true, true, false, false, null, null, false, properties);

            builder.addProperty("spathprop", resolvedData);
            assertEquals(Arrays.asList("some", "some/path"),
                    ResourceHelper.getListProperty(
                            builder.toResource(), "spathprop"));

            // Now with another separator
            properties.put(FieldConstants.PROPERTY_PATH_SEPARATOR, "|");
            resolvedData = new ResolvedDataImpl(
                    "pipe", null, "Path", "s/ome|path|ag/ain",
                    true, true, false, false, null, null, false,
                    properties);

            builder.addProperty("pipe", resolvedData);
            assertEquals(Arrays.asList("s/ome", "s/ome|path", "s/ome|path|ag/ain"),
                    ResourceHelper.getListProperty(
                            builder.toResource(), "pipe"));

        } finally {
            session.close();
        }
    }

    /**
     *  Test for NXP-1540. It is vital here to use a property name that has the
     *  correct converter. Otherwise the issue wasn't reproduced
     */
    public void testMultipleDate() throws Exception {
        try {
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(2007, 0, 1, 0, 0, 0);
            ResolvedData resolvedData = new ResolvedDataImpl(
                    "bk:published", null, "date", Arrays.asList(cal, cal),
                    true, true, true, false, null, null, false, null);
            builder.addProperty("bk:published", resolvedData);
            String scal = "2007-01-01-00-00-00-0-AM";
            assertEquals(Arrays.asList(scal, scal),
                    ResourceHelper.getListProperty(
                            builder.toResource(), "bk:published"));

        } finally {
            session.close();
        }
    }

    public void testaddSecurityProperty() throws Exception {
        ACP acp = new ACPImpl();

        ACL acl = new ACLImpl();
        acl.add(new ACE("dupont", "A", true));
        acl.add(new ACE("hugo", "C", false));
        acl.add(new ACE("authors", "D", true));
        acp.addACL(0, acl);

        acl = new ACLImpl();
        acl.add(new ACE("accountants", "B", true));
        acl.add(new ACE("employees", "E", true));
        acp.addACL(1, acl);

        // TODO this is far too naive !!
        try {
            builder.addSecurityProperty("s1", Arrays.asList("A", "B"), acp);
            assertEquals(Arrays.asList("+dupont#A", "+accountants#B"),
                    ResourceHelper.getListProperty(builder.toResource(), "s1"));

            builder.addSecurityProperty("s2", Arrays.asList("C", "E"), acp);
            assertEquals(Arrays.asList("-hugo#C", "+employees#E"),
                    ResourceHelper.getListProperty(builder.toResource(), "s2"));

            builder.addSecurityProperty("s3", Arrays.asList("A", "E"), acp);
            assertEquals(Arrays.asList("+dupont#A", "+employees#E"),
                    ResourceHelper.getListProperty(builder.toResource(), "s3"));
        } finally {
            session.close();
        }
    }

    public void testSortableTextField() throws Exception {
        try {
            String textValue = "Here is the text";
            builder.addProperty("prop", textValue, "text",
                    true, false, false, true, new HashMap<String, Serializable>(), null);
            Resource r = builder.toResource();
            Property prop = r.getProperty("prop" + Util.SORTABLE_FIELD_SUFFIX);
            assertNotNull(prop);
            assertFalse(prop.isTokenized());
            assertEquals(textValue, prop.getStringValue());

            prop = r.getProperty("prop");
            assertNotNull(prop);
            assertTrue(prop.isTokenized());
            assertEquals(textValue, prop.getStringValue());

        } finally {
            session.close();
        }

    }

    // Now with a property that has defined in compass mappings file
    // Jira:NXP-1313
    public void testSortableTextFieldInMappings() throws Exception {
        try {
            String textValue = "Here is the text";
            builder.addProperty("bk:frenchtitle", textValue, "text",
                    true, false, false, true, new HashMap<String, Serializable>(), null);
            Resource r = builder.toResource();
            Property prop = r.getProperty("bk:frenchtitle"
                        + Util.SORTABLE_FIELD_SUFFIX);
            assertNotNull(prop);
            assertFalse(prop.isTokenized());
            assertEquals(textValue, prop.getStringValue());

        } finally {
            session.close();
        }

    }
}
