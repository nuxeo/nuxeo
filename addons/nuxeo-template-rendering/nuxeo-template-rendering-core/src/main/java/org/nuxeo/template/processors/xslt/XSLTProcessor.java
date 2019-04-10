/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.processors.xslt;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.AbstractTemplateProcessor;

public class XSLTProcessor extends AbstractTemplateProcessor implements TemplateProcessor {

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument, String templateName) {

        BlobHolder bh = templateBasedDocument.getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }

        Blob xmlContent = bh.getBlob();
        if (xmlContent == null) {
            return null;
        }

        Blob sourceTemplateBlob = getSourceTemplateBlob(templateBasedDocument, templateName);

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            tFactory.setFeature(FEATURE_SECURE_PROCESSING, true);
            transformer = tFactory.newTransformer(new StreamSource(sourceTemplateBlob.getStream()));
        } catch (TransformerConfigurationException | IOException e) {
            throw new NuxeoException(e);
        }
        transformer.setErrorListener(new ErrorListener() {

            @Override
            public void warning(TransformerException exception) throws TransformerException {
                log.warn("Problem during transformation", exception);
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                log.error("Fatal error during transformation", exception);
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
                log.error("Error during transformation", exception);
            }
        });
        transformer.setURIResolver(null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            transformer.transform(new StreamSource(xmlContent.getStream()), new StreamResult(out));
        } catch (TransformerException | IOException e) {
            throw new NuxeoException(e);
        }

        Blob result = new ByteArrayBlob(out.toByteArray(), "text/xml");
        String targetFileName = FileUtils.getFileNameNoExt(templateBasedDocument.getAdaptedDoc().getTitle());
        // result.setFilename(targetFileName + ".xml");
        result.setFilename(targetFileName + ".html");

        return result;

    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) {
        return new ArrayList<TemplateInput>();
    }

}
