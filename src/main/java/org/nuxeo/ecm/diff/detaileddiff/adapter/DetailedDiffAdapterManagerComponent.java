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
package org.nuxeo.ecm.diff.detaileddiff.adapter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffException;
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffAdapter;
import org.nuxeo.ecm.diff.detaileddiff.adapter.factories.BlobHolderDetailedDiffAdapterFactory;
import org.nuxeo.ecm.diff.detaileddiff.adapter.factories.FileBasedDetailedDiffAdapterFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that handles the extension points and the service interface
 * for Detailed diff Adapter management.
 *
 * @author tiry
 */
public class DetailedDiffAdapterManagerComponent extends DefaultComponent
        implements DetailedDiffAdapterManager {

    public static final String ADAPTER_FACTORY_EP = "adapterFactory";

    public static final String MIME_TYPE_DETAILED_DIFFER_EP = "mimeTypeDetailedDiffer";

    // public static final String BLOB_POST_PROCESSOR_EP = "blobPostProcessor";

    private static final Log log = LogFactory.getLog(DetailedDiffAdapterManagerComponent.class);

    protected Map<String, DetailedDiffAdapterFactory> factoryRegistry = new HashMap<String, DetailedDiffAdapterFactory>();

    protected Map<String, MimeTypeDetailedDiffer> detailedDifferFactory = new HashMap<String, MimeTypeDetailedDiffer>();

    // protected List<BlobPostProcessor> blobPostProcessors = new
    // ArrayList<BlobPostProcessor>();

    // Component and EP management

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (ADAPTER_FACTORY_EP.equals(extensionPoint)) {
            DetailedDiffAdapterFactoryDescriptor desc = (DetailedDiffAdapterFactoryDescriptor) contribution;
            if (desc.isEnabled()) {
                DetailedDiffAdapterFactory factory = desc.getNewInstance();
                if (factory != null) {
                    factoryRegistry.put(desc.getTypeName(), factory);
                }
            } else {
                factoryRegistry.remove(desc.getTypeName());
            }
        } else if (MIME_TYPE_DETAILED_DIFFER_EP.equals(extensionPoint)) {
            MimeTypeDetailedDifferDescriptor desc = (MimeTypeDetailedDifferDescriptor) contribution;
            detailedDifferFactory.put(desc.getPattern(),
                    desc.getKlass().newInstance());
        }
        // else if (BLOB_POST_PROCESSOR_EP.equals(extensionPoint)) {
        // BlobPostProcessorDescriptor desc = (BlobPostProcessorDescriptor)
        // contribution;
        // blobPostProcessors.add(desc.getKlass().newInstance());
        // }
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

    public DetailedDiffAdapter getAdapter(DocumentModel doc) {
        if (doc == null) {
            return null;
        }

        String docType = doc.getType();

        log.debug("Looking for HTMLDetailedDiffAdapter for type " + docType);

        if (factoryRegistry.containsKey(docType)) {
            log.debug("Dedicated HTMLDetailedDiffAdapter factory found");
            return factoryRegistry.get(docType).getAdapter(doc);
        }

        if (doc.isFolder()) {
            return null;
        }

        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            log.debug("Using Blob Holder based HtmlDetailedDiffAdapter factory");
            DetailedDiffAdapterFactory factory = new BlobHolderDetailedDiffAdapterFactory();
            return factory.getAdapter(doc);

        }

        if (doc.hasSchema("file") || doc.hasSchema("files")) {
            log.debug("Using default file based HtmlDetailedDiffAdapter factory");
            DetailedDiffAdapterFactory factory = new FileBasedDetailedDiffAdapterFactory();
            return factory.getAdapter(doc);
        } else {
            return null;
        }
    }

    public MimeTypeDetailedDiffer getDetailedDiffer(String mimeType) {
        for (Map.Entry<String, MimeTypeDetailedDiffer> entry : detailedDifferFactory.entrySet()) {
            if (mimeType.matches(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public HtmlDetailedDiffer getHtmlDetailedDiffer()
            throws DetailedDiffException {
        MimeTypeDetailedDiffer htmlDetailedDiffer = detailedDifferFactory.get("text/html");
        if (htmlDetailedDiffer == null
                || !(htmlDetailedDiffer instanceof HtmlDetailedDiffer)) {
            throw new DetailedDiffException(
                    "No detailed differ of type HtmlDetailedDiffer found for the 'text/html' mime-type. Please check the 'mimeTypeDetailedDiffer' contributions.");
        }
        return (HtmlDetailedDiffer) htmlDetailedDiffer;
    }

    // public List<BlobPostProcessor> getBlobPostProcessors() {
    // return Collections.unmodifiableList(blobPostProcessors);
    // }

}
