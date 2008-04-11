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

import org.nuxeo.ecm.platform.rendering.api.RenderingContext;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContextDocumentTemplate extends DocumentTemplate {

    protected ContextDocumentTemplate zuper;
    protected RenderingContext ctx;

    public ContextDocumentTemplate(RootContextModel root, ContextDocumentTemplate zuper, RenderingContext ctx) {
        super (root, ctx.getDocument());
        this.ctx = ctx;
        this.zuper = zuper;
    }

    public RenderingContext getContext() {
        return ctx;
    }

    public ContextDocumentTemplate getSuper() {
        return zuper;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        TemplateModel result = super.get(key);
        if (result == null)
            if ("super".equals(key)) {
                return wrap(zuper);
            } else if ("this".equals(key)) {
                return wrap(this);
            }
        return result;
    }


    public Collection<String> getRawKeys() {
        Collection<String> keysCol = super.getRawKeys();
        keysCol.add("this");
        keysCol.add("super");
        return keysCol;
    }

    public Collection<Object> getRawValues() throws TemplateModelException {
        Collection<Object> values = super.getRawValues();
        values.add(this);
        values.add(zuper);
        return values;
    }

}
