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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.blobs;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.platform.transform.api.TransformException;
import org.nuxeo.ecm.platform.transform.api.TransformServiceDelegate;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

/**
 * Blob extractor that leverages Nuxeo transform service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class NXTransformBlobExtractor implements BlobExtractor {

    private static final long serialVersionUID = -4204325669629533663L;

    private static final Log log = LogFactory.getLog(NXTransformBlobExtractor.class);

    private static final int BYTE_ORDER_MARK_CHAR = 0xFEFF;

    private static TransformServiceCommon transformService;

    private static TransformServiceCommon getTransformService()
            throws TransformException {
        if (transformService == null) {
            try {
                transformService = TransformServiceDelegate.getLocalTransformService();
            } catch (TransformException te) {
                // Fallback with remote call
                log.debug("Cannot find local transform service. Trying to perform a remote lookup");
                transformService = TransformServiceDelegate.getRemoteTransformService();
            }
        }
        return transformService;
    }

    public String extract(Blob blob, String mimetype,
            FulltextFieldDescriptor desc) throws Exception {

        String res = "";

        String transformerName = desc.lookupTransformer(mimetype);
        if (transformerName != null) {
            Transformer transformer = getTransformService().getTransformerByName(
                    transformerName);

            if (transformer == null) {
                log.warn("Transformer with name :" + transformerName
                        + " not found...");
                return res;
            }

            try {
                List<TransformDocument> docs = transformService.transform(
                        transformerName, null, blob);
                for (TransformDocument doc : docs) {
                    // XXX check this out if not too costly.
                    Blob docBlob = doc.getBlob();
                    res += readContent(docBlob.getReader());
                }
                docs = null;
            } catch (Throwable t) {
                // We don't want to throw back exceptions that are not caught be
                // underlying plugin internals
                log.error("Couldn't extract blob content...", t);
            }
        } else {
            log.error("Transformer with name=" + transformerName
                    + " cannot be found... Couldn't extract blob content"
                    + " Check out your configuration.");
        }

        return res;
    }

    public static String readContent(Reader reader) throws IOException {
        char[] buffer = new char[2048];
        StringWriter writer = new StringWriter();
        int length;
        while ((length = reader.read(buffer, 0, 2048)) != -1) {
            writer.write(buffer, 0, length);
        }
        String content = stripByteOrderMarkChar(writer.toString());
        return content.trim();
    }

    private static String stripByteOrderMarkChar(String content) {
        if (content.length() > 0 && content.charAt(0) == BYTE_ORDER_MARK_CHAR) {
            return content.substring(1).trim();
        }
        return content;
    }

}
