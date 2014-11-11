/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;

/**
 * Base class to build views for Document oriented contexts (contexts that are
 * bound to a document)
 * <p>
 * Note that this class cannot be used with contexts for which the
 * {@link RenderingContext#getDocument()} method is returning null.
 * <p>
 * This implementation ensure that the context argument is never used so it can
 * be used outside the scope of a rendering context to get a view over the
 * document.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultDocumentView implements DocumentView {

    // Must be returned by get() method when the key is unknown since the caller
    // should be able to
    // treat differently a key hit that returned null from a key that is not
    // known by this view
    public static final Object UNKNOWN = new Object();

    public interface Field {
        String getName();

        Object getValue(DocumentModel doc) throws Exception;
    }

    protected final Map<String, Field> fields;

    public DefaultDocumentView() {
        fields = new HashMap<String, Field>();
        initialize();
    }

    public DefaultDocumentView(Map<String, Field> fields) {
        this.fields = fields == null ? new HashMap<String, Field>() : fields;
    }

    protected void initialize() {
        addField(SESSION);

        addField(ID);
        addField(NAME);
        addField(PATH);
        addField(TYPE);
        addField(PARTS);
        addField(SID);
        addField(REPOSITORY);

        addField(SCHEMAS);
        addField(FACETS);
        addField(LOCKED);
        addField(LIFE_CYCLE_STATE);
        addField(LIFE_CYCLE_POLICY);
        addField(ALLOWED_STATE_TRANSITIONS);
        addField(IS_FOLDER);
        addField(TITLE);
        addField(AUTHOR);
        addField(CREATED);
        addField(MODIFIED);
        addField(CONTENT);

        addField(PARENT);
        addField(CHILDREN);
        addField(REF);
        addField(VERSIONS);
        addField(PROXIES);
        addField(VERSION_LABEL);
        addField(SOURCE_ID);
    }

    public final void addField(Field field) {
        fields.put(field.getName(), field);
    }

    public final void addFields(Collection<Field> fields) {
        for (Field field : fields) {
            this.fields.put(field.getName(), field);
        }
    }

    public final void removeField(String name) {
        fields.remove(name);
    }

    public Field getField(String name) {
        return fields.get(name);
    }

    public Object get(DocumentModel doc, String name) throws Exception {
        Field field = fields.get(name);
        if (field != null) {
            return field.getValue(doc);
        }
        // may be a a property xpath
        if (name.indexOf(':') > -1) {
            return doc.getProperty(name);
        }
        // may be a schema name
        DocumentPart part = doc.getPart(name);
        if (part != null) {
            return part;
        }
        return UNKNOWN;
    }

    public Collection<String> keys(DocumentModel doc) {
        Collection<String> keys = new ArrayList<String>(fields.keySet());
        keys.addAll(Arrays.asList(doc.getDeclaredSchemas()));
        return keys;
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public int size(DocumentModel doc) {
        return fields.size() + doc.getDeclaredSchemas().length;
    }

    protected static final Field SESSION = new Field() {
        public final String getName() {
            return "session";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return CoreInstance.getInstance().getSession(doc.getSessionId());
        }
    };

    protected static final Field ID = new Field() {
        public final String getName() {
            return "id";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getId();
        }
    };

    protected static final Field NAME = new Field() {
        public final String getName() {
            return "name";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getName();
        }
    };

    protected static final Field PATH = new Field() {
        public final String getName() {
            return "path";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getPathAsString();
        }
    };

    protected static final Field TYPE = new Field() {
        public final String getName() {
            return "type";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getType();
        }
    };

    protected static final Field SCHEMAS = new Field() {
        public final String getName() {
            return "schemas";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getDeclaredSchemas();
        }
    };

    protected static final Field FACETS = new Field() {
        public final String getName() {
            return "facets";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getDeclaredFacets();
        }
    };

    protected static final Field STATE = new Field() {
        public final String getName() {
            return "state";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getCurrentLifeCycleState();
        }
    };

    protected static final Field LOCKED = new Field() {
        public final String getName() {
            return "isLocked";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.isLocked();
        }
    };

    protected static final Field LIFE_CYCLE_STATE = new Field() {
        public final String getName() {
            return "lifeCycleState";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getCurrentLifeCycleState();
        }
    };

    protected static final Field LIFE_CYCLE_POLICY = new Field() {
        public final String getName() {
            return "lifeCyclePolicy";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getLifeCyclePolicy();
        }
    };

    protected static final Field ALLOWED_STATE_TRANSITIONS = new Field() {
        public final String getName() {
            return "allowedStateTransitions";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getAllowedStateTransitions();
        }
    };

    protected static final Field IS_FOLDER = new Field() {
        public final String getName() {
            return "isFolder";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getDeclaredFacets().contains("Folderish");
        }
    };

    protected static final Field TITLE = new Field() {
        public String getName() {
            return "title";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getTitle();
        }
    };

    protected static final Field AUTHOR = new Field() {
        public String getName() {
            return "author";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getPart("dublincore").get("creator").getValue();
        }
    };

    protected static final Field CREATED = new Field() {
        public String getName() {
            return "created";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            Calendar cal = (Calendar) doc.getPart("dublincore").get("created").getValue();
            return cal == null ? null : cal.getTime();
        }
    };

    protected static final Field MODIFIED = new Field() {
        public String getName() {
            return "modified";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            Calendar cal = (Calendar) doc.getPart("dublincore").get("modified").getValue();
            return cal == null ? null : cal.getTime();
        }
    };

    protected static final Field CONTENT = new Field() {
        public String getName() {
            return "content";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            DocumentPart part = doc.getPart("file");
            Blob blob = null;
            if (part != null) {
                blob = (Blob) part.get("content").getValue();
            }
            return blob != null ? blob : new StringBlob("", "text/plain");
        }
    };

    protected static final Field PARTS = new Field() {
        public String getName() {
            return "parts";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getParts();
        }
    };

    protected static final Field SID = new Field() {
        public String getName() {
            return "sessionId";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getSessionId();
        }
    };

    protected static final Field REPOSITORY = new Field() {
        public String getName() {
            return "repository";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getRepositoryName();
        }
    };

    protected static final Field PARENT = new Field() {
        public String getName() {
            return "parent";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            CoreSession session = CoreInstance.getInstance().getSession(
                    doc.getSessionId());
            return session.getParentDocument(doc.getRef());
        }
    };

    protected static final Field CHILDREN = new Field() {
        public String getName() {
            return "children";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            CoreSession session = CoreInstance.getInstance().getSession(
                    doc.getSessionId());
            return session.getChildren(doc.getRef());
        }
    };

    protected static final Field REF = new Field() {
        public String getName() {
            return "ref";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getRef();
        }
    };

    protected static final Field VERSIONS = new Field() {
        public String getName() {
            return "versions";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return CoreInstance.getInstance().getSession(doc.getSessionId()).getVersions(
                    doc.getRef());
        }
    };

    protected static final Field PROXIES = new Field() {
        public String getName() {
            return "proxies";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return CoreInstance.getInstance().getSession(doc.getSessionId()).getProxies(
                    doc.getRef(), null);
        }
    };

    protected static final Field VERSION_LABEL = new Field() {
        public String getName() {
            return "versionLabel";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getVersionLabel();
        }
    };

    protected static final Field SOURCE_ID = new Field() {
        public String getName() {
            return "sourceId";
        }

        public Object getValue(DocumentModel doc) throws Exception {
            return doc.getSourceId();
        }
    };

    /**
     * The singleton instance that should be used by clients. Warn that this
     * static field must be defined at the end of the class after any other
     * field class since it will try to register these fields (otherwise fields
     * will not be defined yet at the time of the initialization of that static
     * member
     */
    public static final DefaultDocumentView DEFAULT = new DefaultDocumentView();

}
