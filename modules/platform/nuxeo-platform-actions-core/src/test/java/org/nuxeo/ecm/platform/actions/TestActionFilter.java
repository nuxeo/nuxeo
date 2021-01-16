/*
 * (C) Copyright 2007-2017 Nuxeo (http://nuxeo.com/) and others.
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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.actions:OSGI-INF/actions-framework.xml")
@Deploy("org.nuxeo.ecm.actions.tests:test-filters-contrib.xml")
@Deploy("org.nuxeo.ecm.actions.tests:test-filters-override-contrib.xml")
public class TestActionFilter {

    protected DocumentModel doc;

    @Inject
    protected ActionManager as;

    protected ActionContext getActionContext(DocumentModel doc) {
        ActionContext context = new ELActionContext();
        context.setCurrentDocument(doc);
        return context;
    }

    protected boolean filterAccept(DocumentModel doc, ActionFilter filter) {
        return filter.accept(getActionContext(doc));
    }

    private ActionFilter getFilter(String name) {
        return as.getFilter(name);
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
        // FIXME
        // assertFalse(filterAccept(doc, filter));
        assertTrue(filterAccept(doc, filter));
    }

    @Test
    public void testAppendFilter() {
        ActionFilter filter = getFilter("AppendFilter");
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
    public void testMergedFilter() {
        ActionFilter filter = getFilter("MergedFilter");
        doc = new MockDocumentModel("Workspace", new String[0]);
        assertTrue(filterAccept(doc, filter));
        doc = new MockDocumentModel("Section", new String[0]);
        assertTrue(filterAccept(doc, filter));
    }

    @Test
    public void testFilterCaching() {
        ActionFilter filter = getFilter("WorkspaceOrSection");
        ActionContext context = new ELActionContext();
        context.setCurrentDocument(doc);
        assertEquals(0, context.size());
        filter.accept(context);
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
    @Deploy("org.nuxeo.ecm.actions.tests:test-actions-contrib.xml")
    public void testGetAction() {
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
