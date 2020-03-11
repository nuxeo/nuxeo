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
package org.nuxeo.template.processors.xdocreport;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;

import fr.opensagres.xdocreport.core.document.ImageFormat;
import fr.opensagres.xdocreport.document.images.AbstractInputStreamImageProvider;

/**
 * XDocReport wrapper for a Picture stored in a Nuxeo Blob
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class BlobImageProvider extends AbstractInputStreamImageProvider {

    protected final Blob blob;

    protected final ImageFormat imageFormat;

    public BlobImageProvider(Blob blob) {
        super(false);
        this.blob = blob;
        this.imageFormat = ImageFormat.getFormatByResourceName(blob.getFilename());
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return blob.getStream();
    }

    @Override
    public ImageFormat getImageFormat() {
        return imageFormat;
    }

}
