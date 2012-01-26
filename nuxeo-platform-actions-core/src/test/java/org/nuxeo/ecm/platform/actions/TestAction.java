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
 * $Id: TestAction.java 20218 2007-06-07 19:19:46Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestAction extends NXRuntimeTestCase {

    ActionService as;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.actions", "OSGI-INF/actions-framework.xml");
        deployContrib("org.nuxeo.ecm.actions.tests", "test-actions-contrib.xml");
        as = (ActionService) runtime.getComponent(ActionService.ID);
    }

    public void testActionExtensionPoint() {
        Collection<Action> actions = as.getActionRegistry().getActions();
        assertEquals(6, actions.size());

        Action newDocument = as.getAction("newDocument");
        assertEquals("newDocument", newDocument.getId());
        assertEquals("select_document_type", newDocument.getLink());
        assertEquals("action.new.document", newDocument.getLabel());
        assertTrue(newDocument.isEnabled());
        assertEquals("/icons/action_add.gif", newDocument.getIcon());

        String[] categories = newDocument.getCategories();
        assertEquals(1, categories.length);
        assertEquals("folder", categories[0]);
        List<String> filterIds = new ArrayList<String>();
        filterIds.add("createChild");
        assertEquals(filterIds, newDocument.getFilterIds());

        Action logout = as.getAction("logout");
        assertEquals("logout", logout.getId());
        assertEquals("logout", logout.getLink());
        assertEquals("Logout", logout.getLabel());
        assertTrue(logout.isEnabled());
        assertEquals("/icons/logout.gif", logout.getIcon());

        categories = logout.getCategories();
        assertEquals(1, categories.length);
        assertEquals("global", categories[0]);
        filterIds.clear();
        assertEquals(filterIds, logout.getFilterIds());

        Action viewHiddenInfo = as.getAction("viewHiddenInfo");
        assertEquals("viewHiddenInfo", viewHiddenInfo.getId());
        assertEquals("view_hidden_info", viewHiddenInfo.getLink());
        assertEquals("viewHiddenInfo", viewHiddenInfo.getLabel());
        assertFalse(viewHiddenInfo.isEnabled());
        assertNull(viewHiddenInfo.getIcon());
        categories = logout.getCategories();
        assertEquals(1, categories.length);
        assertEquals("global", categories[0]);
        filterIds.clear();
        assertEquals(filterIds, viewHiddenInfo.getFilterIds());

        Action view = as.getAction("TAB_VIEW");
        assertEquals("TAB_VIEW", view.getId());
        assertEquals("view", view.getLink());
        assertEquals("View", view.getLabel());
        assertTrue(view.isEnabled());
        assertEquals("/icons/view.gif", view.getIcon());
        categories = view.getCategories();
        assertEquals(2, categories.length);
        assertEquals("tabs", categories[0]);
        assertEquals("view", categories[1]);
        filterIds.clear();
        filterIds.add("MyCustomFilter");
        assertEquals(filterIds, view.getFilterIds());
    }

    public void testFilterExtensionPoint() {
        Collection<ActionFilter> filters = as.getFilterRegistry().getFilters();
        assertEquals(5, filters.size());

        ActionFilter f1 = as.getFilterRegistry().getFilter("MyCustomFilter");
        DefaultActionFilter f2 = (DefaultActionFilter) as.getFilterRegistry().getFilter(
                "theFilter");
        DefaultActionFilter f3 = (DefaultActionFilter) as.getFilterRegistry().getFilter(
                "createChild");

        assertSame(DummyFilter.class, f1.getClass());
        assertSame(DefaultActionFilter.class, f2.getClass());
        assertSame(DefaultActionFilter.class, f3.getClass());

        assertEquals("MyCustomFilter", f1.getId());
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

    public void testActionOverride() throws Exception {
        Action viewAction = as.getAction("TAB_VIEW");
        assertNotNull(viewAction);
        assertEquals(2, viewAction.getCategories().length);
        assertFalse(Arrays.asList(viewAction.getCategories()).contains(
                "OVERRIDE"));
        assertEquals(1, viewAction.getFilterIds().size());
        assertTrue(viewAction.getFilterIds().contains("MyCustomFilter"));

        // deploy override
        deployContrib("org.nuxeo.ecm.actions.tests",
                "test-actions-override-contrib.xml");

        Action oviewAction = as.getAction("TAB_VIEW");
        assertNotNull(oviewAction);
        assertEquals("newConfirm", oviewAction.getConfirm());
        assertEquals(3, oviewAction.getCategories().length);
        assertTrue(Arrays.asList(oviewAction.getCategories()).contains(
                "OVERRIDE"));
        assertTrue(Arrays.asList(oviewAction.getCategories()).contains("view"));
        List<String> filterIds = oviewAction.getFilterIds();
        assertNotNull(filterIds);
        assertEquals(5, filterIds.size());
        assertEquals("MyCustomFilter", filterIds.get(0));
        assertEquals("newFilterId1", filterIds.get(1));
        // filter 4 is not embedded => comes first
        assertEquals("newFilterId4", filterIds.get(2));
        assertEquals("newFilter2", filterIds.get(3));
        assertEquals("newFilter3", filterIds.get(4));

        // check corresponding filters are registered correctly
        ActionFilter filter = as.getFilterRegistry().getFilter("foo");
        assertNull(filter);
        filter = as.getFilterRegistry().getFilter("MyCustomFilter");
        assertNotNull(filter);
        // no filter by that name
        filter = as.getFilterRegistry().getFilter("newFilterId1");
        assertNull(filter);
        // no filter by that name
        filter = as.getFilterRegistry().getFilter("newFilterId4");
        assertNull(filter);
        filter = as.getFilterRegistry().getFilter("newFilter2");
        assertNotNull(filter);
        filter = as.getFilterRegistry().getFilter("newFilter3");
        assertNotNull(filter);
    }

    // NXP-7287: test override of inner filter
    public void testActionOverrideOfInnerFilter() throws Exception {
        Action previewAction = as.getAction("TAB_WITH_LOCAL_FILTER");
        assertNotNull(previewAction);
        assertEquals(1, previewAction.getCategories().length);
        assertTrue(Arrays.asList(previewAction.getCategories()).contains(
                "VIEW_ACTION_LIST"));
        assertFalse(Arrays.asList(previewAction.getCategories()).contains(
                "OVERRIDE"));
        List<String> previewFilterIds = previewAction.getFilterIds();
        assertEquals(1, previewFilterIds.size());
        assertTrue(previewFilterIds.contains("local_filter"));
        DefaultActionFilter previewFilter = (DefaultActionFilter) as.getFilterRegistry().getFilter(
                "local_filter");
        FilterRule[] previewRules = previewFilter.getRules();
        assertNotNull(previewRules);
        assertEquals(1, previewRules.length);
        assertTrue(previewRules[0].grant);
        assertEquals("filter defined in action", previewRules[0].types[0]);

        // deploy override
        deployContrib("org.nuxeo.ecm.actions.tests",
                "test-actions-override-contrib.xml");

        Action opreviewAction = as.getAction("TAB_WITH_LOCAL_FILTER");
        assertNotNull(opreviewAction);
        assertEquals(1, opreviewAction.getCategories().length);
        assertTrue(Arrays.asList(opreviewAction.getCategories()).contains(
                "VIEW_ACTION_LIST"));
        assertFalse(Arrays.asList(opreviewAction.getCategories()).contains(
                "OVERRIDE"));
        assertEquals(1, opreviewAction.getFilterIds().size());
        assertTrue(opreviewAction.getFilterIds().contains("local_filter"));
        DefaultActionFilter opreviewFilter = (DefaultActionFilter) as.getFilterRegistry().getFilter(
                "local_filter");
        FilterRule[] opreviewRules = opreviewFilter.getRules();
        assertNotNull(opreviewRules);
        assertEquals(2, opreviewRules.length);
        assertTrue(opreviewRules[0].grant);
        assertEquals("filter defined in action", previewRules[0].types[0]);
        assertFalse(opreviewRules[1].grant);
        assertEquals("filter overriden globally", opreviewRules[1].types[0]);
    }

    // NXP-7287: test override by inner filter
    public void testActionOverrideByInnerFilter() throws Exception {
        Action previewAction = as.getAction("TAB_WITH_GLOBAL_FILTER");
        assertNotNull(previewAction);
        assertEquals(1, previewAction.getCategories().length);
        assertTrue(Arrays.asList(previewAction.getCategories()).contains(
                "VIEW_ACTION_LIST"));
        assertFalse(Arrays.asList(previewAction.getCategories()).contains(
                "OVERRIDE"));
        List<String> previewFilterIds = previewAction.getFilterIds();
        assertEquals(1, previewFilterIds.size());
        assertTrue(previewFilterIds.contains("filter_defined_globally"));
        DefaultActionFilter previewFilter = (DefaultActionFilter) as.getFilterRegistry().getFilter(
                "filter_defined_globally");
        FilterRule[] previewRules = previewFilter.getRules();
        assertNotNull(previewRules);
        assertEquals(1, previewRules.length);
        assertFalse(previewRules[0].grant);
        assertEquals("filter defined in its extension point",
                previewRules[0].types[0]);

        // deploy override
        deployContrib("org.nuxeo.ecm.actions.tests",
                "test-actions-override-contrib.xml");

        Action opreviewAction = as.getAction("TAB_WITH_GLOBAL_FILTER");
        assertNotNull(opreviewAction);
        assertEquals(2, opreviewAction.getCategories().length);
        assertTrue(Arrays.asList(opreviewAction.getCategories()).contains(
                "VIEW_ACTION_LIST"));
        assertTrue(Arrays.asList(opreviewAction.getCategories()).contains(
                "OVERRIDE"));
        assertEquals(1, opreviewAction.getFilterIds().size());
        assertTrue(opreviewAction.getFilterIds().contains(
                "filter_defined_globally"));
        DefaultActionFilter opreviewFilter = (DefaultActionFilter) as.getFilterRegistry().getFilter(
                "filter_defined_globally");
        FilterRule[] opreviewRules = opreviewFilter.getRules();
        assertNotNull(opreviewRules);
        assertEquals(2, opreviewRules.length);
        assertFalse(opreviewRules[0].grant);
        assertEquals("filter defined in its extension point",
                previewRules[0].types[0]);
        assertFalse(opreviewRules[1].grant);
        assertEquals("local override of global filter",
                opreviewRules[1].types[0]);
    }

    public void testRetrieveActionsByCategory() throws Exception {
        List<Action> viewActions = as.getActions("VIEW_ACTION_LIST",
                new ActionContext());
        assertNotNull(viewActions);
        assertEquals(1, viewActions.size());
        assertEquals("TAB_WITH_GLOBAL_FILTER", viewActions.get(0).getId());

        // deploy override
        deployContrib("org.nuxeo.ecm.actions.tests",
                "test-actions-override-contrib.xml");

        // check there are no duplicates
        viewActions = as.getActions("VIEW_ACTION_LIST", new ActionContext());
        assertNotNull(viewActions);
        assertEquals(1, viewActions.size());
        assertEquals("TAB_WITH_GLOBAL_FILTER", viewActions.get(0).getId());
    }

    public void testFilter() {
        List<Action> actions = as.getActions("view", new ActionContext());
        assertEquals(1, actions.size());
        Action[] actAr = actions.toArray(new Action[actions.size()]);
        assertEquals("TAB_VIEW", actAr[0].getId());
    }

    public void testDefaultFilter() {
        List<Action> actions = as.getActions("global", new ActionContext());
        assertEquals(1, actions.size());
    }

    // test sort of actions with same order value
    public void testActionSort() {
        List<Action> actions;
        Action a1 = new Action("id1", null);
        Action a2 = new Action("id2", null);
        actions = new ArrayList<Action>(Arrays.asList(a1, a2));
        Collections.sort(actions);
        assertEquals("id1", actions.get(0).getId());
        assertEquals("id2", actions.get(1).getId());
        // now start with opposite order
        actions = new ArrayList<Action>(Arrays.asList(a2, a1));
        Collections.sort(actions);
        assertEquals("id1", actions.get(0).getId());
        assertEquals("id2", actions.get(1).getId());
    }

    /**
     * NXP-8739: test that after an action is cloned, availability set on it
     * does not impact the action used on service
     *
     * @since 5.6
     */
    public void testActionClone() {
        Action action1 = as.getAction("viewHiddenInfo");
        assertTrue(action1.getAvailable());
        action1.setAvailable(false);
        assertFalse(action1.getAvailable());
        Action action2 = as.getAction("viewHiddenInfo");
        assertTrue(action2.getAvailable());
        // check first action has not changed
        assertFalse(action1.getAvailable());
    }

}
