/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.template.processors.xdocreport;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.template.processors.AbstractBindingResolver;

import fr.opensagres.xdocreport.core.document.SyntaxKind;
import fr.opensagres.xdocreport.document.images.IImageProvider;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XDocReportBindingResolver extends AbstractBindingResolver {

    protected final FieldsMetadata metadata;

    public XDocReportBindingResolver(FieldsMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    protected String handleHtmlField(String paramName, String htmlValue) {
        metadata.addFieldAsTextStyling(paramName, SyntaxKind.Html);
        return super.handleHtmlField(paramName, htmlValue);
    }

    @Override
    protected void handleBlobField(String paramName, Blob blobValue) {
        if ("text/html".equals(blobValue.getMimeType())) {
            metadata.addFieldAsTextStyling(paramName, SyntaxKind.Html);
        }
    }

    @Override
    protected Object handlePictureField(String paramName, Blob blobValue) {
        if (blobValue == null) {
            // manage a default picture : blank one :)
            try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("blank.png")) {
                byte[] bin = IOUtils.toByteArray(is);
                blobValue = Blobs.createBlob(bin, "image/png");
                blobValue.setFilename("blank.png");
            } catch (IOException e) {
                log.error("Unable to read fake Blob", e);
            }
        }
        IImageProvider imgBlob = new BlobImageProvider(blobValue);
        metadata.addFieldAsImage(paramName);
        return imgBlob;
    }

    @Override
    protected Object handleLoop(String paramName, Object value) {
        metadata.addFieldAsList(paramName);
        try {
            return getWrapper().wrap(value);
        } catch (TemplateModelException e) {
            return null;
        }
    }

}
