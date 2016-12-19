/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.adapters;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Blobs;
import org.nuxeo.ecm.automation.client.model.DocRef;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.client.model.PropertyMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentService {

    public static final String FetchDocument = "Repository.GetDocument";

    public static final String CreateDocument = "Document.Create";

    public static final String DeleteDocument = "Document.Delete";

    public static final String CopyDocument = "Document.Copy";

    public static final String MoveDocument = "Document.Move";

    public static final String GetDocumentChildren = "Document.GetChildren";

    public static final String GetDocumentChild = "Document.GetChild";

    public static final String GetDocumentParent = "Document.GetParent";

    public static final String Query = "Repository.Query";

    public static final String SetPermission = "Document.AddACE";

    public static final String RemoveAcl = "Document.RemoveACL";

    public static final String SetDocumentState = "Document.FollowLifecycleTransition";

    public static final String LockDocument = "Document.Lock";

    public static final String UnlockDocument = "Document.Unlock";

    public static final String SetProperty = "Document.SetProperty";

    public static final String RemoveProperty = "Document.RemoveProperty";

    public static final String UpdateDocument = "Document.Update";

    public static final String PublishDocument = "Document.Publish";

    public static final String CreateRelation = "Document.AddRelation";

    public static final String GetRelations = "Document.GetLinkedDocuments";

    public static final String SetBlob = "Blob.AttachOnDocument";

    public static final String RemoveBlob = "Blob.RemoveFromDocument";

    public static final String GetBlob = "Document.GetBlob";

    public static final String GetBlobs = "Document.GetBlobsByProperty";

    public static final String CreateVersion = "Document.CreateVersion";

    public static final String FireEvent = "Event.Fire";

    // The following are not yet implemented
    public static final String CheckOut = "Document.CheckOut";

    public static final String CheckIn = "Document.CheckIn";

    // //TODO GetAcl?
    protected Session session;

    public DocumentService(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public Document getDocument(String ref) throws IOException {
        return getDocument(DocRef.newRef(ref), null);
    }

    /**
     * @since 5.7
     * @param document document to fetch
     * @param schemas schemas related to the document to fetch (* for all)
     * @return the document returned by server
     */
    public Document getDocument(Document document, String... schemas) throws IOException {
        return getDocument(new DocRef(document.getId()), StringUtils.join(Arrays.asList(schemas), ","));
    }

    public Document getDocument(DocRef ref) throws IOException {
        return getDocument(ref, null);
    }

    public Document getDocument(DocRef ref, String schemas) throws IOException {
        OperationRequest req = session.newRequest(FetchDocument).set("value", ref);
        if (schemas != null) {
            req.setHeader(Constants.HEADER_NX_SCHEMAS, schemas);
        }
        return (Document) req.execute();
    }

    public Document getRootDocument() throws IOException {
        return getDocument(new PathRef("/"));
    }

    /**
     * @since 5.7
     * @param parent can be PathRef or IdRef
     * @param document the document to create
     * @return the document created
     */
    public Document createDocument(String parent, Document document) throws IOException {
        return createDocument(DocRef.newRef(parent), document.getType(), document.getId(), document.getDirties());
    }

    public Document createDocument(DocRef parent, String type, String name) throws IOException {
        return createDocument(parent, type, name, null);
    }

    /**
     * THIS METHOD IS PART OF PRIVATE API NOW, DON'T USE IT.
     *
     * @deprecated since 9.1 use {@link #createDocument(String, Document)} or
     *             {@link #createDocument(DocRef, String, String)} instead as instances of {@link PropertyMap} is now
     *             read-only.
     */
    @Deprecated
    public Document createDocument(DocRef parent, String type, String name, PropertyMap properties) throws IOException {
        OperationRequest req = session.newRequest(CreateDocument).setInput(parent).set("type", type).set("name", name);
        if (properties != null && !properties.isEmpty()) {
            req.set("properties", properties);
        }
        return (Document) req.execute();
    }

    /**
     * @since 5.7
     * @param document the document to remove
     */
    public void remove(Document document) throws IOException {
        remove(new DocRef(document.getId()));
    }

    public void remove(DocRef doc) throws IOException {
        session.newRequest(DeleteDocument).setInput(doc).execute();
    }

    public void remove(String ref) throws IOException {
        session.newRequest(DeleteDocument).setInput(DocRef.newRef(ref)).execute();
    }

    public Document copy(DocRef src, DocRef targetParent) throws IOException {
        return copy(src, targetParent, null);
    }

    public Document copy(DocRef src, DocRef targetParent, String name) throws IOException {
        OperationRequest req = session.newRequest(CopyDocument).setInput(src).set("target", targetParent);
        if (name != null) {
            req.set("name", name);
        }
        return (Document) req.execute();
    }

    public Document move(DocRef src, DocRef targetParent) throws IOException {
        return move(src, targetParent, null);
    }

    public Document move(DocRef src, DocRef targetParent, String name) throws IOException {
        OperationRequest req = session.newRequest(MoveDocument).setInput(src).set("target", targetParent);
        if (name != null) {
            req.set("name", name);
        }
        return (Document) req.execute();
    }

    public Documents getChildren(DocRef docRef) throws IOException {
        return (Documents) session.newRequest(GetDocumentChildren).setInput(docRef).execute();
    }

    public Document getChild(DocRef docRef, String name) throws IOException {
        return (Document) session.newRequest(GetDocumentChild).setInput(docRef).set("name", name).execute();
    }

    public Document getParent(DocRef docRef) throws IOException {
        return (Document) session.newRequest(GetDocumentParent).setInput(docRef).execute();
    }

    public Documents getParent(DocRef docRef, String type) throws IOException {
        return (Documents) session.newRequest(GetDocumentParent).setInput(docRef).set("type", type).execute();
    }

    public Documents query(String query) throws IOException {
        return (Documents) session.newRequest(Query).set("query", query).execute();
    }

    public Document setPermission(DocRef doc, String user, String permission) throws IOException {
        return setPermission(doc, user, permission, null, true);
    }

    public Document setPermission(DocRef doc, String user, String permission, boolean granted) throws IOException {
        return setPermission(doc, user, permission, null, granted);
    }

    public Document setPermission(DocRef doc, String user, String permission, String acl, boolean granted)
            throws IOException {
        OperationRequest req = session.newRequest(SetPermission)
                                      .setInput(doc)
                                      .set("user", user)
                                      .set("permission", permission)
                                      .set("grant", granted);
        if (acl != null) {
            req.set("acl", acl);
        }
        return (Document) req.execute();
    }

    public Document removeAcl(DocRef doc, String acl) throws IOException {
        return (Document) session.newRequest(RemoveAcl).setInput(doc).set("acl", acl).execute();
    }

    public Document setState(DocRef doc, String state) throws IOException {
        return (Document) session.newRequest(SetDocumentState).setInput(doc).set("value", state).execute();
    }

    public Document lock(DocRef doc) throws IOException {
        return lock(doc, null);
    }

    public Document lock(DocRef doc, String lock) throws IOException {
        OperationRequest req = session.newRequest(LockDocument).setInput(doc);
        if (lock != null) {
            req.set("owner", lock);
        }
        return (Document) req.execute();
    }

    public Document unlock(DocRef doc) throws IOException {
        return (Document) session.newRequest(UnlockDocument).setInput(doc).execute();
    }

    // TODO: value Serializable?
    public Document setProperty(DocRef doc, String key, String value) throws IOException {
        return (Document) session.newRequest(SetProperty).setInput(doc).set("xpath", key).set("value", value).execute();
    }

    public Document removeProperty(DocRef doc, String key) throws IOException {
        return (Document) session.newRequest(RemoveProperty).setInput(doc).set("xpath", key).execute();
    }

    /**
     * This method sends the dirty properties to server
     *
     * @since 5.7
     * @param document the document to update
     * @return the document returned by the server
     */
    public Document update(Document document) throws IOException {
        return (Document) session.newRequest(UpdateDocument)
                                 .setInput(document)
                                 .set("properties", document.getDirties())
                                 .execute();
    }

    /**
     * @deprecated since 9.1 use {@link #update(Document)} instead as instances of {@link PropertyMap} is now read-only.
     */
    @Deprecated
    public Document update(DocRef doc, PropertyMap properties) throws IOException {
        return (Document) session.newRequest(UpdateDocument).setInput(doc).set("properties", properties).execute();
    }

    public Document publish(DocRef doc, DocRef section) throws IOException {
        return publish(doc, section, true);
    }

    public Document publish(DocRef doc, DocRef section, boolean override) throws IOException {
        return (Document) session.newRequest(PublishDocument)
                                 .setInput(doc)
                                 .set("target", section)
                                 .set("override", override)
                                 .execute();
    }

    public Document createRelation(DocRef subject, String predicate, DocRef object) throws IOException {
        return (Document) session.newRequest(CreateRelation)
                                 .setInput(subject)
                                 .set("object", object)
                                 .set("predicate", predicate)
                                 .execute();
    }

    public Documents getRelations(DocRef doc, String predicate) throws IOException {
        return getRelations(doc, predicate, true);
    }

    public Documents getRelations(DocRef doc, String predicate, boolean outgoing) throws IOException {
        return (Documents) session.newRequest(GetRelations)
                                  .setInput(doc)
                                  .set("predicate", predicate)
                                  .set("outgoing", outgoing)
                                  .execute();
    }

    /**
     * @since 5.5
     */
    public Documents getRelations(DocRef doc, String predicate, boolean outgoing, String graphName) throws IOException {
        return (Documents) session.newRequest(GetRelations)
                                  .setInput(doc)
                                  .set("predicate", predicate)
                                  .set("outgoing", outgoing)
                                  .set("graphName", graphName)
                                  .execute();
    }

    public void setBlob(DocRef doc, Blob blob) throws IOException {
        setBlob(doc, blob, null);
    }

    public void setBlob(DocRef doc, Blob blob, String xpath) throws IOException {
        OperationRequest req = session.newRequest(SetBlob).setInput(blob).set("document", doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        req.setHeader(Constants.HEADER_NX_VOIDOP, "true");
        req.execute();
    }

    public void removeBlob(DocRef doc) throws IOException {
        removeBlob(doc, null);
    }

    public void removeBlob(DocRef doc, String xpath) throws IOException {
        OperationRequest req = session.newRequest(RemoveBlob).setInput(doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        req.setHeader(Constants.HEADER_NX_VOIDOP, "true");
        req.execute();
    }

    public FileBlob getBlob(DocRef doc) throws IOException {
        return getBlob(doc, null);
    }

    public FileBlob getBlob(DocRef doc, String xpath) throws IOException {
        OperationRequest req = session.newRequest(GetBlob).setInput(doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        return (FileBlob) req.execute();
    }

    public Blobs getBlobs(DocRef doc) throws IOException {
        return getBlobs(doc, null);
    }

    public Blobs getBlobs(DocRef doc, String xpath) throws IOException {
        OperationRequest req = session.newRequest(GetBlobs).setInput(doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        return (Blobs) req.execute();
    }

    public Document createVersion(DocRef doc) throws IOException {
        return createVersion(doc, null);
    }

    /**
     * Increment is one of "None", "Major", "Minor". If null the server default will be used. See
     * {@link VersionIncrement}
     */
    public Document createVersion(DocRef doc, String increment) throws IOException {
        OperationRequest req = session.newRequest(CreateVersion).setInput(doc);
        if (increment != null) {
            req.set("increment", increment);
        }
        return (Document) req.execute();
    }

    public void fireEvent(String event) throws IOException {
        fireEvent(null, event);
    }

    public void fireEvent(DocRef doc, String event) throws IOException {
        OperationRequest req = session.newRequest(CreateVersion).setInput(doc);
        req.setHeader(Constants.HEADER_NX_VOIDOP, "true");
        req.execute();
    }
}
