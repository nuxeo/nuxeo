/*
 * (C) Copyright 2008 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener responsible for computing the mimetype of a new or edited blob and
 * the common:icon field if necessary.
 * <p>
 * The common:size is also maintained as the length of the main blob to preserve
 * backward compatibility.
 * <p>
 * The logics of this event listener is divided into static public methods to
 * make it easy to override this event listener with a custom implementation.
 *
 * @author ogrisel
 */
public class MimetypeIconUpdater implements EventListener {

    public static final String ICON_FIELD = "/common:icon";

    public static final String MAIN_BLOB_FIELD = "/file:content";

    @Deprecated
    // the length of the main blob is now stored inside the blob itself
    private static final String SIZE_FIELD = "common:/size";

    @Deprecated
    // the filename should now be stored inside the main blob
    public static final String MAIN_EXTERNAL_FILENAME_FIELD = "file:/filename";

    public void handleEvent(Event event) throws ClientException {

        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();

            // BBB: handle old filename scheme
            updateFilename(doc);

            try {
                // ensure the document main icon is not null
                setDefaultIcon(doc);

                // Scan blob-typed dirty fields to update there mimetype
                MimetypeRegistry mimetypeService = Framework
                        .getService(MimetypeRegistry.class);
                for (DataModel dm : new ArrayList<DataModel>(doc
                        .getDataModelsCollection())) {
                    // we iterate over doc.getDataModelsCollection() and then grab
                    // the DocumentPart instead of directly calling
                    // doc.getDocumentParts since the later would load yet unfetched
                    // datamodels which cannot be dirty

                    // the data models are copied in a new list to avoid
                    // concurrent modification problem at the iterator level
                    DocumentPart dp = ((DataModelImpl) dm).getDocumentPart();
                    recursivelyUpdateBlobs(doc, mimetypeService, dp
                            .getDirtyChildren());
                }
            }
            catch (Exception e) {
                throw new ClientException("Error in MimetypeIconUpdater listener", e);
            }

        }
    }

    /**
     * Recursively call updateBlobProperty on every dirty blob embedded as
     * direct children or contained in one of the container children.
     */
    public static void recursivelyUpdateBlobs(DocumentModel doc,
            MimetypeRegistry mimetypeService, Iterator<Property> dirtyChildren)
            throws Exception {
        while (dirtyChildren.hasNext()) {
            Property dirtyProperty = dirtyChildren.next();
            if (dirtyProperty instanceof BlobProperty) {
                updateBlobProperty(doc, mimetypeService, dirtyProperty);
            } else if (dirtyProperty.isContainer()) {
                recursivelyUpdateBlobs(doc, mimetypeService, dirtyProperty
                        .getDirtyChildren());
            }
        }
    }

    /**
     * Update the mimetype of a blob allong with the icon and size fields of the
     * document if the blob is the main blob of the document.
     */
    public static void updateBlobProperty(DocumentModel doc,
            MimetypeRegistry mimetypeService, Property dirtyProperty)
            throws Exception {
        String fieldPath = dirtyProperty.getPath();
        //cas shema without prefix : we need to add shema name as prefix
        if(!fieldPath.contains(":"))
            fieldPath = "/"+dirtyProperty.getSchema().getName()+":"+fieldPath.substring(1);


        Blob blob = dirtyProperty.getValue(Blob.class);
        if (blob != null) {
            // update the mimetype using the the mimetype registry
            // service
            blob = mimetypeService.updateMimetype(blob);
            doc.setPropertyValue(fieldPath, (Serializable) blob);
        }
        if (MAIN_BLOB_FIELD.equals(fieldPath)) {
            // update the icon field of the document
            if (blob != null) {
                MimetypeEntry mimetypeEntry = mimetypeService
                        .getMimetypeEntryByMimeType(blob.getMimeType());
                updateIconField(mimetypeEntry, doc);
            } else {
                // reset to document type icon
                updateIconField(null, doc);
            }

            // BBB: update the deprecated common:size field to preserver
            // backward compatibility (we should only use
            // file:content/length instead)
            doc.setPropertyValue(SIZE_FIELD, blob != null ? blob.getLength()
                    : 0);
        }
    }

    /**
     * Backward compatibility for external filename field: if edited, it might
     * affect the main blob mimetype
     */
    public static void updateFilename(DocumentModel doc)
            throws PropertyException {

        if (doc.hasSchema(MAIN_BLOB_FIELD.split(":")[0])) {
            Property filenameProperty;
            try {
                filenameProperty = doc
                        .getProperty(MAIN_EXTERNAL_FILENAME_FIELD);
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            if (filenameProperty.isDirty()) {
                String filename = filenameProperty.getValue(String.class);
                try {
                    if (doc.getProperty(MAIN_BLOB_FIELD).getValue() != null) {
                        Blob blob = doc.getProperty(MAIN_BLOB_FIELD).getValue(
                                Blob.class);
                        blob.setFilename(filename);
                        doc.setPropertyValue(MAIN_BLOB_FIELD,
                                (Serializable) blob);
                    }
                } catch (ClientException e) {
                    throw new ClientRuntimeException(e);
                }
            }
        }
    }

    /**
     * If the icon field is empty, initialize it to the document type icon
     */
    public static void setDefaultIcon(DocumentModel doc) throws Exception {
        if (doc.getProperty(ICON_FIELD).getValue(String.class) == null) {
            updateIconField(null, doc);
        }
    }

    /**
     * Compute the main icon of a Nuxeo document based on the mimetype of the
     * main attached blob with of fallback on the document type generic icon.
     */
    public static void updateIconField(MimetypeEntry mimetypeEntry,
            DocumentModel doc) throws Exception {
        String iconPath = null;
        if (mimetypeEntry != null && mimetypeEntry.getIconPath() != null) {
            iconPath = "/icons/" + mimetypeEntry.getIconPath();
        } else {
            TypeManager typeManager = Framework.getService(TypeManager.class);
            if (typeManager==null) {
                return;
            }
            Type uiType = typeManager.getType(doc.getType());
            if (uiType != null) {
                iconPath = uiType.getIcon();
            }
        }
        if (iconPath != null) {
            doc.setPropertyValue(ICON_FIELD, iconPath);
        }
    }

}
