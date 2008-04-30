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

package org.nuxeo.ecm.platform.rendering.wiki;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.RenderingTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.filters.DocumentExpression;
import org.nuxeo.ecm.platform.rendering.wiki.filters.FreemarkerMacro;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiTransformer implements RenderingTransformer {

    protected WikiSerializer serializer;


    public WikiTransformer() {
        this(new WikiSerializer());
    }

    public WikiTransformer(WikiSerializer serializer) {
        this.serializer = serializer;
        this.serializer.registerMacro(new FreemarkerMacro());
        this.serializer.registerExpression(new DocumentExpression());
    }

    public WikiSerializer getSerializer() {
        return serializer;
    }

    public void transform(Reader reader, Writer writer, RenderingContext ctx)
            throws RenderingException {
        try {
            serializer.serialize(reader, writer, ctx);
        } catch (Exception e) {
            throw new RenderingException(e);
        }
    }

    public void transform(URL url, Writer writer, RenderingContext ctx)
            throws RenderingException {
        Reader reader = null;
        try {
            InputStream in = url.openStream();
            reader = new BufferedReader(new InputStreamReader(in));
            transform(reader, writer, ctx);
        } catch (Exception e) {
            throw new RenderingException(e);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception e) {}
            }
        }
    }

}
