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

package org.nuxeo.ecm.platform.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TypeTest extends NXRuntimeTestCase {
    TypeService typeService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "types-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-bundle.xml");
        typeService = (TypeService) runtime.getComponent(TypeService.ID);
    }

    @Test
    public void testTypesExtensionPoint() {
        Collection<Type> types = typeService.getTypeRegistry().getTypes();
        assertEquals(5, types.size());

        Type type = typeService.getTypeRegistry().getType("MyDocType");
        assertEquals("MyDocType", type.getId());
        assertEquals("type icon", type.getIcon());
        assertEquals("type label", type.getLabel());

        String[] actions = type.getActions();
        assertEquals(3, actions.length);
        assertEquals("action_id1", actions[0]);
        assertEquals("action_id2", actions[1]);
        assertEquals("action_id3", actions[2]);

        String[] anyLayouts = type.getLayouts(BuiltinModes.ANY);
        assertEquals(1, anyLayouts.length);
        assertEquals("dublincore", anyLayouts[0]);
        String[] createLayouts = type.getLayouts(BuiltinModes.CREATE);
        assertEquals(2, createLayouts.length);
        assertEquals("dublincore", createLayouts[0]);
        assertEquals("file", createLayouts[1]);

        String[] cv = type.getContentViews("default");
        assertNotNull(cv);
        assertEquals(2, cv.length);
        assertEquals("cv_1", cv[0]);
        assertEquals("cv_2", cv[1]);
        cv = type.getContentViews("other");
        assertNotNull(cv);
        assertEquals(1, cv.length);
        assertEquals("cv_3", cv[0]);
        cv = type.getContentViews("foo");
        assertNull(cv);
    }

    @Test
    public void testTypeViews() {
        Type type = typeService.getTypeRegistry().getType("MyDocType");
        assertNotNull(type);

        assertEquals("default_view", type.getDefaultView());
        assertEquals("create_view", type.getCreateView());
        assertEquals("edit_view", type.getEditView());
        assertEquals("edit_detail_view", type.getView("edit_detail").getValue());
        assertEquals("metadata_view", type.getView("metadata").getValue());
    }

    @Test
    public void testAllowedSubTypes() {
        Type type = typeService.getTypeRegistry().getType("MyDocType");
        Map<String, SubType> allowed = type.getAllowedSubTypes();
        assertEquals(2, allowed.size());
        assertTrue(allowed.containsKey("MyOtherDocType"));
        assertTrue(allowed.containsKey("MyHiddenDocType"));
        SubType myHiddenDocType = allowed.get("MyHiddenDocType");
        List<String> hidden = myHiddenDocType.getHidden();
        assertEquals(2, hidden.size());
        assertTrue(hidden.contains("create"));
        assertTrue(hidden.contains("edit"));
    }

    @Test
    public void testDeploymentOverride() throws Exception {
        Collection<Type> types = typeService.getTypeRegistry().getTypes();
        assertEquals(5, types.size());

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        // One removed
        types = typeService.getTypeRegistry().getTypes();
        assertEquals(4, types.size());

        // The Other changed
        Type type = typeService.getTypeRegistry().getType("MyDocType");
        assertNotNull(type);

        assertEquals("MyDocType", type.getId());
        assertEquals("type icon 2", type.getIcon());
        assertEquals("type label 2", type.getLabel());

        assertEquals("default_view2", type.getDefaultView());
        assertEquals("create_view2", type.getCreateView());
        assertEquals("edit_view2", type.getEditView());

        Map<String, SubType> allowed = type.getAllowedSubTypes();
        assertEquals(2, allowed.size());
        assertTrue(allowed.containsKey("MyOtherDocType2"));
        assertTrue(allowed.containsKey("MyHiddenDocType"));

        SubType subType = allowed.get("MyHiddenDocType");
        List<String> hidden = subType.getHidden();
        assertEquals(0, hidden.size());

        // layout override done
        Map<String, Layouts> layouts = type.getLayouts();
        assertEquals(2, layouts.size());
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(0,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        // Override not ready but test that nothing's changed
        String[] actions = type.getActions();
        assertEquals(3, actions.length);
        TypeView[] views = type.getViews();
        assertEquals(2, views.length);

        // content views override
        String[] cv = type.getContentViews("default");
        assertNotNull(cv);
        assertEquals(3, cv.length);
        assertEquals("cv_1", cv[0]);
        assertEquals("cv_2", cv[1]);
        assertEquals("cv_3", cv[2]);
        cv = type.getContentViews("other");
        assertNotNull(cv);
        assertEquals(2, cv.length);
        assertEquals("cv_4", cv[0]);
        assertEquals("cv_5", cv[1]);
        cv = type.getContentViews("foo");
        assertNull(cv);

        Map<String, DocumentContentViews> allCvs = type.getContentViews();
        assertEquals(2, allCvs.size());
        DocumentContentViews defaultCvs = allCvs.get("default");
        assertNotNull(defaultCvs);
        DocumentContentView[] cvs = defaultCvs.getContentViews();
        assertEquals(3, cvs.length);
        assertEquals("cv_1", cvs[0].getContentViewName());
        assertFalse(cvs[0].getShowInExportView());
        assertEquals("cv_2", cvs[1].getContentViewName());
        assertTrue(cvs[1].getShowInExportView());
        assertEquals("cv_3", cvs[2].getContentViewName());
        assertTrue(cvs[2].getShowInExportView());
    }

    @Test
    public void testLayoutOverride() throws Exception {
        Type type = typeService.getTypeRegistry().getType("DocTypeWithLayout");
        assertEquals("doc type with layout", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        // Test layout is left unchanged
        type = typeService.getTypeRegistry().getType("DocTypeWithLayout");
        assertEquals("overridden doc type, but layout left unchanged",
                type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
    }

    @Test
    public void testLayoutOverrideWithAppend() throws Exception {
        Type type = typeService.getTypeRegistry().getType(
                "DocTypeTestLayoutOverride");
        assertEquals("doc type with layout to override", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        type = typeService.getTypeRegistry().getType(
                "DocTypeTestLayoutOverride");
        // Test layout is left unchanged
        assertEquals(2, type.getLayouts().size());
        assertEquals(2,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
    }

    @Test
    public void testHotReload() throws Exception {
        Type type = typeService.getTypeRegistry().getType(
                "DocTypeTestLayoutOverride");
        assertEquals("doc type with layout to override", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
        // check the one to be removed is there
        Type typeToBeRemoved = typeService.getTypeRegistry().getType(
                "MyOtherDocType");
        assertNotNull(typeToBeRemoved);
        assertEquals("initial alternative doc type", typeToBeRemoved.getLabel());
        assertEquals("initial icon", typeToBeRemoved.getIcon());

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        type = typeService.getTypeRegistry().getType(
                "DocTypeTestLayoutOverride");
        // Test layout is left unchanged
        assertEquals(2, type.getLayouts().size());
        assertEquals(2,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
        // check the one to be removed is not there
        typeToBeRemoved = typeService.getTypeRegistry().getType(
                "MyOtherDocType");
        assertNull(typeToBeRemoved);

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-remove-bundle.xml");

        // check the one to be removed is back there again and has not been
        // merged with the contribution before removal
        typeToBeRemoved = typeService.getTypeRegistry().getType(
                "MyOtherDocType");
        assertNotNull(typeToBeRemoved);
        assertEquals("Resurrected doc type", typeToBeRemoved.getLabel());
        assertNull(typeToBeRemoved.getIcon());

        undeployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-remove-bundle.xml");
        // check the one to be removed is not there
        typeToBeRemoved = typeService.getTypeRegistry().getType(
                "MyOtherDocType");
        assertNull(typeToBeRemoved);

        undeployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        type = typeService.getTypeRegistry().getType(
                "DocTypeTestLayoutOverride");
        assertEquals("doc type with layout to override", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1,
                type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2,
                type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
        // check the one to be removed is back there again and again and again
        typeToBeRemoved = typeService.getTypeRegistry().getType(
                "MyOtherDocType");
        assertNotNull(typeToBeRemoved);
        assertEquals("initial alternative doc type", typeToBeRemoved.getLabel());
        assertEquals("initial icon", typeToBeRemoved.getIcon());
    }

}
