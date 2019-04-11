/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.opencmis.impl;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tracks Download Events.
 */
public class DownloadListener implements EventListener {

    public static List<String> MESSAGES = new ArrayList<>();

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        String comment = (String) ctx.getProperty("comment");
        String downloadReason = (String) ((Map<?,?>) ctx.getProperty("extendedInfos")).get("downloadReason");
        MESSAGES.add(event.getName() + ":comment=" + comment + ",downloadReason=" + downloadReason);
    }

    public static void clearMessages() {
        MESSAGES.clear();
    }

    public static List<String> getMessages() {
        return MESSAGES;
    }

}
