/*
 * (C) Copyright 2008-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener responsible for computing the mimetype of a new or edited blob and the common:icon field if necessary.
 * <p>
 * The logic of this event listener is divided into static public methods to make it easy to override this event
 * listener with a custom implementation.
 *
 * @author ogrisel
 */
public class MimetypeIconUpdater implements EventListener {

    protected Log log = LogFactory.getLog(MimetypeIconUpdater.class);

    public static final String ICON_SCHEMA = "common";

    public static final String ICON_FIELD = ICON_SCHEMA + ":" + "icon";

    public static final String MAIN_BLOB_FIELD = "file:content";

    public static final String MAIN_BLOB_SCHEMA = "file";

    protected static final String OCTET_STREAM_MT = "application/octet-stream";

    public final BlobsExtractor blobExtractor = new BlobsExtractor();

    MimetypeRegistry mimetypeService;

    public MimetypeRegistry getMimetypeRegistry() {
        if (mimetypeService == null) {
            mimetypeService = Framework.getService(MimetypeRegistry.class);
        }

        return mimetypeService;
    }

    public void handleEvent(Event event) {

        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {

            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();

            // Don't update icon for immutable documents
            if (doc.hasFacet(FacetNames.IMMUTABLE)) {
                return;
            }

            try {
                // ensure the document main icon is not null
                setDefaultIcon(doc);

                // update mimetypes of blobs in the document
                for (Property prop : blobExtractor.getBlobsProperties(doc)) {
                    if (prop.isDirty()) {
                        updateBlobProperty(doc, getMimetypeRegistry(), prop);
                    }
                }

                // update the document icon and size according to the main blob
                if (doc.hasSchema(MAIN_BLOB_SCHEMA) && doc.getProperty(MAIN_BLOB_FIELD).isDirty()) {
                    updateIconAndSizeFields(doc, getMimetypeRegistry(),
                            doc.getProperty(MAIN_BLOB_FIELD).getValue(Blob.class));
                }
            } catch (PropertyException e) {
                e.addInfo("Error in MimetypeIconUpdater listener");
                throw e;
            }
        }
    }

    /**
     * Update the mimetype of a blob along with the icon and size fields of the document if the blob is the main blob of
     * the document.
     */
    public void updateBlobProperty(DocumentModel doc, MimetypeRegistry mimetypeService, Property dirtyProperty) {
        String fieldPath = dirtyProperty.getPath();
        // cas shema without prefix : we need to add schema name as prefix
        if (!fieldPath.contains(":")) {
            fieldPath = dirtyProperty.getSchema().getName() + ":" + fieldPath.substring(1);
        }

        Blob blob = dirtyProperty.getValue(Blob.class);
        if (blob != null && (blob.getMimeType() == null || blob.getMimeType().equals(OCTET_STREAM_MT))) {
            // update the mimetype (if not set) using the the mimetype registry
            // service
            blob = mimetypeService.updateMimetype(blob);
            doc.setPropertyValue(fieldPath, (Serializable) blob);
        }
    }

    private void updateIconAndSizeFields(DocumentModel doc, MimetypeRegistry mimetypeService, Blob blob)
            throws PropertyException {
        // update the icon field of the document
        if (blob != null && !doc.isFolder()) {
            MimetypeEntry mimetypeEntry = mimetypeService.getMimetypeEntryByMimeType(blob.getMimeType());
            updateIconField(mimetypeEntry, doc);
        } else {
            // reset to document type icon
            updateIconField(null, doc);
        }
    }

    /**
     * If the icon field is empty, initialize it to the document type icon
     */
    public void setDefaultIcon(DocumentModel doc) {
        if (doc.hasSchema(ICON_SCHEMA) && doc.getProperty(ICON_FIELD).getValue(String.class) == null) {
            updateIconField(null, doc);
        }
    }

    /**
     * Compute the main icon of a Nuxeo document based on the mimetype of the main attached blob with of fallback on the
     * document type generic icon.
     */
    public void updateIconField(MimetypeEntry mimetypeEntry, DocumentModel doc) {
        String iconPath = null;
        if (mimetypeEntry != null && mimetypeEntry.getIconPath() != null) {
            iconPath = "/icons/" + mimetypeEntry.getIconPath();
        } else {
            TypeManager typeManager = Framework.getService(TypeManager.class);
            if (typeManager == null) {
                return;
            }
            Type uiType = typeManager.getType(doc.getType());
            if (uiType != null) {
                iconPath = uiType.getIcon();
            }
        }
        if (iconPath != null && doc.hasSchema(ICON_SCHEMA)) {
            doc.setPropertyValue(ICON_FIELD, iconPath);
        }
    }

}
