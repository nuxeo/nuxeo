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

package org.nuxeo.ecm.platform.rendering.wiki.filters;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.wiki.WikiExpression;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializerHandler;
import org.wikimodel.wem.WikiParameters;
import org.wikimodel.wem.WikiParameter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentExpression implements WikiExpression {

    public void eval(WikiParameters params, WikiSerializerHandler serializer) throws Exception {
        RenderingContext ctx = serializer.getContext();
        if (ctx == null) {
            return;
        }
        int size = params.getSize();
        if (size < 1) {
            return;
        }
        WikiParameter param = params.getParameter(0);
        String value = param.getValue();
        if (size == 1 && value == null) {
            serializer.print(getProperty(ctx, param.getKey()));
        } else {
            serializer.print(getProperty(ctx, param.getValue()));
        }
    }

    public void evalInline(WikiParameters params,
            WikiSerializerHandler serializer) throws Exception {
        eval(params, serializer);
    }

    public String getName() {
        return "doc";
    }

    protected static String getProperty(RenderingContext ctx, String key) throws Exception {
        Property prop = ctx.getDocument().getProperty(key);
        return prop.getValue().toString();
    }

}
