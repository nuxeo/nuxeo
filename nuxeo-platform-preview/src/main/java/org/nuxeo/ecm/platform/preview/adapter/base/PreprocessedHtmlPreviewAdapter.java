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
 */
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

/**
 * Base class for preview adapters that will use preprocessed HTML preview that
 * is stored inside the document.
 *
 * @author tiry
 */
public class PreprocessedHtmlPreviewAdapter extends AbstractHtmlPreviewAdapter {

    private static final Log log = LogFactory.getLog(PreprocessedHtmlPreviewAdapter.class);

    protected List<String> storedPreviewFieldsPaths = new ArrayList<String>();

    public PreprocessedHtmlPreviewAdapter(List<String> fieldsPaths) {
        storedPreviewFieldsPaths = fieldsPaths;
    }

    @Override
    public List<Blob> getPreviewBlobs() throws PreviewException {

        List<Blob> resultBlobs = new ArrayList<Blob>();

        for (String xpath : storedPreviewFieldsPaths) {
            try {
                Property prop = adaptedDoc.getProperty(xpath);
                if (prop.isComplex()) {
                    Blob blob = (Blob) prop.getValue();
                    try {
                        blob.getStream().reset();
                    } catch (IOException e) {
                        log.error(e);
                    }
                    resultBlobs.add(blob);
                } else {
                    String data = (String) prop.getValue();
                    resultBlobs.add(new StringBlob(data));
                }
            } catch (PropertyException e) {
                throw new PreviewException("Unable to get property " + xpath, e);
            } catch (ClientException e) {
                throw new PreviewException("Unable to get property " + xpath, e);
            }
        }
        return resultBlobs;
    }

    @Override
    public List<Blob> getPreviewBlobs(String xpath) throws PreviewException {
        return getPreviewBlobs();
    }

    public void cleanup() {
        // nothing to do
    }

    public boolean cachable() {
        return false;
    }

}
