/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 */
public abstract class DocumentModelReader extends AbstractDocumentReader {

    protected CoreSession session;

    protected boolean inlineBlobs = false;

    protected DocumentModelReader(CoreSession session) {
        this.session = session;
    }

    @Override
    public abstract ExportedDocument read() throws IOException;

    @Override
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
