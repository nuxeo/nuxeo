/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.core.event.async;

import java.util.Map;

import org.nuxeo.ecm.core.event.Event;

/**
 * The EventTransformer takes an event to extract context information from the event in order to add it to the
 * EventRecord created by {@link EventsStreamListener}.
 * 
 * @since XXX
 */
public abstract class EventTransformer {

    protected String id;

    public String getId() {
        return id == null ? this.getClass().getSimpleName() : id;
    }

    protected EventTransformer withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Filter to only process the events the transformer knows how to process.
     *
     * @param event The Nuxeo Event triggered.
     * @return True if the transformer can process the event.
     */
    public abstract boolean accept(Event event);

    /**
     * Build a map of String representing the context information extracted from the Event in order to add them to the
     * EventRecord built.
     * 
     * @param event The Nuxeo Event triggered.
     * @return A map with context information.
     */
    public abstract Map<String, String> buildEventRecordContext(Event event);
}
