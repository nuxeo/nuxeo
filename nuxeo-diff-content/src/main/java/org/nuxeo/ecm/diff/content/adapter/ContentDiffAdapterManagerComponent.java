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
package org.nuxeo.ecm.diff.content.adapter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.adapter.factories.BlobHolderContentDiffAdapterFactory;
import org.nuxeo.ecm.diff.content.adapter.factories.FileBasedContentDiffAdapterFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that handles the extension points and the service interface
 * for content diff Adapter management.
 *
 * @author Antoine Taillefer
 */
public class ContentDiffAdapterManagerComponent extends DefaultComponent
        implements ContentDiffAdapterManager {

    public static final String ADAPTER_FACTORY_EP = "adapterFactory";

    public static final String MIME_TYPE_CONTENT_DIFFER_EP = "mimeTypeContentDiffer";

    private static final Log log = LogFactory.getLog(ContentDiffAdapterManagerComponent.class);

    protected Map<String, ContentDiffAdapterFactory> factoryRegistry = new HashMap<String, ContentDiffAdapterFactory>();

    protected Map<String, MimeTypeContentDiffer> contentDifferFactory = new HashMap<String, MimeTypeContentDiffer>();

    // Component and EP management

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (ADAPTER_FACTORY_EP.equals(extensionPoint)) {
            ContentDiffAdapterFactoryDescriptor desc = (ContentDiffAdapterFactoryDescriptor) contribution;
            if (desc.isEnabled()) {
                ContentDiffAdapterFactory factory = desc.getNewInstance();
                if (factory != null) {
                    factoryRegistry.put(desc.getTypeName(), factory);
                }
            } else {
                factoryRegistry.remove(desc.getTypeName());
            }
        } else if (MIME_TYPE_CONTENT_DIFFER_EP.equals(extensionPoint)) {
            MimeTypeContentDifferDescriptor desc = (MimeTypeContentDifferDescriptor) contribution;
            try {
                contentDifferFactory.put(desc.getPattern(),
                        desc.getKlass().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
    }

    // Service interface impl

    public boolean hasAdapter(DocumentModel doc) {
        if (doc == null) {
            return false;
        }

        String docType = doc.getType();
        if (factoryRegistry.containsKey(docType)) {
            return true;
        }

        return doc.hasSchema("file") || doc.hasSchema("files");
    }

    public ContentDiffAdapter getAdapter(DocumentModel doc) {
        if (doc == null) {
            return null;
        }

        String docType = doc.getType();

        log.debug("Looking for ContentDiffAdapter for type " + docType);

        if (factoryRegistry.containsKey(docType)) {
            log.debug("Dedicated ContentDiffAdapter factory found");
            return factoryRegistry.get(docType).getAdapter(doc);
        }

        if (doc.isFolder()) {
            return null;
        }

        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            log.debug("Using Blob Holder based ContentDiffAdapter factory");
            ContentDiffAdapterFactory factory = new BlobHolderContentDiffAdapterFactory();
            return factory.getAdapter(doc);

        }

        if (doc.hasSchema("file") || doc.hasSchema("files")) {
            log.debug("Using default file based ContentDiffAdapter factory");
            ContentDiffAdapterFactory factory = new FileBasedContentDiffAdapterFactory();
            return factory.getAdapter(doc);
        } else {
            return null;
        }
    }

    public MimeTypeContentDiffer getContentDiffer(String mimeType) {
        for (Map.Entry<String, MimeTypeContentDiffer> entry : contentDifferFactory.entrySet()) {
            if (mimeType.matches(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public HtmlContentDiffer getHtmlContentDiffer() throws ContentDiffException {
        MimeTypeContentDiffer htmlContentDiffer = contentDifferFactory.get("text/html");
        if (htmlContentDiffer == null
                || !(htmlContentDiffer instanceof HtmlContentDiffer)) {
            throw new ContentDiffException(
                    "No content differ of type HtmlContentDiffer found for the 'text/html' mime-type. Please check the 'mimeTypeContentDiffer' contributions.");
        }
        return (HtmlContentDiffer) htmlContentDiffer;
    }

}
