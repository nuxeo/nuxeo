/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoWidgetType;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.query.api.PageSelection;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component providing information contextual to a session on the
 * application.
 *
 * @author Anahide Tchertchian
 */
@Name("layoutDemoContext")
@Scope(SESSION)
public class LayoutDemoContext implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TEST_REPO_NAME = "layoutDemo";

    public static final String DEMO_DOCUMENT_TYPE = "LayoutDemoDocument";

    protected LoginContext demoLoginContext;

    protected CoreSession demoCoreSession;

    @In(create = true)
    protected LayoutDemoManager layoutDemoManager;

    @In(create = true)
    protected WebLayoutManager webLayoutManager;

    protected DocumentModel bareDemoDocument;

    protected DocumentModel previewDocument;

    protected PageSelections<DocumentModel> demoDocuments;

    @Create
    public void openCoreSession() throws Exception {
        demoLoginContext = Framework.login();
        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        demoCoreSession = mgr.getRepository("layoutDemo").open();
    }

    @Destroy
    public void closeCoreSession() throws LoginException {
        if (demoCoreSession != null) {
            CoreInstance.getInstance().close(demoCoreSession);
        }
        if (demoLoginContext != null) {
            demoLoginContext.logout();
        }
    }

    @Factory(value = "standardWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getStandardWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("standard");
    }

    @Factory(value = "listingWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getListingWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("listing");
    }

    @Factory(value = "customWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getCustomWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("custom");
    }

    protected DocumentModel generateBareDemoDocument() throws ClientException {
        try {
            return demoCoreSession.createDocumentModel(DEMO_DOCUMENT_TYPE);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Factory(value = "layoutBareDemoDocument", scope = EVENT)
    public DocumentModel getBareDemoDocument() throws ClientException {
        if (bareDemoDocument == null) {
            bareDemoDocument = generateBareDemoDocument();
        }
        return bareDemoDocument;
    }

    @Factory(value = "layoutPreviewDocument", scope = EVENT)
    public DocumentModel getPreviewDocument() throws ClientException {
        if (previewDocument == null) {
            try {
                previewDocument = generateBareDemoDocument();
                previewDocument.setPathInfo("/", "preview");
                fillPreviewDocumentProperties(previewDocument, 0);
                previewDocument = demoCoreSession.createDocument(previewDocument);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return previewDocument;
    }

    @Factory(value = "layoutDemoDocuments", scope = EVENT)
    public PageSelections<DocumentModel> getDemoDocuments()
            throws ClientException, Exception {
        if (demoDocuments == null) {
            try {
                List<PageSelection<DocumentModel>> docs = new ArrayList<PageSelection<DocumentModel>>();
                DocumentModel demoDocument1 = getListingDemoDocument(1);
                docs.add(new PageSelection<DocumentModel>(demoDocument1, false));
                DocumentModel demoDocument2 = getListingDemoDocument(2);
                docs.add(new PageSelection<DocumentModel>(demoDocument2, false));
                demoDocuments = new PageSelections<DocumentModel>(docs);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return demoDocuments;
    }

    protected DocumentModel fillPreviewDocumentProperties(DocumentModel doc,
            int index) throws ClientException {
        // fill all fields used in preview
        if (index <= 1) {
            doc.setPropertyValue("lds:textField", "Some sample text");
            doc.setPropertyValue("lds:textareaField",
                    "Some sample text with\nseveral lines.");
            doc.setPropertyValue("lds:htmlField",
                    "Some sample text<br/> with html <b>tags</b>.");
            doc.setPropertyValue("lds:secretField", "Some secret text");
            doc.setPropertyValue("lds:selectVocabularyField", "cartman");
            doc.setPropertyValue("lds:selectMultiVocabularyField",
                    new String[] { "cartman", "marsh" });
            doc.setPropertyValue("lds:dateField", Calendar.getInstance());
            doc.setPropertyValue("lds:intField", new Integer(666));
            doc.setPropertyValue("lds:booleanField", Boolean.FALSE);
            StringBlob blob = new StringBlob("Hello!\nThis is a sample text.",
                    "text/plain", "UTF-8");
            blob.setFilename("hello.txt");
            doc.setPropertyValue("lds:fileField", blob);
            // TODO: list?
        } else {
            doc.setPropertyValue("lds:textField", "Some other sample text");
            doc.setPropertyValue("lds:textareaField",
                    "Some other sample text with\nseveral lines.");
            doc.setPropertyValue("lds:htmlField",
                    "Some other sample text<br/> with html <b>tags</b>.");
            doc.setPropertyValue("lds:secretField", "Some other secret text");
            doc.setPropertyValue("lds:selectVocabularyField", "marsh");
            doc.setPropertyValue("lds:selectMultiVocabularyField",
                    new String[] { "cartman" });
            doc.setPropertyValue("lds:dateField", Calendar.getInstance());
            doc.setPropertyValue("lds:intField", new Integer(667));
            doc.setPropertyValue("lds:booleanField", Boolean.TRUE);
            StringBlob blob = new StringBlob(
                    "Hello!\nThis is another sample text.", "text/plain",
                    "UTF-8");
            blob.setFilename("hello-again.txt");
            doc.setPropertyValue("lds:fileField", blob);
        }
        return doc;
    }

    protected DocumentModel fillListingDocumentProperties(DocumentModel doc,
            int index) throws ClientException {
        if (index <= 1) {
            doc.prefetchCurrentLifecycleState("project");
        } else {
            doc.prefetchCurrentLifecycleState("deleted");
        }
        // fill demo docs for listings
        doc.setPropertyValue("dc:title", "Demo document " + index);
        Calendar created = Calendar.getInstance();
        created.set(Calendar.YEAR, 2000 + index);
        created.set(Calendar.MONTH, 2 + index);
        created.set(Calendar.DATE, 2 + index);
        doc.setPropertyValue("dc:created", created);
        Calendar modified = Calendar.getInstance();
        modified.set(Calendar.YEAR, 2011);
        modified.set(Calendar.MONTH, 3);
        modified.set(Calendar.DATE, 16);
        if (index <= 1) {
            doc.setPropertyValue("dc:modified", modified);
            doc.setPropertyValue("dc:creator", "cartman");
            doc.setPropertyValue("dc:lastContributor", "kenny");
        } else {
            doc.setPropertyValue("dc:modified", created);
            doc.setPropertyValue("dc:creator", "kenny");
            doc.setPropertyValue("dc:lastContributor", "cartman");
        }
        doc.setPropertyValue("uid:major_version", new Integer(1));
        doc.setPropertyValue("uid:minor_version", new Integer(index));
        if (index <= 1) {
            doc.setPropertyValue("common:icon", "/icons/pdf.png");
        }
        return doc;
    }

    protected DocumentModel getListingDemoDocument(int index)
            throws ClientException {
        DocumentModel doc = generateBareDemoDocument();
        doc.setPathInfo("/", "demoDoc_" + index);
        fillListingDocumentProperties(doc, index);
        fillPreviewDocumentProperties(doc, index);
        doc = demoCoreSession.createDocument(doc);
        // set lock after creation
        if (index <= 1) {
            doc.setLock();
        }
        return doc;
    }

}
