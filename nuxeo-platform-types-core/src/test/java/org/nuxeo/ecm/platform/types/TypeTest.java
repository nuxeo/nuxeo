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

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TypeTest extends NXRuntimeTestCase {
    TypeService typeService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "types-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-bundle.xml");
        typeService = (TypeService) runtime.getComponent(TypeService.ID);
    }

    public void testTypesExtensionPoint() {
        Collection<Type> types = typeService.getTypeRegistry().getTypes();
        assertEquals(4, types.size());

        Type type = typeService.getTypeRegistry().getType("MyDocType");
        assertEquals("MyDocType", type.getId());
        assertEquals("type icon", type.getIcon());
        assertEquals("type label", type.getLabel());

        String[] actions = type.getActions();
        assertEquals(3, actions.length);
        assertEquals("action_id1", actions[0]);
        assertEquals("action_id2", actions[1]);
        assertEquals("action_id3", actions[2]);

        // old layouts for BBB
        FieldWidget[] layout = type.getLayout();
        assertEquals(3, layout.length);
        assertEquals("jsf1", layout[0].getJsfComponent());
        assertEquals("schema1", layout[0].getSchemaName());
        assertEquals("name1", layout[0].getFieldName());

        assertEquals("jsf2", layout[1].getJsfComponent());
        assertEquals("schema2", layout[1].getSchemaName());
        assertEquals("name2", layout[1].getFieldName());

        assertEquals("jsf3", layout[2].getJsfComponent());
        assertEquals("schema3", layout[2].getSchemaName());
        assertEquals("name3", layout[2].getFieldName());

        // new layouts
        String[] anyLayouts = type.getLayouts(BuiltinModes.ANY);
        assertEquals(1, anyLayouts.length);
        assertEquals("dublincore", anyLayouts[0]);
        String[] createLayouts = type.getLayouts(BuiltinModes.CREATE);
        assertEquals(2, createLayouts.length);
        assertEquals("dublincore", createLayouts[0]);
        assertEquals("file", createLayouts[1]);
    }

    public void testTypeViews() {
        Type type = typeService.getTypeRegistry().getType("MyDocType");
        assertNotNull(type);

        assertEquals("default_view", type.getDefaultView());
        assertEquals("create_view", type.getCreateView());
        assertEquals("edit_view", type.getEditView());
        assertEquals("edit_detail_view", type.getView("edit_detail").getValue());
        assertEquals("metadata_view", type.getView("metadata").getValue());
    }

    public void testAllowedSubTypes() {
        Type type = typeService.getTypeRegistry().getType("MyDocType");
        String[] allowed = type.getAllowedSubTypes();
        assertEquals(1, allowed.length);
        assertEquals("MyOtherDocType", allowed[0]);
    }

    public void testDefaultLayoutExtensionPoint() {
        Map<String, String> map = typeService.getTypeWidgetRegistry().getMap();
        assertEquals(2, map.size());
        assertEquals("def_jsf1", map.get("java.lang.String"));
        assertEquals("def_jsf2", map.get("java.lang.Double"));
        assertNull(map.get("xxx"));
    }

    public void testDeploymentOverride() throws Exception {
        Collection<Type> types = typeService.getTypeRegistry().getTypes();
        assertEquals(4, types.size());

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        // One removed
        types = typeService.getTypeRegistry().getTypes();
        assertEquals(3, types.size());

        // The Other changed
        Type type = typeService.getTypeRegistry().getType("MyDocType");
        assertNotNull(type);

        assertEquals("MyDocType", type.getId());
        assertEquals("type icon 2", type.getIcon());
        assertEquals("type label 2", type.getLabel());

        assertEquals("default_view2", type.getDefaultView());
        assertEquals("create_view2", type.getCreateView());
        assertEquals("edit_view2", type.getEditView());

        String[] allowed = type.getAllowedSubTypes();
        assertEquals(1, allowed.length);
        assertEquals("MyOtherDocType2", allowed[0]);

        // old layout override done
        FieldWidget[] layout = type.getLayout();
        assertEquals(2, layout.length);

        // new layout override done
        Map<String, Layouts> layouts = type.getLayouts();
        assertEquals(2, layouts.size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(0, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        // Override not ready but test that nothing's changed
        String[] actions = type.getActions();
        assertEquals(3, actions.length);
        TypeView[] views = type.getViews();
        assertEquals(2, views.length);
    }

    public void testLayoutOverride() throws Exception {
        Type type = typeService.getTypeRegistry().getType("DocTypeWithLayout");
        assertEquals("doc type with layout", type.getLabel());
        assertEquals(2, type.getLayout().length);
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        // Test layout is left unchanged
        type = typeService.getTypeRegistry().getType("DocTypeWithLayout");
        assertEquals("overridden doc type, but layout left unchanged",
                type.getLabel());
        assertEquals(2, type.getLayout().length);
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
    }

    public void testLayoutOverrideWithAppend() throws Exception {
        Type type = typeService.getTypeRegistry().getType("DocTypeTestLayoutOverride");
        assertEquals("doc type with layout to override", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        deployContrib("org.nuxeo.ecm.platform.types.core.tests",
                "test-types-override-bundle.xml");

        type = typeService.getTypeRegistry().getType("DocTypeTestLayoutOverride");
        // Test layout is left unchanged
        assertEquals(2, type.getLayouts().size());
        assertEquals(2, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(1, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
    }


}
