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
 * $Id: DocumentModelReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.IOException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class DocumentModelReader extends AbstractDocumentReader {

    protected CoreSession session;

    protected boolean inlineBlobs = false;

    protected DocumentModelReader(CoreSession session) {
        this.session = session;
    }

    @Override
    public abstract ExportedDocument read() throws IOException;

    public void close() {
        session = null;
    }

    /**
     * @param inlineBlobs the inlineBlobs to set.
     */
    public void setInlineBlobs(boolean inlineBlobs) {
        this.inlineBlobs = inlineBlobs;
    }

    public boolean getInlineBlobs() {
        return inlineBlobs;
    }

}
