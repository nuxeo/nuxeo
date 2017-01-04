/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.snapshot.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.snapshot.Snapshotable;

/**
 * Listener snapshoting documents with the {@link Snapshotable#FACET} facet if the property
 * {@code snapshotVersioningOption} is set.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class SnapshotableListener implements EventListener {

    private static final Log log = LogFactory.getLog(SnapshotableListener.class);

    public static final String SNAPSHOT_VERSIONING_OPTION_KEY = "snapshotVersioningOption";

    @Override
    public void handleEvent(Event event) {
        String eventName = event.getName();
        if (!DOCUMENT_UPDATED.equals(eventName)) {
            return;
        }

        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (doc.isProxy() || doc.isVersion() || !doc.hasFacet(Snapshotable.FACET)) {
            return;
        }

        String versioningOption = (String) ctx.getProperty(SNAPSHOT_VERSIONING_OPTION_KEY);
        if (versioningOption == null) {
            return;
        }

        VersioningOption option;
        try {
            option = VersioningOption.valueOf(versioningOption);
        } catch (IllegalArgumentException e) {
            log.error(String.format("Unknown versioning option value '%s': %s", versioningOption, e.getMessage()));
            log.debug(e, e);
            return;
        }
        if (option == VersioningOption.NONE) {
            return;
        }

        try {
            Snapshotable snapshotable = doc.getAdapter(Snapshotable.class);
            snapshotable.createSnapshot(option);
        } catch (NuxeoException e) {
            event.markRollBack(e.getMessage(), e);
            throw e;
        }

    }
}
