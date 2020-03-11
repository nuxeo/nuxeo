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
 * Runtime component that handles the extension points and the service interface for Preview Adapter management.
 *
 * @author tiry
 */
public class PreviewAdapterManagerComponent extends DefaultComponent implements PreviewAdapterManager {

    public static final String ADAPTER_FACTORY_EP = "AdapterFactory";

    public static final String PREVIEWED_MIME_TYPE = "MimeTypePreviewer";

    public static final String BLOB_POST_PROCESSOR_EP = "blobPostProcessor";

    private static final Log log = LogFactory.getLog(PreviewAdapterManagerComponent.class);

    protected Map<String, PreviewAdapterFactory> factoryRegistry = new HashMap<>();

    protected Map<String, MimeTypePreviewer> previewerFactory = new HashMap<>();

    protected List<BlobPostProcessor> blobPostProcessors = new ArrayList<>();

    // Component and EP management

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (ADAPTER_FACTORY_EP.equals(extensionPoint)) {
            AdapterFactoryDescriptor desc = (AdapterFactoryDescriptor) contribution;
            if (desc.isEnabled()) {
                PreviewAdapterFactory factory = desc.getNewInstance();
                if (factory != null) {
                    factoryRegistry.put(desc.getTypeName(), factory);
                }
            } else {
                factoryRegistry.remove(desc.getTypeName());
            }
        } else if (PREVIEWED_MIME_TYPE.equals(extensionPoint)) {
            MimeTypePreviewerDescriptor desc = (MimeTypePreviewerDescriptor) contribution;
            for (String pattern : desc.getPatterns()) {
                previewerFactory.put(pattern, newInstance(desc.getKlass()));
            }
        } else if (BLOB_POST_PROCESSOR_EP.equals(extensionPoint)) {
            BlobPostProcessorDescriptor desc = (BlobPostProcessorDescriptor) contribution;
            blobPostProcessors.add(newInstance(desc.getKlass()));
        }
    }

    protected <T> T newInstance(Class<T> klass) {
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
    }

    // service interface impl

    @Override
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

    @Override
    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        if (doc == null) {
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

    @Override
    public MimeTypePreviewer getPreviewer(String mimeType) {
        for (Map.Entry<String, MimeTypePreviewer> entry : previewerFactory.entrySet()) {
            if (mimeType.matches(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public List<BlobPostProcessor> getBlobPostProcessors() {
        return Collections.unmodifiableList(blobPostProcessors);
    }

}
