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
 *     Nuxeo - initial API and implementation
 *
 * $Id: AbstractDocumentReader.java 30477 2008-02-22 10:02:15Z dmihalache $
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractDocumentReader implements DocumentReader {

    private static final Log log = LogFactory.getLog(AbstractDocumentReader.class);

    // this abstract method is needed
    @Override
    public abstract ExportedDocument read() throws IOException;

    @Override
    public ExportedDocument[] read(int count) throws IOException {
        List<ExportedDocument> docs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ExportedDocument doc = read();
            if (doc == null) {
                break;
            }

            /* NXP-1688 Rux: no ID, it should be a OS folder and not an exported one */
            if (doc.getId() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding document to be transformed (path): " + doc.getPath());
                }
                docs.add(doc);
            } else {
                log.warn("no ID for document, won't add " + doc);
            }
        }
        if (docs.isEmpty()) {
            return null;
        }
        return docs.toArray(new ExportedDocument[docs.size()]);
    }

}
