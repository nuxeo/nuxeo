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

import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.fm.DocumentView;
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
public class RootContextModel implements TemplateHashModelEx, AdapterTemplateModel {

    private FreemarkerEngine engine;
    private ContextDocumentTemplate doc; // the current document


    public RootContextModel(FreemarkerEngine engine, RenderingContext ctx) {
        doc = new ContextDocumentTemplate(this, null, ctx);
        this.engine = engine;
    }

    public final FreemarkerEngine getEngine() {
        return engine;
    }

    public final ContextDocumentTemplate getThis() {
        return doc;
    }

    public final RenderingContext getThisContext() {
        return doc.getContext();
    }

    public final ContextDocumentTemplate getSuperDocument() {
        return doc.getSuper();
    }

    public final RenderingContext getSuperRenderingContext() {
        ContextDocumentTemplate zuper = doc.getSuper();
        if (zuper != null) {
            return zuper.getContext();
        }
        return null;
    }

    public final boolean hasSuperContext() {
        return doc.getSuper() != null;
    }

    public final DocumentObjectWrapper getObjectWrapper() {
        return engine.getObjectWrapper();
    }

    public final DocumentView getDocumentView() {
        return engine.getDocumentView();
    }

    public final DocumentModel getDocument() {
        return doc.getDocument();
    }

    public final ContextDocumentTemplate pushContext() {
        RenderingContext subCtx = doc.getContext().createChildContext();
        if (subCtx != null) {
            ContextDocumentTemplate subDoc = new ContextDocumentTemplate(this, doc, subCtx);
            doc = subDoc;
            return subDoc;
        }
        return null;
    }

    public final ContextDocumentTemplate popContext() {
        ContextDocumentTemplate zuper = doc.getSuper();
        if (zuper != null) {
            ContextDocumentTemplate subDoc = doc;
            doc = zuper;
            return subDoc;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object getAdaptedObject(Class hint) {
        return doc;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        Object result = doc.get(key);
        if (result == null && "env".equals(key)) {
            return new EnvironmentProviderTemplate(this);
        }
        return engine.getObjectWrapper().wrap(result);
    }

    /**
     * A doc model is never empty.
     */
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        Collection<String> keys = doc.getRawKeys();
        keys.add("env");
        return (TemplateCollectionModel)engine.getObjectWrapper().wrap(keys);
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        //TODO optimize this
        Collection<Object> values = doc.getRawValues();
        values.add(new EnvironmentProviderTemplate(this)); // cache this
        return (TemplateCollectionModel)engine.getObjectWrapper().wrap(values);
    }

    public int size() throws TemplateModelException {
        return doc.size() + 1; // add the "env" too
    }

}
