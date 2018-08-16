/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.segment.io.listener;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;
import org.nuxeo.segment.io.SegmentIOMapper;

public class SegmentIOAsyncListener implements PostCommitFilteringEventListener {

    @Override
    public boolean acceptEvent(Event event) {
        SegmentIO service = Framework.getService(SegmentIO.class);
        return service.getMappedEvents().contains(event.getName());
    }

    @Override
    public void handleEvent(EventBundle bundle) {

        SegmentIO service = Framework.getService(SegmentIO.class);

        List<String> eventToProcess = service.getMappedEvents()
                                             .stream()
                                             .filter(event -> bundle.containsEventName(event))
                                             .collect(Collectors.toList());

        Map<String, List<SegmentIOMapper>> event2Mappers = service.getMappers(eventToProcess);

        try {
            // Force system login in order to have access to user directory
            LoginContext login = Framework.login();
            try {
                processEvents(event2Mappers, bundle);
            } finally {
                if (login != null) {
                    login.logout();
                }
            }
        } catch (LoginException e) {
            throw new NuxeoException(e);
        }
    }

    protected void processEvents(Map<String, List<SegmentIOMapper>> event2Mappers, EventBundle bundle) {

        for (Event event : bundle) {
            List<SegmentIOMapper> mappers = event2Mappers.get(event.getName());
            if (mappers == null || mappers.size() == 0) {
                continue;
            }

            for (SegmentIOMapper mapper : mappers) {
                Map<String, Object> ctx = new HashMap<>();

                NuxeoPrincipal principal = event.getContext().getPrincipal();
                SegmentIO service = Framework.getService(SegmentIO.class);
                if (!service.mustTrackprincipal(principal.getName())) {
                    break;
                }

                ctx.put("event", event);
                ctx.put("eventContext", event.getContext());
                ctx.put("principal", principal);
                if (event.getContext() instanceof DocumentEventContext) {
                    DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
                    ctx.put("doc", docCtx.getSourceDocument());
                    ctx.put("repository", docCtx.getRepositoryName());
                    ctx.put("session", docCtx.getCoreSession());
                    ctx.put("dest", docCtx.getDestination());
                }

                Map<String, Serializable> mapped = mapper.getMappedData(ctx);
                if (mapper.isIdentify()) {
                    service.identify(principal, mapped);
                } else if (mapper.isPage()) {
                    service.page(principal, getNameWithDefault(event, "page"), mapped);
                } else if (mapper.isScreen()) {
                    service.screen(principal, getNameWithDefault(event, "screen"), mapped);
                } else {
                    service.track(principal, event.getName(), mapped);
                }
            }
        }
    }

    protected String getNameWithDefault(Event e, String defaultName) {
        return (String) e.getContext().getProperties().getOrDefault("name", defaultName);
    }
}
