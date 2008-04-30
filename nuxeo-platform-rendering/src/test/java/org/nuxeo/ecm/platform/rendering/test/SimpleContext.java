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

package org.nuxeo.ecm.platform.rendering.test;

import java.io.OutputStream;
import java.io.Writer;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.DocumentContextView;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;




/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleContext implements RenderingContext {

    DocumentModel doc;
    Writer writer;

    /**
     *
     */
    public SimpleContext(DocumentModel doc, Writer writer) {
        this.doc =doc;
        this.writer = writer;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public CoreSession getSession() {
        return CoreInstance.getInstance().getSession(doc.getSessionId());
    }

    public OutputStream getOut() {
        throw new UnsupportedOperationException("use getWriter");
    }

    public Writer getWriter() {
        return writer;
    }

    public RenderingContext getParentContext() {
        return null;
    }

    public RenderingContextView getView() {
        return DocumentContextView.DEFAULT;
    }

}
