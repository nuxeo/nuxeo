/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.rendering.fm.adapters.SchemaTemplate;

/**
 * Base class to build views for Document oriented contexts (contexts that are bound to a document).
 * <p>
 * Note that this class cannot be used with contexts for which the {@link RenderingContext#getDocument()} method is
 * returning null.
 * <p>
 * This implementation ensure that the context argument is never used so it can be used outside the scope of a rendering
 * context to get a view over the document.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultDocumentView implements DocumentView {

    // Must be returned by get() method when the key is unknown since the caller
    // should be able to
    // treat differently a key hit that returned null from a key that is not
    // known by this view
    public static final Object UNKNOWN = new Object();

    public interface Field {
        String getName();

        Object getValue(DocumentModel doc);
    }

    protected final Map<String, Field> fields;

    public DefaultDocumentView() {
        fields = new HashMap<>();
        initialize();
    }

    public DefaultDocumentView(Map<String, Field> fields) {
        this.fields = fields == null ? new HashMap<>() : fields;
    }

    protected void initialize() {
        addField(SESSION);

        addField(ID);
        addField(NAME);
        addField(PATH);
        addField(TYPE);
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

    @Override
    public Object get(DocumentModel doc, String name) throws PropertyException {
        Field field = fields.get(name);
        if (field != null) {
            return field.getValue(doc);
        }
        // may be a a property xpath
        if (name.indexOf(':') > -1) {
            return doc.getProperty(name);
        }
        // may be a schema name
        if (doc.hasSchema(name)) {
            return new SchemaTemplate.DocumentSchema(doc, name);
        }
        return UNKNOWN;
    }

    @Override
    public Collection<String> keys(DocumentModel doc) {
        Collection<String> keys = new ArrayList<>(fields.keySet());
        keys.addAll(Arrays.asList(doc.getSchemas()));
        return keys;
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public int size(DocumentModel doc) {
        return fields.size() + doc.getSchemas().length;
    }

    protected static final Field SESSION = new Field() {
        @Override
        public final String getName() {
            return "session";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getCoreSession();
        }
    };

    protected static final Field ID = new Field() {
        @Override
        public final String getName() {
            return "id";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getId();
        }
    };

    protected static final Field NAME = new Field() {
        @Override
        public final String getName() {
            return "name";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getName();
        }
    };

    protected static final Field PATH = new Field() {
        @Override
        public final String getName() {
            return "path";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getPathAsString();
        }
    };

    protected static final Field TYPE = new Field() {
        @Override
        public final String getName() {
            return "type";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getType();
        }
    };

    protected static final Field SCHEMAS = new Field() {
        @Override
        public final String getName() {
            return "schemas";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getSchemas();
        }
    };

    protected static final Field FACETS = new Field() {
        @Override
        public final String getName() {
            return "facets";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getFacets();
        }
    };

    protected static final Field STATE = new Field() {
        @Override
        public final String getName() {
            return "state";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getCurrentLifeCycleState();
        }
    };

    protected static final Field LOCKED = new Field() {
        @Override
        public final String getName() {
            return "isLocked";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.isLocked();
        }
    };

    protected static final Field LIFE_CYCLE_STATE = new Field() {
        @Override
        public final String getName() {
            return "lifeCycleState";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getCurrentLifeCycleState();
        }
    };

    protected static final Field LIFE_CYCLE_POLICY = new Field() {
        @Override
        public final String getName() {
            return "lifeCyclePolicy";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getLifeCyclePolicy();
        }
    };

    protected static final Field ALLOWED_STATE_TRANSITIONS = new Field() {
        @Override
        public final String getName() {
            return "allowedStateTransitions";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getAllowedStateTransitions();
        }
    };

    protected static final Field IS_FOLDER = new Field() {
        @Override
        public final String getName() {
            return "isFolder";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getFacets().contains(FacetNames.FOLDERISH);
        }
    };

    protected static final Field TITLE = new Field() {
        @Override
        public String getName() {
            return "title";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getTitle();
        }
    };

    protected static final Field AUTHOR = new Field() {
        @Override
        public String getName() {
            return "author";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            try {
                return doc.getPropertyValue("dc:creator");
            } catch (PropertyNotFoundException e) {
                // ignore
            }
            return null;
        }
    };

    protected static final Field CREATED = new Field() {
        @Override
        public String getName() {
            return "created";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            try {
                Calendar cal = (Calendar) doc.getPropertyValue("dc:created");
                if (cal != null) {
                    return cal.getTime();
                }
            } catch (PropertyNotFoundException e) {
                // ignore
            }
            return null;
        }
    };

    protected static final Field MODIFIED = new Field() {
        @Override
        public String getName() {
            return "modified";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            try {
                Calendar cal = (Calendar) doc.getPropertyValue("dc:modified");
                if (cal != null) {
                    return cal.getTime();
                }
            } catch (PropertyNotFoundException e) {
                // ignore
            }
            return null;
        }
    };

    protected static final Field CONTENT = new Field() {
        @Override
        public String getName() {
            return "content";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            try {
                Blob blob = (Blob) doc.getPropertyValue("file:content");
                if (blob != null) {
                    return blob;
                }
            } catch (PropertyNotFoundException e) {
                // ignore
            }
            return Blobs.createBlob("");
        }
    };

    protected static final Field SID = new Field() {
        @Override
        public String getName() {
            return "sessionId";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getSessionId();
        }
    };

    protected static final Field REPOSITORY = new Field() {
        @Override
        public String getName() {
            return "repository";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getRepositoryName();
        }
    };

    protected static final Field PARENT = new Field() {
        @Override
        public String getName() {
            return "parent";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            CoreSession session = doc.getCoreSession();
            return session.getParentDocument(doc.getRef());
        }
    };

    protected static final Field CHILDREN = new Field() {
        @Override
        public String getName() {
            return "children";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            CoreSession session = doc.getCoreSession();
            return session.getChildren(doc.getRef());
        }
    };

    protected static final Field REF = new Field() {
        @Override
        public String getName() {
            return "ref";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getRef();
        }
    };

    protected static final Field VERSIONS = new Field() {
        @Override
        public String getName() {
            return "versions";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            CoreSession session = doc.getCoreSession();
            return session.getVersions(doc.getRef());
        }
    };

    protected static final Field PROXIES = new Field() {
        @Override
        public String getName() {
            return "proxies";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            CoreSession session = doc.getCoreSession();
            return session.getProxies(doc.getRef(), null);
        }
    };

    protected static final Field VERSION_LABEL = new Field() {
        @Override
        public String getName() {
            return "versionLabel";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getVersionLabel();
        }
    };

    protected static final Field SOURCE_ID = new Field() {
        @Override
        public String getName() {
            return "sourceId";
        }

        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getSourceId();
        }
    };

    /**
     * The singleton instance that should be used by clients. Warn that this static field must be defined at the end of
     * the class after any other field class since it will try to register these fields (otherwise fields will not be
     * defined yet at the time of the initialization of that static member
     */
    public static final DefaultDocumentView DEFAULT = new DefaultDocumentView();

}
