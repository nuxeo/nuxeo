/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Holds information about what type of indexing operation must be processed. IndexingCommands are create "on the fly"
 * via a Synchronous event listener and at post commit time the system will merge the commands and execute worker to
 * process them.
 */
public class IndexingCommand implements Serializable {

    private static final Log log = LogFactory.getLog(IndexingCommand.class);

    private static final long serialVersionUID = 1L;

    public enum Type {
        INSERT, UPDATE, UPDATE_SECURITY, DELETE, UPDATE_DIRECT_CHILDREN,
    }

    public static final String PREFIX = "IxCd-";

    protected String id;

    protected Type type;

    protected boolean sync;

    protected boolean recurse;

    protected String targetDocumentId;

    protected String path;

    protected String repositoryName;

    protected List<String> schemas;

    protected long order;

    protected transient String sessionId;

    protected transient static AtomicLong seq = new AtomicLong(0);

    protected IndexingCommand() {
    }

    /**
     * Create an indexing command
     *
     * @param document the target document
     * @param commandType the type of command
     * @param sync if true the command will be processed on the same thread after transaction completion and the
     *            Elasticsearch index will be refresh
     * @param recurse the command affect the document and all its descendants
     */
    public IndexingCommand(DocumentModel document, Type commandType, boolean sync, boolean recurse) {
        id = PREFIX + seq.incrementAndGet();
        type = commandType;
        this.sync = sync;
        this.recurse = recurse;
        if ((sync && recurse) && commandType != Type.DELETE) {
            // we don't want sync and recursive command
            throw new IllegalArgumentException(
                    "Recurse and synchronous command is not allowed: cmd: " + this + ", doc: " + document);
        }
        if (document == null) {
            throw new IllegalArgumentException("Target document is null for: " + this);
        }
        DocumentModel targetDocument = getValidTargetDocument(document);
        repositoryName = targetDocument.getRepositoryName();
        targetDocumentId = targetDocument.getId();
        sessionId = targetDocument.getSessionId();
        path = targetDocument.getPathAsString();
        if (targetDocumentId == null) {
            throw new IllegalArgumentException("Target document has a null uid: " + this);
        }
    }

