/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.rendering.fm.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.platform.rendering.api.DocumentView;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentTemplate implements TemplateHashModelEx, AdapterTemplateModel {

    protected RootContextModel root; // the root context
    protected final DocumentModel doc;
    //TODO implement a cache


    public DocumentTemplate(RootContextModel ctx, DocumentModel doc) {
        this.doc = doc;
        this.root = ctx;

    }

    //TODO lazy initialization of the context
    public DocumentTemplate(DocumentModel doc) {
        this (FreemarkerEngine.getRootContext(), doc);
    }

    public RootContextModel getRoot() {
        return root;
    }

    @SuppressWarnings("unchecked")
    public Object getAdaptedObject(Class hint) {
        return doc;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public final DocumentView getDocumentView() {
        return root.getThisContext().getDocumentView();
    }

    public final TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) return TemplateModel.NOTHING;
        return root.getObjectWrapper().wrap(obj);
    }

    public TemplateModel get(String key) throws TemplateModelException {
        RenderingContext ctx = root.getThisContext();
        try {
            Object value =  ctx.getDocumentView().get(doc, key, ctx);
            if (value != DocumentView.NULL) {
                return wrap(value);
            }
        } catch(Exception e) {
            throw new TemplateModelException("Failed to get document field: "+key, e);
        }

        // may be a schema name
        DocumentPart part = doc.getPart(key);
        if (part != null) {
            return new ComplexPropertyTemplate(root.getObjectWrapper(), part);
        }
        // ... and 2 special keys
        if ("session".equals(key)) {
            return wrap(getSession());
        } else if ("document".equals(key)) {
            return wrap(doc);
        }
        return null;
    }

    public CoreSession getSession() {
        return CoreInstance.getInstance().getSession(doc.getSessionId());
    }

    /**
     * A doc model is never empty.
     */
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public Collection<String> getRawKeys() {
        List<String> keysCol = new ArrayList<String>();
        keysCol.addAll(root.getThisContext().getDocumentView().keys());
        String[] schemas = doc.getDeclaredSchemas();
        keysCol.addAll(Arrays.asList(schemas));
        keysCol.add("document");
        keysCol.add("session");
        return keysCol;
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        return (TemplateCollectionModel)root.getObjectWrapper().wrap(getRawKeys());
    }


    public Collection<Object> getRawValues() throws TemplateModelException {
        List<Object> values = new ArrayList<Object>();
        try {
            DocumentView view = root.getThisContext().getDocumentView();
            for (String key : view.keys()) {
                values.add(view.get(doc, key, root.getThisContext()));
            }
            for (DocumentPart part : doc.getParts()) {
                values.add(part.getValue());
            }
            values.add(doc);
            values.add(getSession());

        } catch (Exception e) {
            throw new TemplateModelException("failed to fetch field values", e);
        }
        return values;
    }


    public TemplateCollectionModel values() throws TemplateModelException {
        return (TemplateCollectionModel)root.getObjectWrapper().wrap(getRawValues());
    }

    public int size() throws TemplateModelException {
        return root.getThisContext().getDocumentView().size() + doc.getDeclaredSchemas().length + 2;
    }

}
