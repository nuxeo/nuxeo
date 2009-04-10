/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Session.Job;
import org.nuxeo.runtime.api.Framework;

/**
 * Job that converts a set of document's binaries to text, for the fulltext
 * index.
 *
 * @author Florent Guillaume
 */
public class BinaryConversionJob implements Job {

    private static final Log log = LogFactory.getLog(BinaryConversionJob.class);

    private static final long serialVersionUID = 1L;

    private static final String ANY2TEXT = "any2text";

    /** Info, must be serializable */
    public final Map<Serializable, Map<Serializable, String>> binariesInfo;

    /** @param binariesInfo must be serializable */
    public BinaryConversionJob(
            Map<Serializable, Map<Serializable, String>> binariesInfo) {
        this.binariesInfo = binariesInfo;
    }

    /**
     * Does the conversion and stores the result in the document.
     */
    public void run(Session session, boolean save) {
        ConversionService conversionService;
        try {
            conversionService = Framework.getService(ConversionService.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        if (conversionService == null) {
            return;
        }
        try {
            for (Serializable docId : binariesInfo.keySet()) {
                Map<Serializable, String> map = binariesInfo.get(docId);
                Node document = session.getNodeById(docId);
                if (document == null) {
                    continue;
                }
                List<String> strings = new LinkedList<String>();
                for (Serializable nodeId : map.keySet()) {
                    String name = map.get(nodeId);
                    Node node = session.getNodeById(nodeId);
                    if (node == null) {
                        // document is gone
                        continue;
                    }
                    Binary binary = (Binary) node.getSimpleProperty(name).getValue();
                    if (binary == null || binary.getLength() == 0) {
                        continue;
                    }

                    // find mime-type (heuristic on schema)
                    String mimeType;
                    try {
                        mimeType = (String) node.getSimpleProperty("mime-type").getValue();
                    } catch (IllegalArgumentException e) {
                        // no mime-type column
                        mimeType = null;
                    } catch (ClassCastException e) {
                        // not a string
                        mimeType = null;
                    }

                    // convert
                    try {
                        Blob blob = new InputStreamBlob(binary.getStream(),
                                mimeType);
                        SimpleBlobHolder bh = new SimpleBlobHolder(blob);
                        BlobHolder result = conversionService.convert(ANY2TEXT,
                                bh, null);
                        if (result == null) {
                            continue;
                        }
                        blob = result.getBlob();
                        if (blob == null) {
                            continue;
                        }
                        strings.add(new String(blob.getByteArray(), "UTF-8"));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        continue;
                    }
                }

                // store the extracted text
                document.setSingleProperty(Model.FULLTEXT_BINARYTEXT_PROP,
                        StringUtils.join(strings, " "));
            }
            if (save) {
                session.save();
            }
        } catch (StorageException e) {
            log.error(e.getMessage(), e);
            return;
        }
    }

}
