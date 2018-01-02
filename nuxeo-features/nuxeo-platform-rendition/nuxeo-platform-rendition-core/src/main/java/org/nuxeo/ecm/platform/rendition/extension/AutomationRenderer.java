/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.extension;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * Class introduced to share code between sync and lazy automation based renditions
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class AutomationRenderer {

    /**
     * Generate the rendition Blobs for a given {@link RenditionDefinition}. Return is a List of Blob for bigger
     * flexibility (typically HTML rendition with resources)
     *
     * @param doc the target {@link DocumentModel}
     * @param definition the {@link RenditionDefinition} to use
     * @param session the {@link CoreSession} to use
     * @return The list of Blobs
     */
    public static List<Blob> render(DocumentModel doc, RenditionDefinition definition, CoreSession session) {

        String chain = definition.getOperationChain();
        if (chain == null) {
            throw new NuxeoException("no operation defined");
        }

        if (session == null) {
            session = doc.getCoreSession();
        }
        AutomationService as = Framework.getService(AutomationService.class);
        try (OperationContext oc = new OperationContext(session)) {
            oc.push(Constants.O_DOCUMENT, doc);

            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                try {
                    Blob blob = bh.getBlob();
                    if (blob != null) {
                        oc.push(Constants.O_BLOB, blob);
                    }
                } catch (Exception e) {
                    if (!definition.isEmptyBlobAllowed()) {
                        throw new NuxeoException("No Blob available", e);
                    }
                }
            } else {
                if (!definition.isEmptyBlobAllowed()) {
                    throw new NuxeoException("No Blob available");
                }
            }

            Blob blob = (Blob) as.run(oc, definition.getOperationChain());
            if (blob != null && StringUtils.isBlank(blob.getFilename())) {
                String filename = getFilenameWithExtension(doc.getTitle(), blob.getMimeType(), "bin");
                blob.setFilename(filename);
            }
            List<Blob> blobs = new ArrayList<>();
            blobs.add(blob);
            return blobs;

        } catch (Exception e) {
            throw new NuxeoException("Exception while running the operation chain: " + definition.getOperationChain(),
                    e);
        }
    }

    /**
     * Generate a revised filename whose extension is either based on the supplied mimeType if applicable or the
     * supplied default extension.
     *
     * @param filename the filename to use
     * @param mimeType the mimeType from which the assigned extension is derived
     * @param defaultExtension the default extension to be assigned if the mimeType has no corresponding extension
     * @return the filename with the revised extension
     * @since 7.4
     */
    public static String getFilenameWithExtension(String filename, String mimeType, String defaultExtension) {
        String baseName = FilenameUtils.getBaseName(filename);
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        MimetypeEntry mimeTypeEntry = mimetypeRegistry.getMimetypeEntryByMimeType(mimeType);
        List<String> extensions = mimeTypeEntry.getExtensions();
        String extension;
        if (!extensions.isEmpty()) {
            extension = extensions.get(0);
        } else {
            extension = defaultExtension;
        }
        return (extension == null) ? filename : baseName + "." + extension;
    }

}
