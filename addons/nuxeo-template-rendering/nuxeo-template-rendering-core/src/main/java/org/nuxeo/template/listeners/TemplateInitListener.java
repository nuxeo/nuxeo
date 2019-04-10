/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.template.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * Listener to manage initialization :
 * <ul>
 * <li>of the TemplateSourceDocument : init the parameters</li>
 * <li>of the other DocumentModels if they need to be automatically associated to a template</li>
 * </ul>
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class TemplateInitListener implements EventListener {

    private static final Log log = LogFactory.getLog(TemplateInitListener.class);

    @Override
    public void handleEvent(Event event) {

        EventContext ctx = event.getContext();

        if (ABOUT_TO_CREATE.equals(event.getName()) || BEFORE_DOC_UPDATE.equals(event.getName())) {
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;

                DocumentModel targetDoc = docCtx.getSourceDocument();

                if (targetDoc.isVersion()) {
                    return;
                }

                TemplateSourceDocument templateDoc = targetDoc.getAdapter(TemplateSourceDocument.class);
                if (templateDoc != null) {
                    // init types bindings
                    templateDoc.initTypesBindings();

                    // init template source
                    List<TemplateInput> params = templateDoc.getParams();
                    if (params == null || params.size() == 0 || isBlobDirty(targetDoc)) {
                        templateDoc.initTemplate(false);
                    }
                } else {
                    TemplateBasedDocument tmplBased = targetDoc.getAdapter(TemplateBasedDocument.class);
                    if (tmplBased == null) {
                        // if not templateBased see if we must add the facet
                        // because of the type binding
                        // or template selection as main file
                        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);

                        String targetTemplateUid = (String) targetDoc.getContextData("templateId");
                        if ("none".equals(targetTemplateUid)) {
                            targetTemplateUid = null;
                        }
                        List<String> templatesUids = new ArrayList<String>();

                        if (targetTemplateUid != null) {
                            templatesUids.add(targetTemplateUid);
                        }

                        List<String> tuids = tps.getTypeMapping().get(targetDoc.getType());
                        if (tuids != null) {
                            for (String tuid : tuids) {
                                // let's be paranoid
                                if (!templatesUids.contains(tuid)) {
                                    templatesUids.add(tuid);
                                }
                            }
                        }

                        // do the association
                        if (templatesUids.size() > 0) {
                            for (String tuid : templatesUids) {
                                DocumentRef templateRef = new IdRef(tuid);
                                // check if source template is visible
                                if (docCtx.getCoreSession().exists(templateRef)) {
                                    DocumentModel sourceTemplateDoc = docCtx.getCoreSession().getDocument(templateRef);
                                    if (!LifeCycleConstants.DELETED_STATE.equals(sourceTemplateDoc.getCurrentLifeCycleState())) {
                                    	tps.makeTemplateBasedDocument(targetDoc, sourceTemplateDoc, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean isBlobDirty(DocumentModel targetDoc) {
        BlobHolder bh = targetDoc.getAdapter(BlobHolder.class);
        Blob mainBlob = bh.getBlob();
        if (mainBlob != null && mainBlob.getDigest() == null) {
            // Blobs that have not changed should be SQL Blobs and have a digest
            return true;
        } else {
            // newly uploaded Blob should be FileBlob, and anyway Digest can not
            // have been computed so far
            return false;
        }
    }
}
