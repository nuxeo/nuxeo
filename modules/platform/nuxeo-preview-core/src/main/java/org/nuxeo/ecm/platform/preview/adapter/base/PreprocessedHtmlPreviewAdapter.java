/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

/**
 * Base class for preview adapters that will use preprocessed HTML preview that is stored inside the document.
 *
 * @author tiry
 */
public class PreprocessedHtmlPreviewAdapter extends AbstractHtmlPreviewAdapter {

    private static final Log log = LogFactory.getLog(PreprocessedHtmlPreviewAdapter.class);

    protected List<String> storedPreviewFieldsPaths = new ArrayList<>();

    public PreprocessedHtmlPreviewAdapter(List<String> fieldsPaths) {
        storedPreviewFieldsPaths = fieldsPaths;
    }

    @Override
    public List<Blob> getPreviewBlobs() throws PreviewException {

        List<Blob> resultBlobs = new ArrayList<>();

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
                    resultBlobs.add(Blobs.createBlob(data));
                }
            } catch (PropertyException e) {
                throw new PreviewException("Unable to get property " + xpath, e);
            }
        }
        return resultBlobs;
    }

    @Override
    public List<Blob> getPreviewBlobs(String xpath) throws PreviewException {
        return getPreviewBlobs();
    }

    @Override
    public void cleanup() {
        // nothing to do
    }

    @Override
    public boolean cachable() {
        return false;
    }

    @Override
    public boolean hasBlobToPreview() throws PreviewException {
        return getPreviewBlobs().size() > 0;
    }

    @Override
    public boolean hasPreview(String xpath) {
        return hasBlobToPreview();
    }
}
