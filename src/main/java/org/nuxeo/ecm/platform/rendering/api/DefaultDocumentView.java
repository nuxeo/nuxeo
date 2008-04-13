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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultDocumentView implements DocumentView {


    protected Map<String, DocumentViewField> fields;

    protected DefaultDocumentView() {
        fields = new HashMap<String, DocumentViewField>();
        initialize();
    }

    protected DefaultDocumentView(Map<String, DocumentViewField> fields) {
        this.fields = fields == null ? new HashMap<String, DocumentViewField>() : fields;
    }

    protected void initialize() {
        addField(ID);
        addField(NAME);
        addField(PATH);
        addField(TYPE);
        addField(PARTS);
        addField(SID);
        addField(REPOSITORY);

        addField(SCHEMAS);
        addField(FACETS);
        addField(SCHEMAS);
        addField(LOCKED);
        addField(IS_FOLDER);
        addField(TITLE);
        addField(AUTHOR);
        addField(CREATED);
        addField(MODIFIED);
        addField(CONTENT);

        addField(PARENT);
        addField(CHILDREN);

    }

    public final void addField(DocumentViewField field) {
        this.fields.put(field.getName(), field);
    }

    public final void addFields(Collection<DocumentViewField> fields) {
        for (DocumentViewField field : fields) {
            this.fields.put(field.getName(), field);
        }
    }

    public final void removeField(String name) {
        this.fields.remove(name);
    }

    public DocumentViewField getField(String name) {
        return fields.get(name);
    }

    public Object get(DocumentModel doc, String name, RenderingContext ctx) throws Exception {
        DocumentViewField field = fields.get(name);
        if (field != null) {
            return field.getValue(doc, ctx);
        }
        return NULL;
    }

    public Collection<String> keys() {
        return fields.keySet();
    }

    public Map<String, DocumentViewField> getFields() {
        return fields;
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public int size() {
        return fields.size();
    }


    protected static final DocumentViewField ID = new DocumentViewField() {
        public final String getName() {
            return "id";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getId();
        }
    };

    protected static final DocumentViewField NAME = new DocumentViewField() {
        public final String getName() {
            return "name";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getName();
        }
    };

    protected static final DocumentViewField PATH = new DocumentViewField() {
        public final String getName() {
            return "path";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getPathAsString();
        }
    };

    protected static DocumentViewField TYPE = new DocumentViewField() {
        public final String getName() {
            return "type";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getType();
        }
    };

    protected static DocumentViewField SCHEMAS = new DocumentViewField() {
        public final String getName() {
            return "schemas";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getDeclaredSchemas();
        }
    };

    protected static final DocumentViewField FACETS = new DocumentViewField() {
        public final String getName() {
            return "facets";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getDeclaredFacets();
        }
    };

    protected static final DocumentViewField STATE = new DocumentViewField() {
        public final String getName() {
            return "state";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getCurrentLifeCycleState();
        }
    };

    protected static final DocumentViewField LOCKED = new DocumentViewField() {
        public final String getName() {
            return "isLocked";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.isLocked();
        }
    };

    protected static final DocumentViewField IS_FOLDER = new DocumentViewField() {
        public final String getName() {
            return "isFolder";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getDeclaredFacets().contains("Folderish");
        }
    };

    protected static final DocumentViewField TITLE = new DocumentViewField() {
        public String getName() {
            return "title";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getTitle();
        }
    };

    protected static final DocumentViewField AUTHOR = new DocumentViewField() {
        public String getName() {
            return "author";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getPart("dublincore").get("creator").getValue();
        }
    };

    protected static final DocumentViewField CREATED = new DocumentViewField() {
        public String getName() {
            return "created";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            Calendar cal = (Calendar) doc.getPart("dublincore").get("created").getValue();
            return cal == null ? null : cal.getTime();
        }
    };

    protected static final DocumentViewField MODIFIED = new DocumentViewField() {
        public String getName() {
            return "modified";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            Calendar cal = (Calendar) doc.getPart("dublincore").get("modified").getValue();
            return cal == null ? null : cal.getTime();
        }
    };

    protected static final DocumentViewField CONTENT = new DocumentViewField() {
        public String getName() {
            return "content";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            DocumentPart part = (DocumentPart) doc.getPart("file");
            Blob blob = null;
            if (part != null) {
                blob = (Blob) part.get("content").getValue();
            }
            return blob != null ? blob : new StringBlob("", "text/plain");
        }
    };

    protected static final DocumentViewField PARTS = new DocumentViewField() {
        public String getName() {
            return "parts";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getParts();
        }
    };

    protected static final DocumentViewField SID = new DocumentViewField() {
        public String getName() {
            return "sessionId";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getSessionId();
        }
    };

    protected static final DocumentViewField REPOSITORY = new DocumentViewField() {
        public String getName() {
            return "repository";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getRepositoryName();
        }
    };

    protected static final DocumentViewField PARENT = new DocumentViewField() {
        public String getName() {
            return "parent";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            CoreSession session = CoreInstance.getInstance().getSession(doc.getSessionId());
            return session.getParentDocument(doc.getRef());
        }
    };


    protected static final DocumentViewField CHILDREN = new DocumentViewField() {
        public String getName() {
            return "children";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            CoreSession session = CoreInstance.getInstance().getSession(doc.getSessionId());
            return session.getChildren(doc.getRef());
        }
    };


}
