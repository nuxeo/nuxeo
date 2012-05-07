/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core.versioning;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 * Async listener that is in charge to delete the versions. Before running the
 * delete operation on the versions passed as argument of the event, it will
 * call the registred {@link OrphanVersionRemovalFilter} to allow them to mark
 * some of the orphan versions to be kept.
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class OrphanVersionRemoverListener implements PostCommitEventListener {

    protected static final Log log = LogFactory.getLog(OrphanVersionRemoverListener.class);

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        if (events.containsEventName(DefaultVersionRemovalPolicy.ORPHAN_VERSION_REMOVE)) {
            for (Event event : events) {
                if (!event.getName().equals(
                        DefaultVersionRemovalPolicy.ORPHAN_VERSION_REMOVE)) {
                    continue;
                }
                EventContext ctx = event.getContext();
                CoreSession session = ctx.getCoreSession();
                Object[] args = ctx.getArguments();
                if (args.length == 2) {
                    ShallowDocumentModel deletedLiveDoc = (ShallowDocumentModel) args[0];
                    List<String> versionUUIDs = (List<String>) args[1];
                    removeIfPossible(session, deletedLiveDoc, versionUUIDs);
                }
            }
        }
    }

    protected List<OrphanVersionRemovalFilter> getFilters() {
        return Framework.getLocalService(CoreService.class).getOrphanVersionRemovalFilters();
    }

    protected void removeIfPossible(CoreSession session,
            ShallowDocumentModel deletedLiveDoc, List<String> versionUUIDs)
            throws ClientException {

        for (OrphanVersionRemovalFilter filter : getFilters()) {
            versionUUIDs = filter.getRemovableVersionIds(session,
                    deletedLiveDoc, versionUUIDs);
            if (versionUUIDs.size() == 0) {
                break;
            }
        }

        for (String id : versionUUIDs) {
            log.debug("Removing version: " + id);
            session.removeDocument(new IdRef(id));
        }
    }

}
