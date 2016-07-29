/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.threed.BatchConverterHelper;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.ThreeDDocument;
import org.nuxeo.ecm.platform.threed.TransmissionThreeD;

import org.nuxeo.runtime.api.Framework;
import java.util.List;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

/**
 * Work running batch conversions to update 3D document type preview assets
 *
 * @since 8.4
 */
public class ThreeDBatchUpdateWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final String THREED_CHANGED_EVENT = "threeDEvent";

    private static final Log log = LogFactory.getLog(ThreeDBatchUpdateWork.class);

    public static final String CATEGORY_THREED_CONVERSION = "threeDConversion";

    public static final String THREED_CONVERSIONS_DONE_EVENT = "threeDConversionsDone";

    protected static String computeIdPrefix(String repositoryName, String docId) {
        return repositoryName + ':' + docId + ":threedbatch:";
    }

    public ThreeDBatchUpdateWork(String repositoryName, String docId) {
        super(computeIdPrefix(repositoryName, docId));
        setDocument(repositoryName, docId);
    }

    @Override
    public String getCategory() {
        return CATEGORY_THREED_CONVERSION;
    }

    @Override
    public String getTitle() {
        return "3D preview batch update";
    }

    @Override
    public void work() {
        setStatus("Extracting");
        setProgress(Progress.PROGRESS_INDETERMINATE);

        ThreeD originalThreeD = null;
        try {
            openSystemSession();
            originalThreeD = getThreeDToConvert();
            commitOrRollbackTransaction();
        } finally {
            cleanUp(true, null);
        }

        if (originalThreeD == null) {
            setStatus("Nothing to process");
            return;
        }

        // Perform batch conversion
        ThreeDService service = Framework.getLocalService(ThreeDService.class);
        setStatus("Batch conversion");
        List<Blob> blobs = service.batchConvert(originalThreeD);

        // Saving thumbnail to the document
        setStatus("Saving thumbnail");
        List<Blob> renderBlobs = BatchConverterHelper.getRenders(blobs);

        if (!renderBlobs.isEmpty() && renderBlobs.size() == 1) {
            try {
                startTransaction();
                openSystemSession();
                DocumentModel doc = session.getDocument(new IdRef(docId));
                saveNewThumbnail(doc, renderBlobs.get(0));
                commitOrRollbackTransaction();
            } finally {
                cleanUp(true, null);
            }
        }

        setStatus("Converting Collada to glTF");
        List<TransmissionThreeD> colladaThreeDs = BatchConverterHelper.getTransmissons(blobs);
        List<TransmissionThreeD> transmissionThreeDs = colladaThreeDs.stream()
                                                                     .map(service::convertColladaToglTF)
                                                                     .collect(Collectors.toList());

        startTransaction();
        setStatus("Saving transmission formats");
        openSystemSession();
        DocumentModel doc = session.getDocument(new IdRef(docId));
        saveNewTransmissionThreeDs(doc, transmissionThreeDs);

        fireThreeDConversionsDoneEvent(doc);
        setStatus("Done");
    }

    protected ThreeD getThreeDToConvert() {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        ThreeDDocument threedDocument = doc.getAdapter(ThreeDDocument.class);
        ThreeD threed = threedDocument.getThreeD();
        if (threed == null) {
            log.warn("No original 3d to process for: " + doc);
        }
        return threed;
    }

    protected void saveNewTransmissionThreeDs(DocumentModel doc, List<TransmissionThreeD> transmissionThreeDs) {
        // XXX save transmission threed
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);
    }

    protected void saveNewThumbnail(DocumentModel doc, Blob transmissionThreeD) {
        // XXX save blob as thumbnail on document model

        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);
    }

    /**
     * Fire a {@code THREED_CONVERSIONS_DONE_EVENT} event when no other ThreeDBatchUpdateWork is scheduled for this
     * document.
     *
     * @since 5.8
     */
    protected void fireThreeDConversionsDoneEvent(DocumentModel doc) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        List<String> workIds = workManager.listWorkIds(CATEGORY_THREED_CONVERSION, null);
        String idPrefix = computeIdPrefix(repositoryName, docId);
        int worksCount = 0;
        for (String workId : workIds) {
            if (workId.startsWith(idPrefix)) {
                if (++worksCount > 1) {
                    // another work scheduled
                    return;
                }
            }
        }

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(THREED_CONVERSIONS_DONE_EVENT);
        Framework.getLocalService(EventService.class).fireEvent(event);
    }

}