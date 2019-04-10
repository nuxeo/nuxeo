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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;

import org.nuxeo.ecm.platform.filemanager.core.listener.MimetypeIconUpdater;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.threed.BatchConverterHelper;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.ThreeDDocument;
import org.nuxeo.ecm.platform.threed.ThreeDInfo;
import org.nuxeo.ecm.platform.threed.ThreeDRenderView;
import org.nuxeo.ecm.platform.threed.TransmissionThreeD;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_TYPE;
import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.MAIN_INFO_PROPERTY;
import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.RENDER_VIEWS_PROPERTY;
import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.TRANSMISSIONS_PROPERTY;

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
        if (isWorkInstanceSuspended()) {
            return;
        }
        // Extract
        setStatus("Extracting");
        setProgress(Progress.PROGRESS_INDETERMINATE);

        ThreeD originalThreeD = null;
        try {
            openSystemSession();
            DocumentModel doc = session.getDocument(new IdRef(docId));
            originalThreeD = getThreeDToConvert(doc);
            commitOrRollbackTransaction();
        } finally {
            cleanUp(true, null);
        }

        if (originalThreeD == null) {
            setStatus("Nothing to process");
            return;
        }

        if (isWorkInstanceSuspended()) {
            return;
        }
        // Perform batch conversion
        setStatus("Batch conversion");
        ThreeDService service = Framework.getService(ThreeDService.class);
        BlobHolder batch;
        try {
            batch = service.batchConvert(originalThreeD);
        } catch (ConverterNotRegistered e) {
            return;
        }

        if (isWorkInstanceSuspended()) {
            return;
        }
        // Saving thumbnail to the document
        setStatus("Saving thumbnail");
        List<ThreeDRenderView> threeDRenderViews = BatchConverterHelper.getRenders(batch);
        long numRenderViews = service.getAutomaticRenderViews().stream().filter(RenderView::isEnabled).count();
        if (!threeDRenderViews.isEmpty() && threeDRenderViews.size() == numRenderViews) {
            try {
                startTransaction();
                openSystemSession();
                DocumentModel doc = session.getDocument(new IdRef(docId));
                saveNewRenderViews(doc, threeDRenderViews);
                commitOrRollbackTransaction();
            } finally {
                cleanUp(true, null);
            }
        }

        if (isWorkInstanceSuspended()) {
            return;
        }
        // Save 3D blob information
        setStatus("Saving 3D information on main content");
        List<BlobHolder> resources = BatchConverterHelper.getResources(batch);
        ThreeDInfo mainInfo;
        mainInfo = BatchConverterHelper.getMainInfo(batch, resources);
        if (mainInfo != null) {
            try {
                startTransaction();
                openSystemSession();
                DocumentModel doc = session.getDocument(new IdRef(docId));
                saveMainInfo(doc, mainInfo);
                commitOrRollbackTransaction();
            } finally {
                cleanUp(true, null);
            }
        }

        if (isWorkInstanceSuspended()) {
            return;
        }
        // Convert transmission formats
        setStatus("Converting Collada to glTF");

        List<TransmissionThreeD> colladaThreeDs = BatchConverterHelper.getTransmissions(batch, resources);
        List<TransmissionThreeD> transmissionThreeDs = colladaThreeDs.stream()
                                                                     .map(service::convertColladaToglTF)
                                                                     .collect(Collectors.toList());

        if (isWorkInstanceSuspended()) {
            return;
        }
        // Save transmission formats
        setStatus("Saving transmission formats");
        startTransaction();
        openSystemSession();
        DocumentModel doc = session.getDocument(new IdRef(docId));
        saveNewTransmissionThreeDs(doc, transmissionThreeDs);

        if (isWorkInstanceSuspended()) {
            return;
        }
        // Finalize
        fireThreeDConversionsDoneEvent(doc);
        setStatus("Done");
    }

    protected ThreeD getThreeDToConvert(DocumentModel doc) {
        ThreeDDocument threedDocument = doc.getAdapter(ThreeDDocument.class);
        ThreeD threed = threedDocument.getThreeD();
        if (threed == null) {
            log.warn("No original 3d to process for: " + doc);
        }
        return threed;
    }

    protected void saveNewProperties(DocumentModel doc, Serializable properties, String schema) {
        doc.setPropertyValue(schema, properties);
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);
    }

    protected void saveMainInfo(DocumentModel doc, ThreeDInfo info) {
        saveNewProperties(doc, (Serializable) info.toMap(), MAIN_INFO_PROPERTY);

    }

    protected void saveNewTransmissionThreeDs(DocumentModel doc, List<TransmissionThreeD> transmissionThreeDs) {
        List<Map<String, Serializable>> transmissionList = new ArrayList<>();
        transmissionList.addAll(
                transmissionThreeDs.stream().map(TransmissionThreeD::toMap).collect(Collectors.toList()));
        saveNewProperties(doc, (Serializable) transmissionList, TRANSMISSIONS_PROPERTY);
    }

    protected void saveNewRenderViews(DocumentModel doc, List<ThreeDRenderView> threeDRenderViews) {
        List<Map<String, Serializable>> renderViewList = new ArrayList<>();
        renderViewList.addAll(threeDRenderViews.stream().map(ThreeDRenderView::toMap).collect(Collectors.toList()));

        saveNewProperties(doc, (Serializable) renderViewList, RENDER_VIEWS_PROPERTY);
    }

    /**
     * Fire a {@code THREED_CONVERSIONS_DONE_EVENT} event when no other ThreeDBatchUpdateWork is scheduled for this
     * document.
     */
    protected void fireThreeDConversionsDoneEvent(DocumentModel doc) {
        WorkManager workManager = Framework.getService(WorkManager.class);
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
        Framework.getService(EventService.class).fireEvent(event);
        // force the 3d doc icon
        MimetypeIconUpdater iconUpdater = new MimetypeIconUpdater();
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        MimetypeEntry mimeTypeEntry = mimetypeRegistry.getMimetypeEntryByMimeType(THREED_TYPE);
        iconUpdater.updateIconField(mimeTypeEntry, doc);
    }

}