/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.ui.web.api.TabActionsSelection;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * @since 5.4.2
 */
public class TestTabActionsSelection {

    ActionManager actionManager;

    static String DEFAULT_CATEGORY = WebActions.DEFAULT_TABS_CATEGORY;

    static String CUSTOM_CATEGORY = "custom_category";

    enum TAB_ACTION {

        // standard tab
        TAB_VIEW(new String[] { DEFAULT_CATEGORY }),
        // custom tab with multiple categories
        TAB_CUSTOM_MULTICATS(new String[] { DEFAULT_CATEGORY, CUSTOM_CATEGORY }),
        // custom tab
        TAB_CUSTOM(new String[] { CUSTOM_CATEGORY }),
        // custom sub tab
        SUBTAB_CUSTOM(new String[] { "TAB_CUSTOM" + WebActions.SUBTAB_CATEGORY_SUFFIX });

        String[] categories;

        TAB_ACTION(String[] categories) {
            this.categories = categories;
        }

        public String getId() {
            return name();
        }

        public Action getAction() {
            return new Action(name(), categories);
        }

    }

    @Before
    public void setUp() throws Exception {
        List<Action> testActions = new ArrayList<>();
        for (TAB_ACTION tabAction : TAB_ACTION.values()) {
            testActions.add(tabAction.getAction());
        }
        actionManager = new MockActionManager(testActions);
    }

    @After
    public void tearDown() throws Exception {
        actionManager = null;
    }

