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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.multiresult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;

/**
 * Test plugin for multiple result values within a TransformDocument. Will copy
 * the input parameters to result params.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class MultipleResultsEchoPlugin extends AbstractPlugin {

    private static final long serialVersionUID = -7522610596626881865L;

    private static final Log log = LogFactory.getLog(MultipleResultsEchoPlugin.class);

    public MultipleResultsEchoPlugin() {
        // Only takes XML as sources documents.
        sourceMimeTypes = new ArrayList<String>();
        sourceMimeTypes.add("text/xml");

        // Only outputs text.
        destinationMimeType = "text/xml";
    }

    /**
     * Just fills in TransformDocument(s) echo properties.
     */
    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) {

        final String logPrefix = "<transform> ";

        List<TransformDocument> results = new ArrayList<TransformDocument>();
        try {
            results = super.transform(options, sources);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (TransformDocument srcDocument : sources) {

            TransformDocumentImpl resultDocument;
            try {
                // FIXME: JA : What the hell is happening here ?
                resultDocument = new TransformDocumentImpl(
                        srcDocument.getBlob(), destinationMimeType);

                for (Map.Entry<String, Serializable> entry : options.entrySet()) {
                    resultDocument.setPropertyValue(entry.getKey(),
                            entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
                resultDocument = null;
            }

            if (resultDocument != null) {
                results.add(resultDocument);
            }
        }

        return results;
    }
}
