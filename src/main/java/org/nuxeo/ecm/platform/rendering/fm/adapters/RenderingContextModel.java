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

import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RenderingContextModel extends RenderingContextTemplate {


    public RenderingContextModel(FreemarkerEngine engine, RenderingContext ctx) {
        super (engine, ctx);
    }

    public final RenderingContext pushContext() {
        RenderingContext subCtx = ctx.getChildContext();
        if (subCtx != null) {
            ctx = subCtx;
            return ctx;
        }
        return null;
    }

    public final RenderingContext popContext() {
        RenderingContext superCtx = ctx.getParentContext();
        if (superCtx != null) {
            ctx = superCtx;
            return ctx;
        }
        return null;
    }


    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if ("this".equals(key)) {
                return new RenderingContextTemplate(engine, ctx);
            } else if ("super".equals(key)) {
                RenderingContext superCtx = ctx.getParentContext();
                if (superCtx != null) {
                    return new RenderingContextTemplate(engine, superCtx);
                } else {
                    return null;
                }
            } else {
                // try second the shared document view
                Object value = engine.getSharedDocumentView().get(key, ctx);
                if (value != RenderingContextView.UNKNOWN) {
                    return wrap(value);
                }
            }
        } catch (TemplateModelException e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateModelException("Failed to get field: "+key, e);
        }
        return null;
    }

    public Collection<String> getRawKeys() {
        Collection<String> keys = super.getRawKeys();
        keys.addAll(engine.getSharedDocumentView().keys(ctx));
        return keys;
    }

    public int size() throws TemplateModelException {
        return ctx.getDocumentView().size(ctx) + engine.getSharedDocumentView().size(ctx);
    }

}
