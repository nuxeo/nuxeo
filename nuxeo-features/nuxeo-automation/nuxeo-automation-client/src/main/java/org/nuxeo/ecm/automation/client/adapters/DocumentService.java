/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.adapters;

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

    public static final String FetchDocument = "Document.Fetch";

    public static final String CreateDocument = "Document.Create";

    public static final String DeleteDocument = "Document.Delete";

    public static final String CopyDocument = "Document.Copy";

    public static final String MoveDocument = "Document.Move";

    public static final String GetDocumentChildren = "Document.GetChildren";

    public static final String GetDocumentChild = "Document.GetChild";

    public static final String GetDocumentParent = "Document.GetParent";

    public static final String Query = "Document.Query";

    public static final String SetPermission = "Document.SetACE";

    public static final String RemoveAcl = "Document.RemoveACL";

    public static final String SetDocumentState = "Document.SetLifeCycle";

    public static final String LockDocument = "Document.Lock";

    public static final String UnlockDocument = "Document.Unlock";

    public static final String SetProperty = "Document.SetProperty";

    public static final String RemoveProperty = "Document.RemoveProperty";

    public static final String UpdateDocument = "Document.Update";

    public static final String PublishDocument = "Document.Publish";

    public static final String CreateRelation = "Relations.CreateRelation";

    public static final String GetRelations = "Relations.GetRelations";

    public static final String SetBlob = "Blob.Attach";

    public static final String RemoveBlob = "Blob.Remove";

    public static final String GetBlob = "Blob.Get";

    public static final String GetBlobs = "Blob.GetList";

    public static final String CreateVersion = "Document.CreateVersion";

    public static final String FireEvent = "Notification.SendEvent";

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

    public Document getDocument(String ref) throws Exception {
        return getDocument(DocRef.newRef(ref), null);
    }

    /**
     * @since 5.7
     * @param document document to fetch
     * @param schemas schemas related to the document to fetch (* for all)
     * @return the document returned by server
     * @throws Exception
     */
    public Document getDocument(Document document, String... schemas)
            throws Exception {
        return getDocument(new DocRef(document.getId()),
                StringUtils.join(Arrays.asList(schemas), ","));
    }

    public Document getDocument(DocRef ref) throws Exception {
        return getDocument(ref, null);
    }

    public Document getDocument(DocRef ref, String schemas) throws Exception {
        OperationRequest req = session.newRequest(FetchDocument).set("value",
                ref);
        if (schemas != null) {
            req.setHeader(Constants.HEADER_NX_SCHEMAS, schemas);
        }
        return (Document) req.execute();
    }

    public Document getRootDocument() throws Exception {
        return getDocument(new PathRef("/"));
    }

    /**
     * @since 5.7
     * @param parent can be PathRef or IdRef
     * @param document the document to create
     * @return the document created
     * @throws Exception
     */
    public Document createDocument(String parent, Document document)
            throws Exception {
        return createDocument(DocRef.newRef(parent), document.getType(),
                document.getId(), document.getDirties());
    }

    public Document createDocument(DocRef parent, String type, String name)
            throws Exception {
        return createDocument(parent, type, name, null);
    }

    public Document createDocument(DocRef parent, String type, String name,
            PropertyMap properties) throws Exception {
        OperationRequest req = session.newRequest(CreateDocument).setInput(
                parent).set("type", type).set("name", name);
        if (properties != null && !properties.isEmpty()) {
            req.set("properties", properties);
        }
        return (Document) req.execute();
    }

    /**
     * @since 5.7
     * @param document the document to remove
     * @throws Exception
     */
    public void remove(Document document) throws Exception {
        remove(new DocRef(document.getId()));
    }

    public void remove(DocRef doc) throws Exception {
        session.newRequest(DeleteDocument).setInput(doc).execute();
    }

    public void remove(String ref) throws Exception {
        session.newRequest(DeleteDocument).setInput(DocRef.newRef(ref)).execute();
    }

    public Document copy(DocRef src, DocRef targetParent) throws Exception {
        return copy(src, targetParent, null);
    }

    public Document copy(DocRef src, DocRef targetParent, String name)
            throws Exception {
        OperationRequest req = session.newRequest(CopyDocument).setInput(src).set(
                "target", targetParent);
        if (name != null) {
            req.set("name", name);
        }
        return (Document) req.execute();
    }

    public Document move(DocRef src, DocRef targetParent) throws Exception {
        return move(src, targetParent, null);
    }

    public Document move(DocRef src, DocRef targetParent, String name)
            throws Exception {
        OperationRequest req = session.newRequest(MoveDocument).setInput(src).set(
                "target", targetParent);
        if (name != null) {
            req.set("name", name);
        }
        return (Document) req.execute();
    }

    public Documents getChildren(DocRef docRef) throws Exception {
        return (Documents) session.newRequest(GetDocumentChildren).setInput(
                docRef).execute();
    }

    public Document getChild(DocRef docRef, String name) throws Exception {
        return (Document) session.newRequest(GetDocumentChild).setInput(docRef).set(
                "name", name).execute();
    }

    public Document getParent(DocRef docRef) throws Exception {
        return (Document) session.newRequest(GetDocumentParent).setInput(docRef).execute();
    }

    public Documents getParent(DocRef docRef, String type) throws Exception {
        return (Documents) session.newRequest(GetDocumentParent).setInput(
                docRef).set("type", type).execute();
    }

    public Documents query(String query) throws Exception {
        return (Documents) session.newRequest(Query).set("query", query).execute();
    }

    public Document setPermission(DocRef doc, String user, String permission)
            throws Exception {
        return setPermission(doc, user, permission, null, true);
    }

    public Document setPermission(DocRef doc, String user, String permission,
            boolean granted) throws Exception {
        return setPermission(doc, user, permission, null, granted);
    }

    public Document setPermission(DocRef doc, String user, String permission,
            String acl, boolean granted) throws Exception {
        OperationRequest req = session.newRequest(SetPermission).setInput(doc).set(
                "user", user).set("permission", permission).set("grant",
                granted);
        if (acl != null) {
            req.set("acl", acl);
        }
        return (Document) req.execute();
    }

    public Document removeAcl(DocRef doc, String acl) throws Exception {
        return (Document) session.newRequest(RemoveAcl).setInput(doc).set(
                "acl", acl).execute();
    }

    public Document setState(DocRef doc, String state) throws Exception {
        return (Document) session.newRequest(SetDocumentState).setInput(doc).set(
                "value", state).execute();
    }

    public Document lock(DocRef doc) throws Exception {
        return lock(doc, null);
    }

    public Document lock(DocRef doc, String lock) throws Exception {
        OperationRequest req = session.newRequest(LockDocument).setInput(doc);
        if (lock != null) {
            req.set("owner", lock);
        }
        return (Document) req.execute();
    }

    public Document unlock(DocRef doc) throws Exception {
        return (Document) session.newRequest(UnlockDocument).setInput(doc).execute();
    }

    // TODO: value Serializable?
    public Document setProperty(DocRef doc, String key, String value)
            throws Exception {
        return (Document) session.newRequest(SetProperty).setInput(doc).set(
                "xpath", key).set("value", value).execute();
    }

    public Document removeProperty(DocRef doc, String key) throws Exception {
        return (Document) session.newRequest(RemoveProperty).setInput(doc).set(
                "xpath", key).execute();
    }

    /**
     * This method sends the dirty properties to server
     *
     * @since 5.7
     * @param document the document to update
     * @return the document returned by the server
     * @throws Exception
     */
    public Document update(Document document) throws Exception {
        return update(new DocRef(document.getId()), document.getDirties());
    }

    public Document update(DocRef doc, PropertyMap properties) throws Exception {
        return (Document) session.newRequest(UpdateDocument).setInput(doc).set(
                "properties", properties).execute();
    }

    public Document publish(DocRef doc, DocRef section) throws Exception {
        return publish(doc, section, true);
    }

    public Document publish(DocRef doc, DocRef section, boolean override)
            throws Exception {
        return (Document) session.newRequest(PublishDocument).setInput(doc).set(
                "target", section).set("override", override).execute();
    }

    public Document createRelation(DocRef subject, String predicate,
            DocRef object) throws Exception {
        return (Document) session.newRequest(CreateRelation).setInput(subject).set(
                "object", object).set("predicate", predicate).execute();
    }

    public Documents getRelations(DocRef doc, String predicate)
            throws Exception {
        return getRelations(doc, predicate, true);
    }

    public Documents getRelations(DocRef doc, String predicate, boolean outgoing)
            throws Exception {
        return (Documents) session.newRequest(GetRelations).setInput(doc).set(
                "predicate", predicate).set("outgoing", outgoing).execute();
    }

    /**
     * @since 5.5
     */
    public Documents getRelations(DocRef doc, String predicate,
            boolean outgoing, String graphName) throws Exception {
        return (Documents) session.newRequest(GetRelations).setInput(doc).set(
                "predicate", predicate).set("outgoing", outgoing).set(
                "graphName", graphName).execute();
    }

    public void setBlob(DocRef doc, Blob blob) throws Exception {
        setBlob(doc, blob, null);
    }

    public void setBlob(DocRef doc, Blob blob, String xpath) throws Exception {
        OperationRequest req = session.newRequest(SetBlob).setInput(blob).set(
                "document", doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        req.setHeader(Constants.HEADER_NX_VOIDOP, "true");
        req.execute();
    }

    public void removeBlob(DocRef doc) throws Exception {
        removeBlob(doc, null);
    }

    public void removeBlob(DocRef doc, String xpath) throws Exception {
        OperationRequest req = session.newRequest(RemoveBlob).setInput(doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        req.setHeader(Constants.HEADER_NX_VOIDOP, "true");
        req.execute();
    }

    public FileBlob getBlob(DocRef doc) throws Exception {
        return getBlob(doc, null);
    }

    public FileBlob getBlob(DocRef doc, String xpath) throws Exception {
        OperationRequest req = session.newRequest(GetBlob).setInput(doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        return (FileBlob) req.execute();
    }

    public Blobs getBlobs(DocRef doc) throws Exception {
        return getBlobs(doc, null);
    }

    public Blobs getBlobs(DocRef doc, String xpath) throws Exception {
        OperationRequest req = session.newRequest(GetBlobs).setInput(doc);
        if (xpath != null) {
            req.set("xpath", xpath);
        }
        return (Blobs) req.execute();
    }

    public Document createVersion(DocRef doc) throws Exception {
        return createVersion(doc, null);
    }

    /**
     * Increment is one of "None", "Major", "Minor". If null the server default
     * will be used.
     *
     * See {@link VersionIncrement}
     */
    public Document createVersion(DocRef doc, String increment)
            throws Exception {
        OperationRequest req = session.newRequest(CreateVersion).setInput(doc);
        if (increment != null) {
            req.set("increment", increment);
        }
        return (Document) req.execute();
    }

    public void fireEvent(String event) throws Exception {
        fireEvent(null, event);
    }

    public void fireEvent(DocRef doc, String event) throws Exception {
        OperationRequest req = session.newRequest(CreateVersion).setInput(doc);
        req.setHeader(Constants.HEADER_NX_VOIDOP, "true");
        req.execute();
    }
}
