/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
public abstract class AbstractDocumentReader implements DocumentReader {

    private static final Log log = LogFactory.getLog(AbstractDocumentReader.class);

    // this abstract method is needed
    public abstract ExportedDocument read() throws IOException;

    public ExportedDocument[] read(int count) throws IOException {
        List<ExportedDocument> docs = new ArrayList<ExportedDocument>(count);
        for (int i = 0; i < count; i++) {
            ExportedDocument doc = read();
            if (doc  == null) {
                break;
            }

            /*NXP-1688 Rux: no ID, it should be a OS folder and not an exported one*/
            if (doc.getId() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding document to be transformed (path): "
                            + doc.getPath());
                }
                docs.add(doc);
            }
            else {
                log.warn("no ID for document, won't add " + doc);
            }
        }
        if (docs.isEmpty()) {
            return null;
        }
        return docs.toArray(new ExportedDocument[docs.size()]);
    }

}
