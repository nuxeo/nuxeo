/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertTrue;

import javax.faces.context.FacesContext;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.jsf.JSFActionContext;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestActionFilter extends NXRuntimeTestCase {

    protected DocumentModel doc;

    protected ActionService as;

    protected MockFacesContext facesContext;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.actions", "OSGI-INF/actions-framework.xml");
        deployContrib("org.nuxeo.ecm.actions.jsf.tests", "test-filters-contrib.xml");
        deployContrib("org.nuxeo.ecm.actions.jsf.tests", "test-filters-override-contrib.xml");
        as = (ActionService) runtime.getComponent(ActionService.ID);

        facesContext = new MockFacesContext();
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());
    }

    protected ActionContext getActionContext(DocumentModel doc) {
        ActionContext context = new JSFActionContext(facesContext);
        context.setCurrentDocument(doc);
        return context;
    }

    protected boolean filterAccept(DocumentModel doc, ActionFilter filter) {
        // XXX AT: action is not used anyway
        Action action = new Action();
        return filter.accept(action, getActionContext(doc));
    }

    private ActionFilter getFilter(String name) {
        return as.getFilter(name);
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
        facesContext.mapVariable("document", doc);
        try {
            assertTrue(filterAccept(doc, filter));
        } finally {
            facesContext.resetExpressions();
        }
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
        ActionContext context = new JSFActionContext(facesContext);
        context.setCurrentDocument(doc);
        assertEquals(0, context.size());
        filter.accept(action, context);
        // something cached
        assertEquals(1, context.size());
        Object precomputed = context.getLocalVariable(DefaultActionFilter.PRECOMPUTED_KEY);
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
        deployContrib("org.nuxeo.ecm.actions.jsf.tests", "test-actions-contrib.xml");

        DocumentModel doc = new MockDocumentModel("Workspace", new String[0]);
        Action action = as.getAction("singleActionRetrievedWithFilter", getActionContext(doc), true);
        assertNotNull(action);
        assertTrue(action.getAvailable());
        action = as.getAction("singleActionRetrievedWithFilter", getActionContext(doc), false);
        assertNotNull(action);
        assertTrue(action.getAvailable());

        doc = new MockDocumentModel("File", new String[0]);
        action = as.getAction("singleActionRetrievedWithFilter", getActionContext(doc), true);
        assertNull(action);
        action = as.getAction("singleActionRetrievedWithFilter", getActionContext(doc), false);
        assertNotNull(action);
        assertFalse(action.getAvailable());
    }

}
