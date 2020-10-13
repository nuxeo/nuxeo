/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.types:test-core-types-bundle.xml")
@Deploy("org.nuxeo.ecm.platform.types:test-types-bundle.xml")
public class TypeTest {

    @Inject
    public SchemaManager schemaManager;

    @Inject
    protected HotDeployer hotDeployer;

    private TypeService getTypeService() {
        return (TypeService) Framework.getService(TypeManager.class);
    }

    @Test
    public void testTypesExtensionPoint() {
        Collection<Type> types = getTypeService().getTypeRegistry().getTypes();
        assertEquals(6, types.size());

        Type type = getTypeService().getTypeRegistry().getType("MyDocType");
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
        Type type = getTypeService().getTypeRegistry().getType("MyDocType");
        assertNotNull(type);

        assertEquals("default_view", type.getDefaultView());
        assertEquals("create_view", type.getCreateView());
        assertEquals("edit_view", type.getEditView());
        assertEquals("edit_detail_view", type.getView("edit_detail").getValue());
        assertEquals("metadata_view", type.getView("metadata").getValue());
    }

    @Test
    public void testAllowedSubTypes() {
        Type type = getTypeService().getTypeRegistry().getType("MyDocType");
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
        Collection<Type> types = getTypeService().getTypeRegistry().getTypes();
        assertEquals(6, types.size());

        hotDeployer.deploy("org.nuxeo.ecm.platform.types:test-types-override-bundle.xml");

        // One removed
        types = getTypeService().getTypeRegistry().getTypes();
        assertEquals(6, types.size());

        // The Other changed
        Type type = getTypeService().getTypeRegistry().getType("MyDocType");
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
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(0, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

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
        Type type = getTypeService().getTypeRegistry().getType("DocTypeWithLayout");
        assertEquals("doc type with layout", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        hotDeployer.deploy("org.nuxeo.ecm.platform.types:test-types-override-bundle.xml");

        // Test layout is left unchanged
        type = getTypeService().getTypeRegistry().getType("DocTypeWithLayout");
        assertEquals("overridden doc type, but layout left unchanged", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
    }

    @Test
    public void testLayoutOverrideWithAppend() throws Exception {
        Type type = getTypeService().getTypeRegistry().getType("DocTypeTestLayoutOverride");
        assertEquals("doc type with layout to override", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);

        hotDeployer.deploy("org.nuxeo.ecm.platform.types:test-types-override-bundle.xml");

        type = getTypeService().getTypeRegistry().getType("DocTypeTestLayoutOverride");
        // Test layout is left unchanged
        assertEquals(2, type.getLayouts().size());
        assertEquals(2, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(1, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
    }

    @Test
    public void testHotReload() throws Exception {
        Type type = getTypeService().getTypeRegistry().getType("DocTypeTestLayoutOverride");
        assertEquals("doc type with layout to override", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
        // check the one to be removed is there
        Type typeToBeRemoved = getTypeService().getTypeRegistry().getType("MyOtherDocType");
        assertNotNull(typeToBeRemoved);
        assertEquals("initial alternative doc type", typeToBeRemoved.getLabel());
        assertEquals("initial icon", typeToBeRemoved.getIcon());

        hotDeployer.deploy("org.nuxeo.ecm.platform.types:test-types-override-bundle.xml");

        type = getTypeService().getTypeRegistry().getType("DocTypeTestLayoutOverride");
        // Test layout is left unchanged
        assertEquals(2, type.getLayouts().size());
        assertEquals(2, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(1, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
        // check the one to be removed is not there
        typeToBeRemoved = getTypeService().getTypeRegistry().getType("MyOtherDocType");
        assertNull(typeToBeRemoved);

        hotDeployer.deploy("org.nuxeo.ecm.platform.types:test-types-override-remove-bundle.xml");

        // check the one to be removed is back there again and has not been
        // merged with the contribution before removal
        typeToBeRemoved = getTypeService().getTypeRegistry().getType("MyOtherDocType");
        assertNotNull(typeToBeRemoved);
        assertEquals("Resurrected doc type", typeToBeRemoved.getLabel());
        assertNull(typeToBeRemoved.getIcon());

        hotDeployer.undeploy("org.nuxeo.ecm.platform.types:test-types-override-remove-bundle.xml");

        // check the one to be removed is not there
        typeToBeRemoved = getTypeService().getTypeRegistry().getType("MyOtherDocType");
        assertNull(typeToBeRemoved);

        // undeploy org.nuxeo.ecm.platform.types.core.tests:test-types-override-bundle.xml contribution
        hotDeployer.undeploy("org.nuxeo.ecm.platform.types:test-types-override-bundle.xml");

        type = getTypeService().getTypeRegistry().getType("DocTypeTestLayoutOverride");
        assertEquals("doc type with layout to override", type.getLabel());
        assertEquals(2, type.getLayouts().size());
        assertEquals(1, type.getLayouts().get(BuiltinModes.ANY).getLayouts().length);
        assertEquals(2, type.getLayouts().get(BuiltinModes.CREATE).getLayouts().length);
        // check the one to be removed is back there again and again and again
        typeToBeRemoved = getTypeService().getTypeRegistry().getType("MyOtherDocType");
        assertNotNull(typeToBeRemoved);
        assertEquals("initial alternative doc type", typeToBeRemoved.getLabel());
        assertEquals("initial icon", typeToBeRemoved.getIcon());
    }

    @Test
    public void testCoreSubTypesWithHotReload() throws Exception {
        Collection<String> testMyDocTypeSubtypes1 = Arrays.asList("MyOtherDocType", "MyHiddenDocType");
        Collection<String> testMyDocTypeSubtypes2 = Arrays.asList("MyOtherDocType2", "MyHiddenDocType");
        Collection<String> testMyDocType2Subtypes1 = Arrays.asList("MyDocType", "MyOtherDocType", "MyHiddenDocType");
        Collection<String> testMyDocType2Subtypes2 = Arrays.asList("MyDocType", "MyOtherDocType");
        Collection<String> testMyDocType2Subtypes3 = Arrays.asList("MyDocType");
        Collection<String> testSchemas = Arrays.asList("schema1", "schema2");
        Collection<String> testFacets = Arrays.asList("myFacet", "facet1", "facet2");

        assertSubtypes("MyDocType", testMyDocTypeSubtypes1);
        assertSubtypes("MyDocType2", testMyDocType2Subtypes1);
        assertFacetsAndSchemas("MyDocType", testFacets, testSchemas);

        // deploy ecm contribution to override types
        hotDeployer.deploy("org.nuxeo.ecm.platform.types:test-types-override-bundle.xml");

        assertSubtypes("MyDocType", testMyDocTypeSubtypes2);
        // subtypes differ for MyDocType2 because ecm override contrib removed MyOtherDocType from getTypeService()
        assertSubtypes("MyDocType2", testMyDocType2Subtypes1, Arrays.asList("MyDocType", "MyHiddenDocType"));
        assertFacetsAndSchemas("MyDocType", testFacets, testSchemas);

        // deploy core contribution to override types
        hotDeployer.deploy("org.nuxeo.ecm.platform.types:test-core-types-override-bundle.xml");

        assertSubtypes("MyDocType", testMyDocTypeSubtypes2);
        // subtypes differ for MyDocType2 because ecm override contrib removed MyOtherDocType from getTypeService()
        assertSubtypes("MyDocType2", testMyDocType2Subtypes2, testMyDocType2Subtypes3);
        assertFacetsAndSchemas("MyDocType", testFacets, testSchemas);

        // undeploy org.nuxeo.ecm.platform.types.core.tests:test-types-override-bundle.xml - first one deployed
        hotDeployer.undeploy("org.nuxeo.ecm.platform.types:test-types-override-bundle.xml");

        assertSubtypes("MyDocType", testMyDocTypeSubtypes1);
        assertSubtypes("MyDocType2", testMyDocType2Subtypes2);
        assertFacetsAndSchemas("MyDocType", testFacets, testSchemas);

        // undeploy org.nuxeo.ecm.platform.types.core.tests:test-core-types-override-bundle.xml - last one deployed
        hotDeployer.undeploy("org.nuxeo.ecm.platform.types:test-core-types-override-bundle.xml");

        assertSubtypes("MyDocType", testMyDocTypeSubtypes1);
        assertSubtypes("MyDocType2", testMyDocType2Subtypes1);
        assertFacetsAndSchemas("MyDocType", testFacets, testSchemas);

        // undeploy original ecm contribution to override types
        hotDeployer.undeploy("org.nuxeo.ecm.platform.types:test-types-bundle.xml");

        assertSubtypes("MyDocType", Collections.emptyList());
        assertSubtypes("MyDocType2", testMyDocType2Subtypes3, Collections.emptyList());
        assertFacetsAndSchemas("MyDocType", testFacets, testSchemas);
    }

    protected Collection<String> getEcmSubtypes(String type) {
        Collection<Type> ecmSubtypes = getTypeService().getAllowedSubTypes(type);
        Set<String> result = new HashSet<>();
        for (Type t : ecmSubtypes) {
            result.add(t.id);
        }
        return result;
    }

    protected void assertSubtypes(String docType, Collection<String> subtypes) {
        assertSubtypes(docType, subtypes, subtypes);
    }

    protected void assertSubtypes(String docType, Collection<String> subtypesCore, Collection<String> subtypesEcm) {
        Collection<String> coreSubtypes = schemaManager.getAllowedSubTypes(docType);
        assertNotNull(coreSubtypes);
        assertEquals(subtypesCore.size(), coreSubtypes.size());
        assertTrue(subtypesCore.containsAll(coreSubtypes));
        Collection<String> ecmSubtypes = getEcmSubtypes(docType);
        assertEquals(subtypesEcm.size(), ecmSubtypes.size());
        assertTrue(ecmSubtypes.containsAll(subtypesEcm));
    }

    protected void assertFacetsAndSchemas(String docType, Collection<String> facets, Collection<String> schemas) {
        Collection<String> currentFacets = schemaManager.getDocumentType(docType).getFacets();
        assertNotNull(currentFacets);
        assertEquals(facets.size(), currentFacets.size());
        assertTrue(facets.containsAll(currentFacets));

        Collection<String> currentSchemas = Arrays.asList(schemaManager.getDocumentType(docType).getSchemaNames());
        assertNotNull(currentSchemas);
        assertEquals(schemas.size(), currentSchemas.size());
        assertTrue(schemas.containsAll(currentSchemas));
    }

}
