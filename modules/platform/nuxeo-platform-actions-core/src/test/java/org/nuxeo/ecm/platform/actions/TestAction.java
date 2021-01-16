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
package org.nuxeo.ecm.platform.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.actions:OSGI-INF/actions-framework.xml")
@Deploy("org.nuxeo.ecm.actions.tests:test-actions-contrib.xml")
public class TestAction {

    protected static final String OVERRIDE_CONTRIB = "org.nuxeo.ecm.actions.tests:test-actions-override-contrib.xml";

    @Inject
    protected HotDeployer hotDeployer;

    public ActionManager getService() {
        return Framework.getService(ActionManager.class);
    }

    protected MapRegistry getRegistry(String point) {
        return (MapRegistry) Framework.getRuntime()
                                      .getComponentManager()
                                      .getExtensionPointRegistry(ActionService.ID.getName(), point)
                                      .orElseThrow(() -> new IllegalArgumentException(
                                              String.format("Unknown registry '%s'", point)));
    }

    @Test
    public void testActionRegistry() {
        List<ActionDescriptor> actions = getRegistry(ActionService.ACTIONS_XP).getContributionValues();
        assertEquals(
                List.of("newDocument", "logout", "TAB_VIEW", "TAB_WITH_LOCAL_FILTER", "TAB_WITH_GLOBAL_FILTER",
                        "TAB_WITH_LOCAL_FILTER_MERGED", "actionTestProperties", "singleActionRetrievedWithFilter"),
                actions.stream().map(ActionDescriptor::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFilterRegistry() {
        List<ActionFilter> filters = getRegistry(ActionService.FILTERS_XP).getContributionValues();
        assertEquals(
                List.of("createChild", "local_filter", "local_filter_merged", "theFilter", "filter_defined_globally"),
                filters.stream().map(ActionFilter::getId).collect(Collectors.toList()));
    }

    @Test
    public void testActionExtensionPoint() {
        Action newDocument = getService().getAction("newDocument");
        assertEquals("newDocument", newDocument.getId());
        assertEquals("select_document_type", newDocument.getLink());
        assertEquals("action.new.document", newDocument.getLabel());
        assertEquals("/icons/action_add.gif", newDocument.getIcon());

        assertEquals(List.of("folder"), newDocument.getCategoryList());
        assertEquals(List.of("createChild"), newDocument.getFilterIds());

        Action logout = getService().getAction("logout");
        assertEquals("logout", logout.getId());
        assertEquals("logout", logout.getLink());
        assertEquals("Logout", logout.getLabel());
        assertEquals("/icons/logout.gif", logout.getIcon());
        assertEquals(List.of("global"), logout.getCategoryList());
        assertEquals(Collections.emptyList(), logout.getFilterIds());

        Action viewHiddenInfo = getService().getAction("viewHiddenInfo");
        // disabled
        assertNull(viewHiddenInfo);

        Action view = getService().getAction("TAB_VIEW");
        assertEquals("TAB_VIEW", view.getId());
        assertEquals("view", view.getLink());
        assertEquals("View", view.getLabel());
        assertEquals("/icons/view.gif", view.getIcon());
        assertEquals("", view.getConfirm());
        assertEquals(List.of("tabs", "view"), view.getCategoryList());
        assertEquals(List.of(), view.getFilterIds());
    }

    @Test
    public void testFilterExtensionPoint() {
        DefaultActionFilter f2 = (DefaultActionFilter) getService().getFilter("theFilter");
        DefaultActionFilter f3 = (DefaultActionFilter) getService().getFilter("createChild");

        assertSame(DefaultActionFilter.class, f2.getClass());
        assertSame(DefaultActionFilter.class, f3.getClass());

        assertEquals("theFilter", f2.getId());
        assertEquals("createChild", f3.getId());

        assertEquals(2, f2.getRules().length);

        FilterRule rule1 = f2.getRules()[0];
        FilterRule rule2 = f2.getRules()[1];
        assertTrue(rule1.grant);
        assertTrue(rule2.grant);

        assertEquals(1, rule1.conditions.length);
        assertEquals(2, rule2.conditions.length);
        assertEquals("EL condition", rule1.conditions[0]);
        assertEquals("EL condition 1", rule2.conditions[0]);
        assertEquals("EL condition 2", rule2.conditions[1]);

        assertEquals(2, rule1.permissions.length);
        assertEquals(0, rule2.permissions.length);
        assertEquals("admin", rule1.permissions[0]);
        assertEquals("editor", rule1.permissions[1]);

        assertEquals(2, rule1.facets.length);
        assertEquals(0, rule2.facets.length);
        assertEquals("Viewable", rule1.facets[0]);
        assertEquals("Writable", rule1.facets[1]);

        assertEquals(1, f3.getRules().length);

        FilterRule rule = f3.getRules()[0];
        assertTrue(rule.grant);

        assertEquals(1, rule.permissions.length);
        assertEquals("Write", rule.permissions[0]);

        assertEquals(1, rule.facets.length);
        assertEquals("Versionable", rule.facets[0]);

        assertEquals(2, rule.types.length);
        assertEquals("Workspace", rule.types[0]);
        assertEquals("Section", rule.types[1]);

        assertEquals(1, rule.schemas.length);
        assertEquals("Folder", rule.schemas[0]);

        assertEquals(1, rule.conditions.length);
        assertEquals("principal.getName()=='gandalf'", rule.conditions[0]);
    }

    @Test
    public void testActionOverride() throws Exception {
        Action viewAction = getService().getAction("TAB_VIEW");
        assertNotNull(viewAction);
        assertEquals("TAB_VIEW", viewAction.getId());
        assertEquals("view", viewAction.getLink());
        assertEquals("View", viewAction.getLabel());
        assertEquals("/icons/view.gif", viewAction.getIcon());
        assertEquals("", viewAction.getConfirm());
        assertEquals(List.of("tabs", "view"), viewAction.getCategoryList());
        assertEquals(List.of(), viewAction.getFilterIds());

        assertNull(getService().getAction("disabledAction"));

        hotDeployer.deploy(OVERRIDE_CONTRIB);

        Action oviewAction = getService().getAction("TAB_VIEW");
        assertNotNull(oviewAction);
        assertEquals("TAB_VIEW", oviewAction.getId());
        assertEquals("view2", oviewAction.getLink());
        assertEquals("View2", oviewAction.getLabel());
        assertEquals("/icons/view2.gif", oviewAction.getIcon());
        assertEquals("newConfirm", oviewAction.getConfirm());
        assertEquals(List.of("tabs", "view", "OVERRIDE"), oviewAction.getCategoryList());
        assertEquals(List.of("newFilterId1", "newFilter3", "newFilter2", "newFilterId4"), oviewAction.getFilterIds());

        // check corresponding filters are registered correctly
        ActionFilter filter = getService().getFilter("foo");
        assertNull(filter);
        // no filter by that name
        filter = getService().getFilter("newFilterId1");
        assertNull(filter);
        // no filter by that name
        filter = getService().getFilter("newFilterId4");
        assertNull(filter);
        filter = getService().getFilter("newFilter2");
        assertNotNull(filter);
        filter = getService().getFilter("newFilter3");
        assertNotNull(filter);

        assertNull(getService().getAction("disabledAction"));
    }

    // NXP-7287: test override of inner filter
    @Test
    public void testActionOverrideOfInnerFilter() throws Exception {
        Action previewAction = getService().getAction("TAB_WITH_LOCAL_FILTER");
        assertEquals(List.of("VIEW_ACTION_LIST"), previewAction.getCategoryList());
        assertEquals(List.of("local_filter"), previewAction.getFilterIds());
        DefaultActionFilter previewFilter = (DefaultActionFilter) getService().getFilter("local_filter");
        FilterRule[] previewRules = previewFilter.getRules();
        assertNotNull(previewRules);
        assertEquals(1, previewRules.length);
        assertTrue(previewRules[0].grant);
        assertEquals("filter defined in action", previewRules[0].types[0]);

        hotDeployer.deploy(OVERRIDE_CONTRIB);

        Action opreviewAction = getService().getAction("TAB_WITH_LOCAL_FILTER");
        assertEquals(List.of("VIEW_ACTION_LIST"), opreviewAction.getCategoryList());
        assertEquals(List.of("local_filter"), opreviewAction.getFilterIds());
        DefaultActionFilter opreviewFilter = (DefaultActionFilter) getService().getFilter("local_filter");
        FilterRule[] opreviewRules = opreviewFilter.getRules();
        assertNotNull(opreviewRules);
        assertEquals(2, opreviewRules.length);
        assertTrue(opreviewRules[0].grant);
        assertEquals("filter defined in action", opreviewRules[0].types[0]);
        assertFalse(opreviewRules[1].grant);
        assertEquals("filter overriden globally", opreviewRules[1].types[0]);
    }

    // NXP-7287: test override by inner filter
    @Test
    public void testActionOverrideByInnerFilter() throws Exception {
        Action previewAction = getService().getAction("TAB_WITH_GLOBAL_FILTER");
        assertEquals(List.of("VIEW_ACTION_LIST"), previewAction.getCategoryList());
        assertEquals(List.of("filter_defined_globally"), previewAction.getFilterIds());
        DefaultActionFilter previewFilter = (DefaultActionFilter) getService().getFilter("filter_defined_globally");
        FilterRule[] previewRules = previewFilter.getRules();
        assertNotNull(previewRules);
        assertEquals(1, previewRules.length);
        assertFalse(previewRules[0].grant);
        assertEquals("filter defined in its extension point", previewRules[0].types[0]);

        hotDeployer.deploy(OVERRIDE_CONTRIB);

        Action opreviewAction = getService().getAction("TAB_WITH_GLOBAL_FILTER");
        assertNotNull(opreviewAction);
        assertEquals(List.of("VIEW_ACTION_LIST", "OVERRIDE"), opreviewAction.getCategoryList());
        assertEquals(List.of("filter_defined_globally"), opreviewAction.getFilterIds());
        DefaultActionFilter opreviewFilter = (DefaultActionFilter) getService().getFilter("filter_defined_globally");
        FilterRule[] opreviewRules = opreviewFilter.getRules();
        assertNotNull(opreviewRules);
        assertEquals(2, opreviewRules.length);
        assertFalse(opreviewRules[0].grant);
        assertEquals("filter defined in its extension point", previewRules[0].types[0]);
        assertFalse(opreviewRules[1].grant);
        assertEquals("local override of global filter", opreviewRules[1].types[0]);
    }

    @Test
    public void testActionOverrideByInnerFilterNotMerging() throws Exception {
        Action action = getService().getAction("TAB_WITH_LOCAL_FILTER_MERGED");
        assertEquals(List.of("VIEW_ACTION_LIST"), action.getCategoryList());
        assertEquals(List.of("local_filter_merged"), action.getFilterIds());
        DefaultActionFilter filter = (DefaultActionFilter) getService().getFilter("local_filter_merged");
        FilterRule[] rules = filter.getRules();
        assertNotNull(rules);
        assertEquals(1, rules.length);
        assertTrue(rules[0].grant);
        assertEquals("filter defined in action", rules[0].types[0]);

        hotDeployer.deploy(OVERRIDE_CONTRIB);

        Action oAction = getService().getAction("TAB_WITH_LOCAL_FILTER_MERGED");
        assertNotNull(oAction);
        assertEquals(List.of("VIEW_ACTION_LIST", "OVERRIDE"), oAction.getCategoryList());
        assertEquals(List.of("local_filter_merged"), oAction.getFilterIds());
        DefaultActionFilter oFilter = (DefaultActionFilter) getService().getFilter("local_filter_merged");
        FilterRule[] oRules = oFilter.getRules();
        assertNotNull(oRules);
        assertEquals(2, oRules.length);
        assertTrue(oRules[0].grant);
        assertEquals("filter defined in action", oRules[0].types[0]);
        assertFalse(oRules[1].grant);
        assertEquals("local merge of filter", oRules[1].types[0]);
    }

    // test sort of actions with same order value
    @Test
    public void testActionSort() {
        List<Action> actions;
        Action a1 = new Action("id1");
        Action a2 = new Action("id2");
        actions = new ArrayList<>(Arrays.asList(a1, a2));
        Collections.sort(actions);
        assertEquals("id1", actions.get(0).getId());
        assertEquals("id2", actions.get(1).getId());
        // now start with opposite order
        actions = new ArrayList<>(Arrays.asList(a2, a1));
        Collections.sort(actions);
        assertEquals("id1", actions.get(0).getId());
        assertEquals("id2", actions.get(1).getId());
    }

    /**
     * NXP-8739: test that after an action is cloned, availability set on it does not impact the action used on service
     *
     * @since 5.6
     */
    @Test
    public void testActionClone() {
        Action action1 = getService().getAction("TAB_VIEW");
        assertTrue(action1.getAvailable());
        action1.setAvailable(false);
        assertFalse(action1.getAvailable());
        Action action2 = getService().getAction("TAB_VIEW");
        assertTrue(action2.getAvailable());
        // check first action has not changed
        assertFalse(action1.getAvailable());
    }

    @Test
    public void testActionProperties() throws Exception {
        Action action = getService().getAction("actionTestProperties");
        assertTrue(action.getAvailable());
        Map<String, Serializable> properties = action.getProperties();
        assertEquals(2, properties.size());
        // Test single property
        assertEquals("property", properties.get("property"));
        // Test property list
        String[] actionList = (String[]) properties.get("list");
        assertEquals("listItemA", actionList[0]);
        assertEquals("listItemB", actionList[1]);

        hotDeployer.deploy(OVERRIDE_CONTRIB);

        action = getService().getAction("actionTestProperties");
        properties = action.getProperties();
        assertEquals(3, properties.size());
        // Test single property
        assertEquals("property", properties.get("property"));
        // Test property list
        actionList = (String[]) properties.get("list");
        assertEquals("listItemA", actionList[0]);
        assertEquals("listItemB", actionList[1]);
        // Test added single property
        assertEquals("newProperty", properties.get("newProperty"));
    }

    @Test
    public void testUnknownAction() {
        assertNull(getService().getAction("FOO", null, true));
    }

    // NXP-9677: test override of inner filter after uninstall of the first
    // contribution
    @Test
    public void testActionUninstallOverrideOfInnerFilter() throws Exception {
        ActionManager as = getService();

        Action previewAction = as.getAction("TAB_WITH_LOCAL_FILTER");
        assertEquals(List.of("VIEW_ACTION_LIST"), previewAction.getCategoryList());
        assertEquals(List.of("local_filter"), previewAction.getFilterIds());
        DefaultActionFilter previewFilter = (DefaultActionFilter) as.getFilter("local_filter");
        FilterRule[] previewRules = previewFilter.getRules();
        assertTrue(previewRules[0].grant);
        assertEquals("filter defined in action", previewRules[0].types[0]);

        // uninstall first, this time
        hotDeployer.undeploy("org.nuxeo.ecm.actions.tests:test-actions-contrib.xml");
        // deploy override
        hotDeployer.deploy("org.nuxeo.ecm.actions.tests:test-actions-override-innerfilter-contrib.xml");

        as = getService();

        Action opreviewAction = as.getAction("TAB_WITH_LOCAL_FILTER");
        assertEquals(List.of("OVERRIDE"), opreviewAction.getCategoryList());
        assertEquals(List.of("local_filter"), opreviewAction.getFilterIds());
        DefaultActionFilter opreviewFilter = (DefaultActionFilter) as.getFilter("local_filter");
        FilterRule[] opreviewRules = opreviewFilter.getRules();
        assertNotNull(opreviewRules);
        assertEquals(1, opreviewRules.length);
        assertFalse(opreviewRules[0].grant);
        assertEquals("filter re-defined in action", opreviewRules[0].types[0]);
    }

}
