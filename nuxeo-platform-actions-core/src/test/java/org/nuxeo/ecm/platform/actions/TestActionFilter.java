/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestActionFilter.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestActionFilter extends NXRuntimeTestCase {

    DocumentModel doc;

    ActionService as;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.actions", "OSGI-INF/actions-framework.xml");
        deployContrib("org.nuxeo.ecm.actions.tests", "test-filters-contrib.xml");
        deployContrib("org.nuxeo.ecm.actions.tests",
                "test-filters-override-contrib.xml");
        as = (ActionService) runtime.getComponent(ActionService.ID);
    }

    protected ActionContext getActionContext(DocumentModel doc) {
        ActionContext context = new ActionContext();
        context.setCurrentDocument(doc);
        return context;
    }

    protected boolean filterAccept(DocumentModel doc, ActionFilter filter) {
        // XXX AT: action is not used anyway
        Action action = new Action();
        return filter.accept(action, getActionContext(doc));
    }

    private ActionFilter getFilter(String name) {
        return as.getFilterRegistry().getFilter(name);
    }

    @Test
    public void testAccessors() {
        ActionFilter filter = new DefaultActionFilter();
        filter.setId("foo");
        assertEquals("foo", filter.getId());
        filter = new DefaultActionFilter("bar", null);
        assertEquals("bar", filter.getId());
    }

    @Test
    public void testNull() {
        ActionFilter filter = getFilter("null");
        assertTrue(filterAccept(null, filter));
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(null, filter));
    }

    @Test
    public void testWorkspaceOrSection() {
        ActionFilter filter = getFilter("WorkspaceOrSection");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Section", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    // test it also works with 2 filters with grant = true
    @Test
    public void testWorkspaceOrSection2() {
        ActionFilter filter = getFilter("WorkspaceOrSection2");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Section", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testNotFolderish() {
        ActionFilter filter = getFilter("NotFolderish");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Workspace", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testWorkspaceOrSectionFolderish() {
        ActionFilter filter = getFilter("WorkspaceOrSectionFolderish");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertFalse(filterAccept(doc, filter));
        doc = new MockDocumentModel("Workspace", new String[] { "Folderish" });
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testWorkspaceOrSectionNotFolderish() {
        ActionFilter filter = getFilter("WorkspaceOrSectionNotFolderish");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Workspace", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testBadExpression() {
        ActionFilter filter = getFilter("badExpression");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testSF() {
        ActionFilter filter = getFilter("CheckId");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertEquals("My Document ID", doc.getId());
        assertTrue(filterAccept(doc, filter));
    }

    // non regression test for NXP-408 : the action is considered valid if no
    // denying rule is found and at least one granting rule is found. Check
    // that
    // when no denying rule is found and no granting rule is found, filter is
    // not valid.
    @Test
    public void testNoDenyingRuleNoGrantingRule() {
        ActionFilter filter = getFilter("NoDenyingRuleNoGrantingRule");
        doc = new MockDocumentModel("NorWorkspaceNorSection", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testOverrideFilter() {
        ActionFilter filter = getFilter("OverridenFilter");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Section", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testAppendFilter() {
        ActionFilter filter = getFilter("AppenedFilter");

        DefaultActionFilter dFilter = (DefaultActionFilter) filter;
        assertEquals(2, dFilter.getRules().length);

        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Section", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Folder", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    @Test
    public void testFilterCaching() {
        ActionFilter filter = getFilter("WorkspaceOrSection");
        Action action = new Action();
        ActionContext context = new ActionContext();
        context.setCurrentDocument(doc);
        assertEquals(0, context.size());
        filter.accept(action, context);
        // something cached
        assertEquals(1, context.size());
        Object precomputed = context.get(DefaultActionFilter.PRECOMPUTED_KEY);
        assertNotNull(precomputed);
    }

    @Test
    public void testGroupFilter() {
        ActionFilter filter = getFilter("GroupFilter");
        DefaultActionFilter dFilter = (DefaultActionFilter) filter;
        assertEquals(1, dFilter.getRules().length);
        FilterRule rule = dFilter.getRules()[0];
        assertEquals("administrators", rule.groups[0]);
    }

    public void testCheckFilter() {
        // test workspace or section
        DocumentModel doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(as.checkFilter("WorkspaceOrSection", getActionContext(doc)));
        doc = new MockDocumentModel("Section", new String[0]);
        assertTrue(as.checkFilter("WorkspaceOrSection", getActionContext(doc)));
        doc = new MockDocumentModel("File", new String[0]);
        assertFalse(as.checkFilter("WorkspaceOrSection", getActionContext(doc)));

        // test bad expression
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertFalse(as.checkFilter("badExpression", getActionContext(doc)));
    }

    @Test
    public void testGetAction() throws Exception {
        deployContrib("org.nuxeo.ecm.actions.tests", "test-actions-contrib.xml");

        DocumentModel doc = new MockDocumentModel("Workspace", new String[0]);
        Action action = as.getAction("singleActionRetrievedWithFilter",
                getActionContext(doc), true);
        assertNotNull(action);
        assertTrue(action.getAvailable());
        action = as.getAction("singleActionRetrievedWithFilter",
                getActionContext(doc), false);
        assertNotNull(action);
        assertTrue(action.getAvailable());

        doc = new MockDocumentModel("File", new String[0]);
        action = as.getAction("singleActionRetrievedWithFilter",
                getActionContext(doc), true);
        assertNull(action);
        action = as.getAction("singleActionRetrievedWithFilter",
                getActionContext(doc), false);
        assertNotNull(action);
        assertFalse(action.getAvailable());
    }

}
