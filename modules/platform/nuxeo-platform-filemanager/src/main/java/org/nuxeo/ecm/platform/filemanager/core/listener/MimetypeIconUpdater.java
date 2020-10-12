/*
 * (C) Copyright 2008-2019 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry.DEFAULT_MIMETYPE;

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
 * Listener responsible for computing the mimetype of a new or edited blob and the {@code common:icon} field if
 * necessary.
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

    /**
     * @deprecated since 11.1. Use {@link MimetypeRegistry#DEFAULT_MIMETYPE} instead.
     */
    @Deprecated(since = "11.1", forRemoval = true)
    protected static final String OCTET_STREAM_MT = DEFAULT_MIMETYPE;

    /**
     * @deprecated since 11.1. Create a new instance of {@link BlobsExtractor} when needed.
     */
    @Deprecated(since = "11.1", forRemoval = true)
    public final BlobsExtractor blobExtractor = new BlobsExtractor();

    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link MimetypeRegistry} instead.
     */
    @Deprecated(since = "11.1", forRemoval = true)
    MimetypeRegistry mimetypeService;

    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link MimetypeRegistry} instead.
     */
    @Deprecated(since = "11.1", forRemoval = true)
    public MimetypeRegistry getMimetypeRegistry() {
        if (mimetypeService == null) {
            mimetypeService = Framework.getService(MimetypeRegistry.class);
        }

        return mimetypeService;
    }

    @Override
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

                MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
                BlobsExtractor extractor = new BlobsExtractor();
                for (Property prop : extractor.getBlobsProperties(doc)) {
                    if (prop.isDirty()) {
                        updateBlobProperty(doc, mimetypeRegistry, prop);
                    }
                }

                // update the document icon and size according to the main blob
                if (doc.hasSchema(MAIN_BLOB_SCHEMA) && doc.getProperty(MAIN_BLOB_FIELD).isDirty()) {
                    updateIconAndSizeFields(doc, mimetypeRegistry,
                            doc.getProperty(MAIN_BLOB_FIELD).getValue(Blob.class));
                }
            } catch (PropertyException e) {
                e.addInfo("Error in MimetypeIconUpdater listener");
                throw e;
            }
        }
    }

    /**
     * Updates the mimetype of a blob along with the icon and size fields of the document if the blob is the main blob
     * of the document.
     */
    public void updateBlobProperty(DocumentModel doc, MimetypeRegistry mimetypeService, Property dirtyProperty) {
        String fieldPath = dirtyProperty.getXPath();
        if (!fieldPath.contains(":")) {
            // for schema without prefix: we need to add schema name as prefix
            fieldPath = dirtyProperty.getSchema().getName() + ":" + fieldPath;
        }

        Blob blob = dirtyProperty.getValue(Blob.class);
        if (blob == null) {
            return;
        }
        if (blob.getMimeType() == null || blob.getMimeType().startsWith(DEFAULT_MIMETYPE)) {
            // update the mime type (if not set) using the mimetype registry service
            blob = mimetypeService.updateMimetype(blob);
            doc.setPropertyValue(fieldPath, (Serializable) blob);
        } else if (!mimetypeService.isMimeTypeNormalized(blob.getMimeType())) {
            // normalize the mime type if not yet normalized
            mimetypeService.getNormalizedMimeType(blob.getMimeType()).ifPresent(blob::setMimeType);
        }
    }

    private void updateIconAndSizeFields(DocumentModel doc, MimetypeRegistry mimetypeService, Blob blob) {
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
     * Computes the main icon of a Nuxeo document based on the mime type of the main attached blob with of fallback on
     * the document type generic icon.
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
