/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.snapshot;

import static org.nuxeo.snapshot.Snapshotable.ABOUT_TO_CREATE_LEAF_VERSION_EVENT;
import static org.nuxeo.snapshot.Snapshotable.ROOT_DOCUMENT_PROPERTY;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class CreateLeafListener implements EventListener {

    public static final String DO_NOT_CHANGE_CHILD_FLAG = "hold";

    @Override
    public void handleEvent(Event event) {
        if (!event.getName().equals(ABOUT_TO_CREATE_LEAF_VERSION_EVENT)) {
            return;
        }

        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel source = ctx.getSourceDocument();
        DocumentModel root = (DocumentModel) ctx.getProperty(ROOT_DOCUMENT_PROPERTY);

        String rootDescription = (String) root.getPropertyValue("dc:description");
        if (StringUtils.isEmpty(rootDescription) || !rootDescription.contains(DO_NOT_CHANGE_CHILD_FLAG)) {
            source.setPropertyValue("dc:description", "XOXO");
        }
    }
}
