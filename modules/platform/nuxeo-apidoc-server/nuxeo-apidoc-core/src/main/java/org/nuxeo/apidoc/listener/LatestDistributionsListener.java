/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.apidoc.listener;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.nuxeo.apidoc.snapshot.DistributionSnapshot.PROP_LATEST_FT;
import static org.nuxeo.apidoc.snapshot.DistributionSnapshot.PROP_LATEST_LTS;
import static org.nuxeo.apidoc.snapshot.DistributionSnapshot.TYPE_NAME;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Listener used to keep only one latestLTS, or latestFT When a Distribution is created or modified with the flag; it
 * makes it atomic.
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class LatestDistributionsListener implements EventListener {
    protected static final String DISTRIBUTION_QUERY = "Select * from %s where %s = 1";

    @Override
    public void handleEvent(Event event) {
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel srcDoc = ctx.getSourceDocument();
        if (!TYPE_NAME.equals(srcDoc.getType())) {
            return;
        }

        CoreSession session = ctx.getCoreSession();
        List<String> flags = Arrays.asList(PROP_LATEST_FT, PROP_LATEST_LTS);
        flags.forEach(flag -> {
            if (isFalse((Boolean) srcDoc.getPropertyValue(flag))) {
                return;
            }

            String query = String.format(DISTRIBUTION_QUERY, TYPE_NAME, flag);
            session.query(query)
                   .stream()
                   .filter(doc -> srcDoc.getId() == null || !doc.getId().equals(srcDoc.getId()))
                   .forEach(doc -> {
                doc.setPropertyValue(flag, false);
                session.saveDocument(doc);
            });
        });
    }

}
