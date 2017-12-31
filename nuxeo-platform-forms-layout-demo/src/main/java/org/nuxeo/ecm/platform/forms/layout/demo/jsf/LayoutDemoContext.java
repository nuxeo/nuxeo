/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.actions.jsf.JSFActionContext;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoWidgetType;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.PageSelection;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.ecm.platform.query.core.AggregateBase;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.BucketTerm;

/**
 * Seam component providing information contextual to a session on the application.
 *
 * @author Anahide Tchertchian
 */
@Name("layoutDemoContext")
@Scope(SESSION)
public class LayoutDemoContext implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TEST_REPO_NAME = "layoutDemo";

    public static final String DEMO_DOCUMENT_TYPE = "LayoutDemoDocument";

    /**
     * @since 7.2
     */
    public static final String VALIDATION_DOCUMENT_TYPE = "LayoutValidationDocument";

    protected CoreSession demoCoreSession;

    @In(create = true)
    protected LayoutDemoManager layoutDemoManager;

    @In(create = true)
    protected WebLayoutManager webLayoutManager;

    @In(create = true)
    protected transient ActionManager actionManager;

    protected DocumentModel bareDemoDocument;

    protected DocumentModel validationDocument;

    protected DocumentModel previewDocument;

    protected PageSelections<DocumentModel> demoDocuments;

    protected List<Action> layoutDemoCustomActions;

    protected Map<String, Aggregate<Bucket>> layoutDemoAggregates;

    @Create
    public void openCoreSession() {
        demoCoreSession = CoreInstance.openCoreSessionSystem("layoutDemo");
    }

    @Destroy
    @BypassInterceptors
    public void closeCoreSession() {
        if (demoCoreSession != null) {
            ((CloseableCoreSession) demoCoreSession).close();
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

    /**
     * @since 6.0
     */
    @Factory(value = "aggregateWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getAggregateWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("aggregate");
    }

    @Factory(value = "actionWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getActionWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("action");
    }

    protected DocumentModel generateBareDemoDocument() {
        return demoCoreSession.createDocumentModel(DEMO_DOCUMENT_TYPE);
    }

    /**
     * @since 7.2
     */
    protected DocumentModel generateValidationDocument() {
        DocumentModel doc = demoCoreSession.createDocumentModel(VALIDATION_DOCUMENT_TYPE);
        doc.setPropertyValue("dc:title", "My title");
        return doc;
    }

    @Factory(value = "layoutBareDemoDocument", scope = EVENT)
    public DocumentModel getBareDemoDocument() {
        if (bareDemoDocument == null) {
            bareDemoDocument = generateBareDemoDocument();
            bareDemoDocument.setPropertyValue("dc:title", "My title");
            bareDemoDocument.setPropertyValue("dc:description", "My description");
            bareDemoDocument.setPropertyValue("lds:select_coverage_field", "europe/France");
        }
        return bareDemoDocument;
    }

    /**
     * @since 7.2
     */
    @Factory(value = "layoutValidationDocument", scope = EVENT)
    public DocumentModel getValidationDocument() {
        if (validationDocument == null) {
            validationDocument = generateValidationDocument();
        }
        return validationDocument;
    }

    /**
     * @since 7.2
     */
    public void resetValidationDocument() {
        validationDocument = null;
    }

    @Factory(value = "layoutPreviewDocument", scope = EVENT)
    public DocumentModel getPreviewDocument() {
        if (previewDocument == null) {
            previewDocument = generateBareDemoDocument();
            previewDocument.setPathInfo("/", "preview");
            fillPreviewDocumentProperties(previewDocument, 0);
            previewDocument = demoCoreSession.createDocument(previewDocument);
        }
        return previewDocument;
    }

    @Factory(value = "layoutDemoDocuments", scope = EVENT)
    public PageSelections<DocumentModel> getDemoDocuments() {
        if (demoDocuments == null) {
            List<PageSelection<DocumentModel>> docs = new ArrayList<PageSelection<DocumentModel>>();
            DocumentModel demoDocument1 = getListingDemoDocument(1);
            docs.add(new PageSelection<DocumentModel>(demoDocument1, false));
            DocumentModel demoDocument2 = getListingDemoDocument(2);
            docs.add(new PageSelection<DocumentModel>(demoDocument2, false));
            demoDocuments = new PageSelections<DocumentModel>(docs);
        }
        return demoDocuments;
    }

    protected DocumentModel fillPreviewDocumentProperties(DocumentModel doc, int index) {
        // fill all fields used in preview
        if (index <= 1) {
            doc.setPropertyValue("lds:textField", "Some sample text");
            doc.setPropertyValue("lds:anotherTextField", "");
            doc.setPropertyValue("lds:textareaField", "Some sample text with\nseveral lines.");
            doc.setPropertyValue("lds:htmlField", "Some sample text<br/> with html <b>tags</b>.");
            doc.setPropertyValue("lds:secretField", "Some secret text");
            doc.setPropertyValue("lds:selectVocabularyField", "cartman");
            doc.setPropertyValue("lds:selectMultiVocabularyField", new String[] { "cartman", "marsh" });
            doc.setPropertyValue("lds:selectVocabularyLocalizedField", "europe");
            doc.setPropertyValue("lds:selectMultiVocabularyLocalizedField", new String[] { "europe" });
            doc.setPropertyValue("lds:selectVocabularyL10NField", "europe");
            doc.setPropertyValue("lds:selectMultiVocabularyL10NField", new String[] { "europe", "africa" });
            doc.setPropertyValue("lds:select_coverage_field", "africa/Botswana");
            doc.setPropertyValue("lds:select_subjects_multi_fields", new String[] { "art/art history", "art/culture",
                    "sciences/logic" });
            doc.setPropertyValue("lds:select_user_field", "Administrator");
            doc.setPropertyValue("lds:select_users_multi_fields", new String[] { "Administrator" });
            doc.setPropertyValue("lds:select_doc_field", "");
            doc.setPropertyValue("lds:select_docs_multi_fields", new String[] {});
            doc.setPropertyValue("lds:dateField", Calendar.getInstance());
            doc.setPropertyValue("lds:intField", new Integer(666));
            doc.setPropertyValue("lds:booleanField", Boolean.FALSE);
            Blob blob = Blobs.createBlob("Hello!\nThis is a sample text.");
            blob.setFilename("hello.txt");
            doc.setPropertyValue("lds:fileField", (Serializable) blob);

            // complex props
            ArrayList<Map<String, Serializable>> cl = new ArrayList<Map<String, Serializable>>();
            HashMap<String, Serializable> clItem = new HashMap<String, Serializable>();
            clItem.put("stringComplexItem", "Some sample text");
            clItem.put("dateComplexItem", Calendar.getInstance());
            clItem.put("intComplexItem", new Integer(33));
            clItem.put("booleanComplexItem", Boolean.FALSE);
            clItem.put("stringComplexItem2", "Hello, ");
            clItem.put("stringComplexItem3", "is it me you're looking for?");

            HashMap<String, Serializable> clItem2 = new HashMap<String, Serializable>();
            clItem2.put("stringComplexItem", "Some other sample text");
            clItem2.put("dateComplexItem", Calendar.getInstance());
            clItem2.put("intComplexItem", new Integer(-2));
            clItem2.put("booleanComplexItem", Boolean.TRUE);

            cl.add(clItem);
            cl.add(clItem2);

            doc.setPropertyValue("lds:complexList", cl);
            doc.setPropertyValue("lds:complexField", clItem);

        } else {
            doc.setPropertyValue("lds:textField", "Some other sample text");
            doc.setPropertyValue("lds:anotherTextField", "");
            doc.setPropertyValue("lds:textareaField", "Some other sample text with\nseveral lines.");
            doc.setPropertyValue("lds:htmlField", "Some other sample text<br/> with html <b>tags</b>.");
            doc.setPropertyValue("lds:secretField", "Some other secret text");
            doc.setPropertyValue("lds:selectVocabularyField", "marsh");
            doc.setPropertyValue("lds:selectMultiVocabularyField", new String[] { "cartman" });
            doc.setPropertyValue("select_coverage_field", "africa/Botswana");
            doc.setPropertyValue("lds:select_subjects_multi_fields", new String[] { "art/art history", "art/culture",
                    "sciences/logic" });
            doc.setPropertyValue("lds:dateField", Calendar.getInstance());
            doc.setPropertyValue("lds:intField", new Integer(667));
            doc.setPropertyValue("lds:booleanField", Boolean.TRUE);
            Blob blob = Blobs.createBlob("Hello!\nThis is another sample text.");
            blob.setFilename("hello-again.txt");
            doc.setPropertyValue("lds:fileField", (Serializable) blob);
        }
        return doc;
    }

    protected DocumentModel fillListingDocumentProperties(DocumentModel doc, int index) {
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
            doc.setPropertyValue("dc:creator", "Administrator");
            doc.setPropertyValue("dc:lastContributor", "Administrator");
        } else {
            doc.setPropertyValue("dc:modified", created);
            doc.setPropertyValue("dc:creator", "Administrator");
            doc.setPropertyValue("dc:lastContributor", "Administrator");
        }
        doc.setPropertyValue("uid:major_version", new Integer(1));
        doc.setPropertyValue("uid:minor_version", new Integer(index));
        if (index <= 1) {
            doc.setPropertyValue("common:icon", "/icons/pdf.png");
        }
        return doc;
    }

    protected DocumentModel getListingDemoDocument(int index) {
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

    @Factory(value = "layoutDemoCustomActions", scope = EVENT)
    public List<Action> getLayoutDemoCustomActions() {
        if (layoutDemoCustomActions == null) {
            layoutDemoCustomActions = new ArrayList<Action>();
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null) {
                throw new NuxeoException("Faces context is null");
            }
            ActionContext ctx = new JSFActionContext(faces);
            List<Action> actions = actionManager.getActions("LAYOUT_DEMO_ACTIONS", ctx);
            if (actions != null) {
                layoutDemoCustomActions.addAll(actions);
            }
        }
        return layoutDemoCustomActions;
    }

    /**
     * @since 6.0
     */
    @Factory(value = "layoutDemoAggregates", scope = EVENT)
    public Map<String, Aggregate<Bucket>> getLayoutDemoAggregates() {
        if (layoutDemoAggregates == null) {
            layoutDemoAggregates = new HashMap<>();

            AggregateDefinition mockDef = new AggregateDescriptor();
            mockDef.setId("mock");

            List<Bucket> buckets = new ArrayList<>();
            buckets.add(new BucketTerm("eric", 10));
            buckets.add(new BucketTerm("stan", 5));
            buckets.add(new BucketTerm("kyle", 2));
            Aggregate<Bucket> stringTerms = new AggregateBase<Bucket>(mockDef, null);
            stringTerms.setBuckets(buckets);
            layoutDemoAggregates.put("string_terms", stringTerms);

            buckets = new ArrayList<>();
            buckets.add(new BucketTerm("cartman", 10));
            buckets.add(new BucketTerm("marsh", 5));
            buckets.add(new BucketTerm("broflovski", 2));
            Aggregate<Bucket> dirTerms = new AggregateBase<Bucket>(mockDef, null);
            dirTerms.setBuckets(buckets);
            layoutDemoAggregates.put("dir_terms", dirTerms);

            buckets = new ArrayList<>();
            buckets.add(new BucketTerm("oceania", 10));
            buckets.add(new BucketTerm("antarctica", 5));
            buckets.add(new BucketTerm("europe", 2));
            Aggregate<Bucket> dirTermsL10n = new AggregateBase<Bucket>(mockDef, null);
            dirTermsL10n.setBuckets(buckets);
            layoutDemoAggregates.put("dir_terms_translated", dirTermsL10n);

            buckets = new ArrayList<>();
            buckets.add(new BucketTerm("oceania/Australia", 10));
            buckets.add(new BucketTerm("antarctica", 5));
            buckets.add(new BucketTerm("europe/France", 2));
            Aggregate<Bucket> dirTermsL10nHier = new AggregateBase<Bucket>(mockDef, null);
            dirTermsL10nHier.setBuckets(buckets);
            layoutDemoAggregates.put("dir_terms_l10n", dirTermsL10nHier);
        }
        return layoutDemoAggregates;
    }
}
