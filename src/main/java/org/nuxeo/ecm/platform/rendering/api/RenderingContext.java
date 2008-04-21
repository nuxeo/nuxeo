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

import java.io.OutputStream;
import java.io.Writer;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;


/**
 * The context of rendering is the bridge between the rendering engine and the application logic.
 * The rendering context is describing a document rendering task and it is used by the rendering engine
 * to interact with the application layer.
 * <p>
 * Example of usage:
 * <p><pre>
 * RenderingContext ctx = new MyRenderingContext(doc, env);
 * ctx.setWriter(new FileWriter("/path/to/my/file"))
 * engine.render(ctx);
 * </pre>
 * <p>
 * If the current template is using the directive <code><@renderChild/></code> then
 * the rendering engine will call createChildContext() on the current context to create a new context
 * to be used by the sub-rendering process.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RenderingContext {

    DocumentModel getDocument();

    CoreSession getSession();

    OutputStream getOut();

    Writer getWriter();

    RenderingContext getParentContext();

    /**
     * Must never return null. If no special document view is used
     * you may return the defualt one {@link RenderingContextView#DEFAULT}
     * @return
     */
    RenderingContextView getView();

}
