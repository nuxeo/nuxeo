/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Laurent Doguin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.convert.plugins;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.ZipEntryBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Zip2Html converter.
 */
public class Zip2HtmlConverter implements Converter {

    protected static final String INDEX_HTML = "index.html";

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Blob zipBlob = blobHolder.getBlob();
        String mimeType = zipBlob.getMimeType();
        if (!mimeType.equals("application/zip") && !mimeType.equals("application/x-zip-compressed")) {
            throw new ConversionException("not a zip file", blobHolder);
        }

        List<String> names = new ArrayList<>();
        Optional<Blob> indexBlob = listNamesAndCreateIndex(zipBlob, names);

        List<Blob> blobs = new ArrayList<>();
        indexBlob.ifPresent(blobs::add);
        names.forEach(name -> blobs.add(new ZipEntryBlob(zipBlob, name)));
        return new SimpleBlobHolder(blobs);
    }

    protected Optional<Blob> listNamesAndCreateIndex(Blob zipBlob, List<String> names) {
        File file = zipBlob.getFile();
        if (file != null) {
            // if there's a file then we can be fast
            try (ZipFile zipFile = new ZipFile(file)) {
                zipFile.stream().forEach(entry -> names.add(entry.getName()));
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        } else {
            // else use the stream
            try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(zipBlob.getStream()))) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    names.add(entry.getName());
                }
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }

        orderIndexFirst(names);
        if (names.isEmpty() || names.get(0).contains(INDEX_HTML)) {
            return Optional.empty(); // no index blob to create
        }
        Blob indexBlob = createIndexBlob(zipBlob.getFilename(), names);
        return Optional.of(indexBlob);
    }

    // see also similar method in SimpleCachableBlobHolder
    protected void orderIndexFirst(List<String> names) {
        String indexName = null;
        for (String name : names) {
            if (name.contains(INDEX_HTML) && (indexName == null || name.compareTo(indexName) < 0)) {
                indexName = name;
            }
        }
        if (indexName != null) {
            names.remove(indexName);
            names.add(0, indexName);
        }
    }

    protected Blob createIndexBlob(String title, List<String> names) {
        StringBuilder page = new StringBuilder("<html><body>");
        page.append("<h1>")
            .append(StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(title)))
            .append("</h1>");
        page.append("<ul>");
        for (String name : names) {
            String fn = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(name));
            page.append("<li><a href=\"").append(fn).append("\">");
            page.append(fn);
            page.append("</a></li>");
        }
        page.append("</ul></body></html>");
        return Blobs.createBlob(page.toString(), "text/html", null, INDEX_HTML);
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