    @Test
    public void testSetCurrentTabAction() throws Exception {
        TabActionsSelection sel = new TabActionsSelection();
        sel.setCurrentTabAction(CUSTOM_CATEGORY, TAB_ACTION.TAB_CUSTOM.getAction());
        assertEquals(TAB_ACTION.TAB_CUSTOM.getAction(), sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals("custom_category:TAB_CUSTOM", sel.getCurrentTabIds());
        // add another one
        sel.setCurrentTabAction(DEFAULT_CATEGORY, TAB_ACTION.TAB_VIEW.getAction());
        assertEquals(TAB_ACTION.TAB_VIEW.getAction(), sel.getCurrentTabAction(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        // check nothing's changed for previous selection
        assertEquals(TAB_ACTION.TAB_CUSTOM.getAction(), sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals("custom_category:TAB_CUSTOM,:TAB_VIEW", sel.getCurrentTabIds());
        // check reset
        sel.resetCurrentTabs(CUSTOM_CATEGORY);
        assertNull(sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_VIEW.getAction(), sel.getCurrentTabAction(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertEquals(":TAB_VIEW", sel.getCurrentTabIds());
    }

    @Test
    public void testSetCurrentTabActionWithSubTab() throws Exception {
        TabActionsSelection sel = new TabActionsSelection();
        sel.setCurrentTabAction(CUSTOM_CATEGORY, TAB_ACTION.TAB_CUSTOM.getAction());
        assertEquals(TAB_ACTION.TAB_CUSTOM.getAction(), sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals("custom_category:TAB_CUSTOM", sel.getCurrentTabIds());
        // add a sub tab
        sel.setCurrentTabAction(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX,
                TAB_ACTION.SUBTAB_CUSTOM.getAction());
        assertEquals(TAB_ACTION.SUBTAB_CUSTOM.getAction(),
                sel.getCurrentTabAction(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals(TAB_ACTION.SUBTAB_CUSTOM.getId(),
                sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("custom_category:TAB_CUSTOM:SUBTAB_CUSTOM", sel.getCurrentTabIds());
        // check override
        sel.setCurrentTabAction(CUSTOM_CATEGORY, TAB_ACTION.TAB_CUSTOM_MULTICATS.getAction());
        assertEquals(TAB_ACTION.TAB_CUSTOM_MULTICATS.getAction(), sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM_MULTICATS.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        // check subtab is not there anymore
        assertNull(sel.getCurrentTabAction(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertNull(sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("custom_category:TAB_CUSTOM_MULTICATS", sel.getCurrentTabIds());
    }

    @Test
    public void testResetCurrentTabActionWithSubTab() throws Exception {
        TabActionsSelection sel = new TabActionsSelection();
        sel.setCurrentTabAction(CUSTOM_CATEGORY, TAB_ACTION.TAB_CUSTOM.getAction());
        sel.setCurrentTabAction(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX,
                TAB_ACTION.SUBTAB_CUSTOM.getAction());
        // check reset
        sel.resetCurrentTabs(CUSTOM_CATEGORY);
        assertNull(sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabAction(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertNull(sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("", sel.getCurrentTabIds());
    }

    @Test
    public void testSetCurrentTabId() throws Exception {
        TabActionsSelection sel = new TabActionsSelection();
        sel.setCurrentTabId(actionManager, null, CUSTOM_CATEGORY, TAB_ACTION.TAB_CUSTOM.getId());
        assertEquals(TAB_ACTION.TAB_CUSTOM.getAction(), sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals("custom_category:TAB_CUSTOM", sel.getCurrentTabIds());
        // add another one
        sel.setCurrentTabId(actionManager, null, DEFAULT_CATEGORY, TAB_ACTION.TAB_VIEW.getId());
        assertEquals(TAB_ACTION.TAB_VIEW.getAction(), sel.getCurrentTabAction(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        // check nothing's changed for previous selection
        assertEquals(TAB_ACTION.TAB_CUSTOM.getAction(), sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals("custom_category:TAB_CUSTOM,:TAB_VIEW", sel.getCurrentTabIds());
        // check reset
        sel.resetCurrentTabs(CUSTOM_CATEGORY);
        assertNull(sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertEquals(":TAB_VIEW", sel.getCurrentTabIds());
    }

    @Test
    public void testSetCurrentTabIds() throws Exception {
        TabActionsSelection sel = new TabActionsSelection();
        sel.setCurrentTabIds(actionManager, null, "custom_category:TAB_CUSTOM:SUBTAB_CUSTOM,:TAB_VIEW");
        assertEquals(TAB_ACTION.TAB_VIEW.getAction(), sel.getCurrentTabAction(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getAction(), sel.getCurrentTabAction(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.SUBTAB_CUSTOM.getId(),
                sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("custom_category:TAB_CUSTOM:SUBTAB_CUSTOM,:TAB_VIEW", sel.getCurrentTabIds());
    }

    protected TabActionsSelection getTestSelectionToReset() {
        TabActionsSelection sel = new TabActionsSelection();
        sel.setCurrentTabIds(actionManager, null, "custom_category:TAB_CUSTOM:SUBTAB_CUSTOM,:TAB_VIEW");
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.SUBTAB_CUSTOM.getId(),
                sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("custom_category:TAB_CUSTOM:SUBTAB_CUSTOM,:TAB_VIEW", sel.getCurrentTabIds());
        return sel;
    }

    @Test
    public void testSetCurrentTabIds_resetSubTab() throws Exception {
        TabActionsSelection sel = getTestSelectionToReset();
        sel.setCurrentTabIds(actionManager, null, "custom_category:TAB_CUSTOM,:TAB_VIEW");
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("custom_category:TAB_CUSTOM,:TAB_VIEW", sel.getCurrentTabIds());
    }

    @Test
    public void testSetCurrentTabIds_resetDefaultTab() throws Exception {
        TabActionsSelection sel = getTestSelectionToReset();
        sel.setCurrentTabIds(actionManager, null, ":");
        assertNull(sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertEquals(TAB_ACTION.TAB_CUSTOM.getId(), sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertEquals(TAB_ACTION.SUBTAB_CUSTOM.getId(),
                sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("custom_category:TAB_CUSTOM:SUBTAB_CUSTOM", sel.getCurrentTabIds());
    }

    @Test
    public void testSetCurrentTabIds_resetTabWithSubTab() throws Exception {
        TabActionsSelection sel = getTestSelectionToReset();
        sel.setCurrentTabIds(actionManager, null, "custom_category:,:TAB_VIEW");
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertNull(sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals(":TAB_VIEW", sel.getCurrentTabIds());
    }

    @Test
    public void testSetCurrentTabIds_resetAll() throws Exception {
        TabActionsSelection sel = getTestSelectionToReset();
        sel.setCurrentTabIds(actionManager, null, "*:");
        assertNull(sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertNull(sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals("", sel.getCurrentTabIds());
    }

    @Test
    public void testSetCurrentTabIds_resetAllAndSet() throws Exception {
        TabActionsSelection sel = getTestSelectionToReset();
        sel.setCurrentTabIds(actionManager, null, "*:,:TAB_VIEW");
        assertEquals(TAB_ACTION.TAB_VIEW.getId(), sel.getCurrentTabId(DEFAULT_CATEGORY));
        assertNull(sel.getCurrentTabId(CUSTOM_CATEGORY));
        assertNull(sel.getCurrentTabId(TAB_ACTION.TAB_CUSTOM.getId() + WebActions.SUBTAB_CATEGORY_SUFFIX));
        assertEquals(":TAB_VIEW", sel.getCurrentTabIds());
    }

}
