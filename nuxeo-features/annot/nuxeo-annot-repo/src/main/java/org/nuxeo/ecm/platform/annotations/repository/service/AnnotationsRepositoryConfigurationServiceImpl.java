/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexandre Russel
 */
public class AnnotationsRepositoryConfigurationServiceImpl implements AnnotationsRepositoryConfigurationService {

    private final Map<String, AnnotatedDocumentEventListener> listeners = new HashMap<String, AnnotatedDocumentEventListener>();

    private GraphManagerEventListener graphManagerEventListener;

    public GraphManagerEventListener getGraphManagerEventListener() {
        return graphManagerEventListener;
    }

    public void setGraphManagerEventListener(GraphManagerEventListener graphManagerEventListener) {
        this.graphManagerEventListener = graphManagerEventListener;
    }

    private final List<String> eventIds = new ArrayList<String>();

    public void addEventListener(String listenerName, AnnotatedDocumentEventListener listener) {
        listeners.put(listenerName, listener);
    }

    public List<AnnotatedDocumentEventListener> getEventListeners() {
        return new ArrayList<AnnotatedDocumentEventListener>(listeners.values());
    }

    public List<String> getEventIds() {
        return eventIds;
    }

    public void addEventId(String eventId) {
        eventIds.add(eventId);
    }

}
