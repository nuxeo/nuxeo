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
        deployContrib("org.nuxeo.ecm.actions.tests", "test-actions-service.xml");
        deployContrib("org.nuxeo.ecm.actions.tests", "test-actions-contrib.xml");
        as = (ActionService) runtime.getComponent(ActionService.ID);
    }

    public void testActionExtensionPoint() {
        Collection<Action> actions = as.getActionRegistry().getActions();
        assertEquals(4, actions.size());

        Action newDocument = as.getActionRegistry().getAction("newDocument");
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

        Action logout = as.getActionRegistry().getAction("logout");
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

        Action viewHiddenInfo = as.getActionRegistry().getAction(
                "viewHiddenInfo");
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

        Action view = as.getActionRegistry().getAction("TAB_VIEW");
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
        assertEquals(3, filters.size());

        ActionFilter f1 = as.getFilterRegistry().getFilter("MyCustomFilter");
        DefaultActionFilter f2 = (DefaultActionFilter) as.getFilterRegistry()
                .getFilter("theFilter");
        DefaultActionFilter f3 = (DefaultActionFilter) as.getFilterRegistry()
                .getFilter("createChild");

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
        Action act1 = as.getActionRegistry().getAction("TAB_VIEW");
        assertNotNull(act1);
        assertEquals(2, act1.getCategories().length);
        assertFalse(Arrays.asList(act1.getCategories()).contains("OVERRIDE"));
        assertEquals(1, act1.getFilterIds().size());
        assertTrue(act1.getFilterIds().contains("MyCustomFilter"));

        deployContrib("org.nuxeo.ecm.actions.tests",
                "test-actions-override-contrib.xml");

        act1 = as.getActionRegistry().getAction("TAB_VIEW");
        assertNotNull(act1);
        assertEquals("newConfirm", act1.getConfirm());
        assertEquals(3, act1.getCategories().length);
        assertTrue(Arrays.asList(act1.getCategories()).contains("OVERRIDE"));
        assertTrue(Arrays.asList(act1.getCategories()).contains("view"));
        assertEquals(3, act1.getFilterIds().size());
        assertTrue(act1.getFilterIds().contains("MyCustomFilter"));
        assertTrue(act1.getFilterIds().contains("newFilter"));
        assertTrue(act1.getFilterIds().contains("otherNewFilter"));
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

}
