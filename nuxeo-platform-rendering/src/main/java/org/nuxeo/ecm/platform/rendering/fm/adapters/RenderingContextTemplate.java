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
import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;
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
public class RenderingContextTemplate implements TemplateHashModelEx, AdapterTemplateModel {

    protected final FreemarkerEngine engine;
    protected final RenderingContext ctx;

    public RenderingContextTemplate(FreemarkerEngine engine, RenderingContext ctx) {
        this.ctx = ctx;
        this.engine = engine;
    }

    public final FreemarkerEngine getEngine() {
        return engine;
    }

    public final RenderingContext getContext() {
        return ctx;
    }

    @SuppressWarnings("unchecked")
    public Object getAdaptedObject(Class hint) {
        return ctx;
    }

    public final RenderingContext getParentContext() {
        return ctx.getParentContext();
    }

    public final boolean isRoot() {
        return ctx.getParentContext() == null;
    }

    public final DocumentObjectWrapper getObjectWrapper() {
        return engine.getObjectWrapper();
    }

    public final RenderingContextView getDocumentView() {
        return ctx.getView();
    }

    public final DocumentModel getDocument() {
        return ctx.getDocument();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            // try first the contextual document view
            Object value = ctx.getView().get(key, ctx);
            if (value != RenderingContextView.UNKNOWN) {
                return wrap(value);
            }
        } catch (TemplateModelException e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateModelException("Failed to get field: "+key, e);
        }
        return null;
    }

    /**
     * A doc model is never empty.
     */
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        return (TemplateCollectionModel)engine.getObjectWrapper().wrap(getRawKeys());
    }

    public Collection<String> getRawKeys() {
        return ctx.getView().keys(ctx);
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        Collection<String> keys = getRawKeys();
        Collection<Object> values = new ArrayList<Object>();
        for (String key : keys) {
            values.add(get(key));
        }
        return (TemplateCollectionModel)engine.getObjectWrapper().wrap(values);
    }

    public int size() throws TemplateModelException {
        return ctx.getView().size(ctx);
    }

    public final TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) {
            return TemplateModel.NOTHING;
        }
        return engine.getObjectWrapper().wrap(obj);
    }

}
