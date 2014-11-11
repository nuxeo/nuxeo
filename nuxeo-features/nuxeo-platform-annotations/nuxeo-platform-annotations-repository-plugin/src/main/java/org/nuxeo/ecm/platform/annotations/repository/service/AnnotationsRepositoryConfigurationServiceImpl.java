/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public class AnnotationsRepositoryConfigurationServiceImpl implements
        AnnotationsRepositoryConfigurationService {

    private final Map<String, AnnotatedDocumentEventListener> listeners = new HashMap<String, AnnotatedDocumentEventListener>();

    private GraphManagerEventListener graphManagerEventListener;

    public GraphManagerEventListener getGraphManagerEventListener() {
        return graphManagerEventListener;
    }

    public void setGraphManagerEventListener(
            GraphManagerEventListener graphManagerEventListener) {
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
