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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestActionFilter extends NXRuntimeTestCase {

    DocumentModel doc;

    ActionService as;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.actions.tests", "test-actions-service.xml");
        deployContrib("org.nuxeo.ecm.actions.tests", "test-filters-contrib.xml");
        deployContrib("org.nuxeo.ecm.actions.tests", "test-filters-override-contrib.xml");
        as = (ActionService) runtime.getComponent(ActionService.ID);
    }

    private static boolean filterAccept(DocumentModel doc, ActionFilter filter) {
        // XXX AT: action is not used anyway
        Action action = new Action();
        ActionContext context = new ActionContext();
        context.setCurrentDocument(doc);
        return filter.accept(action, context);
    }

    private ActionFilter getFilter(String name) {
        return as.getFilterRegistry().getFilter(name);
    }

    public void testAccessors() {
        ActionFilter filter = new DefaultActionFilter();
        filter.setId("foo");
        assertEquals("foo", filter.getId());
        filter = new DefaultActionFilter("bar", null);
        assertEquals("bar", filter.getId());
    }

    public void testNull() {
        ActionFilter filter = getFilter("null");
        assertTrue(filterAccept(null, filter));
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(null, filter));
    }

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
    public void testWorkspaceOrSection2() {
        ActionFilter filter = getFilter("WorkspaceOrSection2");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Section", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    public void testNotFolderish() {
        ActionFilter filter = getFilter("NotFolderish");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Workspace", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
    }

    public void testWorkspaceOrSectionFolderish() {
        ActionFilter filter = getFilter("WorkspaceOrSectionFolderish");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertFalse(filterAccept(doc, filter));
        doc = new MockDocumentModel("Workspace", new String[] { "Folderish" });
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
    }

    public void testWorkspaceOrSectionNotFolderish() {
        ActionFilter filter = getFilter("WorkspaceOrSectionNotFolderish");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Workspace", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
        doc = new MockDocumentModel("File", new String[] { "Folderish" });
        assertFalse(filterAccept(doc, filter));
    }

    public void testBadExpression() {
        ActionFilter filter = getFilter("badExpression");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    public void testSF() {
        ActionFilter filter = getFilter("CheckId");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertEquals("My Document ID", doc.getId());
        assertTrue(filterAccept(doc, filter));
    }

    // non regression test for NXP-408 : the action is considered valid if no
    // denying rule is found and at least one granting rule is found. Check that
    // when no denying rule is found and no granting rule is found, filter is
    // not valid.
    public void testNoDenyingRuleNoGrantingRule() {
        ActionFilter filter = getFilter("NoDenyingRuleNoGrantingRule");
        doc = new MockDocumentModel("NorWorkspaceNorSection", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

    public void testOverrideFilter() {
        ActionFilter filter = getFilter("OverridenFilter");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Section", new String[0]);
        assertFalse(filterAccept(doc, filter));
    }

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

    public void testGroupFilter() {
        ActionFilter filter = getFilter("GroupFilter");
        DefaultActionFilter dFilter = (DefaultActionFilter) filter;
        assertEquals(1, dFilter.getRules().length);
        FilterRule rule = dFilter.getRules()[0];
        assertEquals("administrators", rule.groups[0]);
    }

}
