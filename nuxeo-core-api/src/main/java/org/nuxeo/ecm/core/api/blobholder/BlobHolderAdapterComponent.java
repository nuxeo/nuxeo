/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.api.blobholder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapter;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapterDescriptor;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component to manage the pluggable factory for
 * {@link DocumentAdapterFactory}.
 * <p>
 * Also provides the service interface {@link BlobHolderAdapterService}
 *
 * @author tiry
 */
public class BlobHolderAdapterComponent extends DefaultComponent implements
        BlobHolderAdapterService {

    private static final Log log = LogFactory.getLog(BlobHolderAdapterComponent.class);

    public static final String BLOBHOLDERFACTORY_EP = "BlobHolderFactory";

    public static final String EXTERNALBLOB_ADAPTER_EP = "ExternalBlobAdapter";

    protected static final Map<String, BlobHolderFactory> factories = new HashMap<String, BlobHolderFactory>();

    protected static final Map<String, ExternalBlobAdapter> externalBlobAdapters = new HashMap<String, ExternalBlobAdapter>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (BLOBHOLDERFACTORY_EP.equals(extensionPoint)) {
            BlobHolderFactoryDescriptor desc = (BlobHolderFactoryDescriptor) contribution;
            factories.put(desc.getDocType(), desc.getFactory());
        } else if (EXTERNALBLOB_ADAPTER_EP.equals(extensionPoint)) {
            ExternalBlobAdapterDescriptor desc = (ExternalBlobAdapterDescriptor) contribution;
            ExternalBlobAdapter adapter = desc.getAdapter();
            String prefix = desc.getPrefix();
            if (externalBlobAdapters.containsKey(prefix)) {
                log.info(String.format(
                        "Overriding external blob adapter with prefix '%s'",
                        prefix));
                externalBlobAdapters.remove(prefix);
            }
            adapter.setPrefix(desc.getPrefix());
            adapter.setProperties(desc.getProperties());
            externalBlobAdapters.put(desc.getPrefix(), adapter);
            log.info(String.format(
                    "Registered external blob adapter with prefix '%s'", prefix));
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    /* for test */

    public static Set<String> getFactoryNames() {
        return factories.keySet();
    }

    /* Service Interface */

    public ExternalBlobAdapter getExternalBlobAdapterForPrefix(String prefix) {
        return externalBlobAdapters.get(prefix);
    }

    public ExternalBlobAdapter getExternalBlobAdapterForUri(String uri) {
        if (uri != null && uri.contains(ExternalBlobAdapter.PREFIX_SEPARATOR)) {
            String prefix = uri.substring(0,
                    uri.indexOf(ExternalBlobAdapter.PREFIX_SEPARATOR));
            return getExternalBlobAdapterForPrefix(prefix);
        }
        return null;
    }

    public Blob getExternalBlobForUri(String uri) throws PropertyException {
        ExternalBlobAdapter adapter = getExternalBlobAdapterForUri(uri);
        if (adapter == null) {
            throw new PropertyException(String.format(
                    "No external blob adapter found for uri '%s'", uri));
        }
        return adapter.getBlob(uri);
    }

    public BlobHolder getBlobHolderAdapter(DocumentModel doc) {
        if (factories.containsKey(doc.getType())) {
            BlobHolderFactory factory = factories.get(doc.getType());
            return factory.getBlobHolder(doc);
        }

        if (doc.hasSchema("file")) {
            return new DocumentBlobHolder(doc, "file:content", "file:filename");
        } else if (doc.hasSchema("note")) {
            try {
                String mt = null;
                try {
                    mt = (String) doc.getPropertyValue("note:mime_type");
                    if (mt == null) {
                        String note = (String) doc.getPropertyValue("note:note");
                        if (note != null && !"".equals(note)) {
                            mt = "text/plain"; // BBB
                        }
                    }
                } catch (PropertyException e) {
                    // mt = null;
                }
                return new DocumentStringBlobHolder(doc, "note:note", mt);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }

}
