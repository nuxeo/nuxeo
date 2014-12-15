/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tdelprat
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.elasticsearch.listener.EventConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Holds information about what org.nuxeo.elasticsearch.commandstype of indexing operation must be processed.
 * IndexingCommands are create "on the fly" via a Synchronous event listener and at commit time the system will merge
 * the commands and generate events for the sync commands.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class IndexingCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Name {
        INSERT, UPDATE, UPDATE_SECURITY, DELETE
    };

    protected Name name;

    public static final String PREFIX = "IndexingCommand-";

    public static final String UNKOWN_DOCUMENT_ID = "unknown";

    protected boolean sync;

    protected boolean recurse;

    protected transient DocumentModel targetDocument;

    protected String uid;

    protected String path;

    protected String repository;

    protected String id;

    protected List<String> schemas;

    protected static final Log log = LogFactory.getLog(IndexingCommand.class);

    protected transient Event indexingEvent;

    protected IndexingCommand() {
        //
    }

    public IndexingCommand(DocumentModel targetDocument, boolean sync, boolean recurse) {
        this(targetDocument, Name.INSERT, sync, recurse);
    }

    public IndexingCommand(DocumentModel targetDocument, Name command, boolean sync, boolean recurse) {
        // we don't want sync and recursive command
        assert (!(sync && recurse) || command == Name.DELETE);
        id = PREFIX + UUID.randomUUID().toString();
        name = command;
        this.sync = sync;
        this.recurse = recurse;
        if (targetDocument == null) {
            uid = UNKOWN_DOCUMENT_ID;
            repository = getDefaultRepository();
            this.targetDocument = null;
        } else {
            DocumentModel doc = getValidTargetDocument(targetDocument);
            this.targetDocument = doc;
            repository = doc.getRepositoryName();
            uid = doc.getId();
        }
        markUpdated();
        assert uid != null : "Invalid IndexingCommand with a null doc id";
    }

    private DocumentModel getValidTargetDocument(DocumentModel target) {
        if (target.getId() != null) {
            return target;
        }
        // transient document try to get it from its path
        DocumentRef documentRef = target.getRef();
        log.warn("Processing indexing command on a document with a null id: " + documentRef
                + " activate trace level for more info.");
        if (log.isTraceEnabled()) {
            Throwable throwable = new Throwable();
            StringWriter stack = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stack));
            log.trace("You should use a document returned by session.createDocument, stack " + stack.toString());
        }
        return target.getCoreSession().getDocument(documentRef);
    }

    private String getDefaultRepository() {
        RepositoryManager mgr = Framework.getLocalService(RepositoryManager.class);
        return mgr.getDefaultRepository().getName();
    }

    public void refresh(CoreSession session) throws ClientException {
        IdRef idref = new IdRef(uid);
        if ((uid != null) && (session.exists(idref))) {
            targetDocument = session.getDocument(idref);
        } else {
            // Doc was deleted : no way we can fetch it
            // re-attach ???
            log.info("Can not refresh document because it was deleted: " + idref);
        }
        markUpdated();
    }

    public String getRepository() {
        return repository;
    }

    public void merge(IndexingCommand other) {
        merge(other.sync, other.recurse);
    }

    public void merge(boolean sync, boolean recurse) {
        this.sync = this.sync || sync;
        this.recurse = this.recurse || recurse;
        markUpdated();
    }

    public boolean canBeMerged(IndexingCommand other) {
        if (name != other.name) {
            return false;
        }
        if (name == Name.DELETE) {
            // we support recursive sync deletion
            return true;
        }
        // only if the result is not a sync and recurse command
        return !((other.sync || sync) && (other.recurse || recurse));
    }

    public boolean isSync() {
        return sync;
    }

    public boolean isRecurse() {
        return recurse;
    }

    public Name getName() {
        return name;
    }

    public DocumentModel getTargetDocument() {
        return targetDocument;
    }

    public String toJSON() throws IOException {
        StringWriter out = new StringWriter();
        JsonFactory factory = new JsonFactory();
        JsonGenerator jsonGen = factory.createJsonGenerator(out);
        toJSON(jsonGen);
        out.flush();
        jsonGen.close();
        return out.toString();
    }

    public void toJSON(JsonGenerator jsonGen) throws IOException {
        jsonGen.writeStartObject();
        jsonGen.writeStringField("id", id);
        jsonGen.writeStringField("name", name.toString());
        jsonGen.writeStringField("docId", getDocId());
        if (targetDocument != null) {
            jsonGen.writeStringField("path", targetDocument.getPathAsString());
        }
        jsonGen.writeStringField("repo", getRepository());
        jsonGen.writeBooleanField("recurse", recurse);
        jsonGen.writeBooleanField("sync", sync);
        jsonGen.writeEndObject();
    }

    public static IndexingCommand fromJSON(CoreSession session, String json) throws ClientException {
        JsonFactory jsonFactory = new JsonFactory();
        try {
            JsonParser jp = jsonFactory.createJsonParser(json);
            try {
                return fromJSON(session, jp);
            } finally {
                jp.close();
            }
        } catch (IOException e) {
            throw ClientException.wrap(e);
        }
    }

    public static IndexingCommand fromJSON(CoreSession session, JsonParser jp) throws IOException {

        IndexingCommand cmd = new IndexingCommand();

        JsonToken token = jp.nextToken(); // will return JsonToken.START_OBJECT
                                          // (verify?)
        if (token != JsonToken.START_OBJECT) {
            return null;
        }
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
            if ("name".equals(fieldname)) { // contains an object
                cmd.name = Name.valueOf(jp.getText());
            } else if ("docId".equals(fieldname)) {
                cmd.uid = jp.getText();
            } else if ("path".equals(fieldname)) {
                cmd.path = jp.getText();
            } else if ("repo".equals(fieldname)) {
                cmd.repository = jp.getText();
            } else if ("id".equals(fieldname)) {
                cmd.id = jp.getText();
            } else if ("recurse".equals(fieldname)) {
                cmd.recurse = jp.getBooleanValue();
            } else if ("sync".equals(fieldname)) {
                cmd.sync = jp.getBooleanValue();
            }
        }
        // resolve DocumentModel if possible
        if (cmd.uid != null) {
            if (!session.getRepositoryName().equals(cmd.repository)) {
                log.error("Unable to restore doc from repository " + cmd.repository + " with a session on repository "
                        + session.getRepositoryName());
            } else {
                IdRef ref = new IdRef(cmd.uid);
                if (!session.exists(ref)) {
                    if (cmd.getName() != Name.DELETE) {
                        log.warn("Unable to retrieve document " + cmd.uid + " form indexing command " + cmd.name);
                    }
                } else {
                    cmd.targetDocument = session.getDocument(ref);
                }
            }
        }

        return cmd;
    }

    public String getId() {
        return id;
    }

    public String getDocId() {
        return uid;
    }

    public IndexingCommand clone(DocumentModel newDoc) {
        return new IndexingCommand(newDoc, name, sync, recurse);
    }

    public String[] getSchemas() {
        String[] ret = null;
        if (schemas != null && schemas.size() > 0) {
            ret = schemas.toArray(new String[schemas.size()]);
        } else if (targetDocument != null) {
            ret = targetDocument.getSchemas();
        }
        return ret;
    }

    public void addSchemas(String schema) {
        if (schemas == null) {
            schemas = new ArrayList<>();
        }
        if (!schemas.contains(schema)) {
            schemas.add(schema);
        }
        markUpdated();
    }

    @Override
    public String toString() {
        try {
            return toJSON();
        } catch (IOException e) {
            return super.toString();
        }
    }

    public void computeIndexingEvent() {
        if (getTargetDocument() != null) {
            CoreSession session = getTargetDocument().getCoreSession();
            if (session != null) {
                EventContextImpl context = new EventContextImpl(session, session.getPrincipal());
                indexingEvent = context.newEvent(EventConstants.ES_INDEX_EVENT_SYNC);
            } else {
                if (Framework.isInitialized()) {
                    log.error(String.format("Unable to generate event, no session found for cmd:"
                            + " %s, sessionid: %s", toString(), getTargetDocument().getSessionId()));
                }
            }
        }
    }

    public Event asIndexingEvent() throws IOException {
        if (indexingEvent == null) {
            computeIndexingEvent();
        }
        if (indexingEvent != null) {
            indexingEvent.getContext().getProperties().put(getId(), toJSON());
        }
        return indexingEvent;
    }

    protected void markUpdated() {
        indexingEvent = null;
        if (sync) {
            computeIndexingEvent();
        }
    }

    /**
     * Try to make the command synchronous. Recurse command will stay in async for update.
     */
    public void makeSync() {
        if (!sync) {
            if (!recurse || name == Name.DELETE) {
                sync = true;
                if (log.isDebugEnabled()) {
                    log.debug("Turn command into sync: " + toString());
                }
                markUpdated();
            }
        }
    }

    public void disconnect() {
        targetDocument = null;
    }
}
