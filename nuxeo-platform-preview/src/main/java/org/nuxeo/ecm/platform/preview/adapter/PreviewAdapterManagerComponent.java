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
package org.nuxeo.ecm.platform.preview.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.preview.adapter.factories.BlobHolderPreviewAdapterFactory;
import org.nuxeo.ecm.platform.preview.adapter.factories.FileBasedPreviewAdapterFactory;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that handles the extension points and the service interface
 * for Preview Adapter management.
 *
 * @author tiry
 */
public class PreviewAdapterManagerComponent extends DefaultComponent implements
        PreviewAdapterManager {

    public static final String ADAPTER_FACTORY_EP = "AdapterFactory";

    public static final String PREVIEWED_MIME_TYPE = "MimeTypePreviewer";

    public static final String BLOB_POST_PROCESSOR_EP = "blobPostProcessor";

    private static final Log log = LogFactory.getLog(PreviewAdapterManagerComponent.class);

    protected Map<String, PreviewAdapterFactory> factoryRegistry = new HashMap<String, PreviewAdapterFactory>();

    protected Map<String, MimeTypePreviewer> previewerFactory = new HashMap<String, MimeTypePreviewer>();

    protected List<BlobPostProcessor> blobPostProcessors = new ArrayList<BlobPostProcessor>();

    // Component and EP management

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (ADAPTER_FACTORY_EP.equals(extensionPoint)) {
            AdapterFactoryDescriptor desc = (AdapterFactoryDescriptor) contribution;

            PreviewAdapterFactory factory = desc.getNewInstance();

            if (factory != null) {
                factoryRegistry.put(desc.getTypeName(), factory);
            }
        } else if (PREVIEWED_MIME_TYPE.equals(extensionPoint)) {
            MimeTypePreviewerDescriptor desc = (MimeTypePreviewerDescriptor) contribution;
            previewerFactory.put(desc.getPattern(), desc.getKlass().newInstance());
        } else if (BLOB_POST_PROCESSOR_EP.equals(extensionPoint)) {
            BlobPostProcessorDescriptor desc = (BlobPostProcessorDescriptor) contribution;
            blobPostProcessors.add(desc.getKlass().newInstance());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    // service interface impl

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

    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        if (doc == null) {
            return null;
        }

        if (doc.isFolder()) {
            return null;
        }

        String docType = doc.getType();

        log.debug("Looking for HTMLPreviewAdapter for type " + docType);

        if (factoryRegistry.containsKey(docType)) {
            log.debug("dedicated HTMLPreviewAdapter factory found");
            return factoryRegistry.get(docType).getAdapter(doc);
        }

        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            log.debug("using Blob Holder based HtmlPreviewAdapter factory");
            PreviewAdapterFactory factory = new BlobHolderPreviewAdapterFactory();
            return factory.getAdapter(doc);

        }

        if (doc.hasSchema("file") || doc.hasSchema("files")) {
            log.debug("using default file based HtmlPreviewAdapter factory");
            PreviewAdapterFactory factory = new FileBasedPreviewAdapterFactory();
            return factory.getAdapter(doc);
        } else {
            return null;
        }
    }

    public MimeTypePreviewer getPreviewer(String mimeType) {
        for(Map.Entry<String, MimeTypePreviewer> entry : previewerFactory.entrySet()) {
            if(mimeType.matches(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public List<BlobPostProcessor> getBlobPostProcessors() {
        return Collections.unmodifiableList(blobPostProcessors);
    }

}