    protected DocumentModel getValidTargetDocument(DocumentModel target) {
        if (target.getId() != null) {
            return target;
        }
        if (target.getRef() == null || target.getCoreSession() == null) {
            throw new IllegalArgumentException("Invalid target document: " + target);
        }
        // transient document try to get it from its path
        DocumentRef documentRef = target.getRef();
        log.warn("Creating indexing command on a document with a null id, ref: " + documentRef
                + ", trying to get the docId from its path, activate trace level for more info " + this);
        if (log.isTraceEnabled()) {
            Throwable throwable = new Throwable();
            StringWriter stack = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stack));
            log.trace("You should use a document returned by session.createDocument, stack " + stack.toString());
        }
        return target.getCoreSession().getDocument(documentRef);
    }

    public void attach(CoreSession session) {
        if (!session.getRepositoryName().equals(repositoryName)) {
            throw new IllegalArgumentException(
                    "Invalid session, expected repo: " + repositoryName + " actual: " + session.getRepositoryName());
        }
        sessionId = session.getSessionId();
        assert sessionId != null : "Attach to session with a null sessionId";
    }

    /**
     * Return the document or null if it does not exists anymore.
     *
     * @throws java.lang.IllegalStateException if there is no session attached
     */
    public DocumentModel getTargetDocument() {
        CoreSession session = null;
        if (sessionId != null) {
            session = Framework.getService(CoreSessionService.class).getCoreSession(sessionId);
        }
        if (session == null) {
            throw new IllegalStateException("Command is not attached to a valid session: " + this);
        }
        IdRef idref = new IdRef(targetDocumentId);
        if (!session.exists(idref)) {
            // Doc was deleted : no way we can fetch it
            return null;
        }
        return session.getDocument(idref);
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * @return true if merged
     */
    public boolean merge(IndexingCommand other) {
        if (canBeMerged(other)) {
            merge(other.sync, other.recurse);
            return true;
        }
        return false;
    }

    protected void merge(boolean sync, boolean recurse) {
        this.sync = this.sync || sync;
        this.recurse = this.recurse || recurse;
    }

    protected boolean canBeMerged(IndexingCommand other) {
        if (type != other.type) {
            return false;
        }
        if (type == Type.DELETE) {
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

    public Type getType() {
        return type;
    }

    public String toJSON() throws IOException {
        StringWriter out = new StringWriter();
        JsonFactory factory = new JsonFactory();
        JsonGenerator jsonGen = factory.createGenerator(out);
        toJSON(jsonGen);
        out.flush();
        jsonGen.close();
        return out.toString();
    }

    public void toJSON(JsonGenerator jsonGen) throws IOException {
        jsonGen.writeStartObject();
        jsonGen.writeStringField("id", id);
        jsonGen.writeStringField("type", String.format("%s", type));
        jsonGen.writeStringField("docId", getTargetDocumentId());
        jsonGen.writeStringField("path", path);
        jsonGen.writeStringField("repo", getRepositoryName());
        jsonGen.writeBooleanField("recurse", recurse);
        jsonGen.writeBooleanField("sync", sync);
        jsonGen.writeNumberField("order", getOrder());
        jsonGen.writeEndObject();
    }

    /**
     * Create a command from a JSON string.
     *
     * @throws IllegalArgumentException if json is invalid or command is invalid
     */
    public static IndexingCommand fromJSON(String json) {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        try {
            return fromJSON(mapper.readTree(json));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON: " + json, e);
        }
    }

    public static IndexingCommand fromJSON(JsonNode jsonNode) {
        IndexingCommand cmd = new IndexingCommand();
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = jsonNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            String key = field.getKey();
            JsonNode value = field.getValue();
            if (value.isNull()) {
                continue;
            }
            if ("type".equals(key)) {
                cmd.type = Type.valueOf(value.textValue());
            } else if ("docId".equals(key)) {
                cmd.targetDocumentId = value.textValue();
            } else if ("path".equals(key)) {
                cmd.path = value.textValue();
            } else if ("repo".equals(key)) {
                cmd.repositoryName = value.textValue();
            } else if ("id".equals(key)) {
                cmd.id = value.textValue();
            } else if ("recurse".equals(key)) {
                cmd.recurse = value.booleanValue();
            } else if ("sync".equals(key)) {
                cmd.sync = value.booleanValue();
            }
        }
        if (cmd.targetDocumentId == null) {
            throw new IllegalArgumentException("Document uid is null: " + cmd);
        }
        if (cmd.type == null) {
            throw new IllegalArgumentException("Invalid type: " + cmd);
        }
        return cmd;
    }

    public String getId() {
        return id;
    }

    public String getTargetDocumentId() {
        return targetDocumentId;
    }

    public IndexingCommand clone(DocumentModel newDoc) {
        return new IndexingCommand(newDoc, type, sync, recurse);
    }

    public String[] getSchemas() {
        String[] ret = null;
        if (schemas != null && schemas.size() > 0) {
            ret = schemas.toArray(new String[schemas.size()]);
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
    }

    @Override
    public String toString() {
        try {
            return toJSON();
        } catch (IOException e) {
            return super.toString();
        }
    }

    /**
     * Try to make the command synchronous. Recurse command will stay in async for update.
     */
    public void makeSync() {
        if (!sync) {
            if (!recurse || type == Type.DELETE) {
                sync = true;
                if (log.isDebugEnabled()) {
                    log.debug("Turn command into sync: " + toString());
                }
            }
        }
    }

    // @since 8.3
    public long getOrder() {
        return order;
    }

    // @since 8.3
    public void setOrder(long order) {
        this.order = order;
    }
}
