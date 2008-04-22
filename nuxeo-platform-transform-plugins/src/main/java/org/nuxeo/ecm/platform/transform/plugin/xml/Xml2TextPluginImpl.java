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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.plugin.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;

/**
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class Xml2TextPluginImpl extends AbstractPlugin {

    private static final long serialVersionUID = -8395742119156169742L;

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        final List<TransformDocument> results = super.transform(options,
                sources);

        extractFromXml(results, sources);

        return results;
    }

    public void extractFromXml(List<TransformDocument> results,
            TransformDocument... sources) throws Exception {

        for (final TransformDocument source : sources) {
            final InputStream sourceIs = source.getBlob().getStream();
            final Blob result = extractFromXmlSource(sourceIs);
            results.add(new TransformDocumentImpl(result, result.getMimeType()));
        }
    }

    /**
     * @param sourceIs
     * @return
     * @throws DocumentException
     * @throws Exception
     * @throws IOException
     */
    private Blob extractFromXmlSource(final InputStream sourceIs)
            throws DocumentException, Exception, IOException {
        String text = "";
        try {
            Xml2Text xml2text = new Xml2Text();
            text = xml2text.parse(sourceIs);
        } catch (Exception e) {
            // do  nothing - this may happen when input stream is empty
        }
        return new StringBlob(text, "text/plain");
    }

}
