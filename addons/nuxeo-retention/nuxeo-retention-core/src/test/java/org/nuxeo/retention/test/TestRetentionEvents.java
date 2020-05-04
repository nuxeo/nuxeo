/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.retention.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.nuxeo.ecm.core.api.CoreSession.RETAIN_UNTIL_INDETERMINATE;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
public class TestRetentionEvents extends RetentionTestCase {

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void shouldNotifyEventsWhenModifyLegalHold() {
        session.makeRecord(file.getRef());

        setLegalHoldAndCheckEvents(true, "I put the legal hold", DocumentEventTypes.BEFORE_SET_LEGAL_HOLD,
                DocumentEventTypes.AFTER_SET_LEGAL_HOLD);

        setLegalHoldAndCheckEvents(false, "I remove the legal hold", DocumentEventTypes.BEFORE_REMOVE_LEGAL_HOLD,
                DocumentEventTypes.AFTER_REMOVE_LEGAL_HOLD);

    }

    protected void setLegalHoldAndCheckEvents(boolean hold, String comment, String... expectedEvents) {
        try (CapturingEventListener listener = new CapturingEventListener(expectedEvents)) {
            session.setLegalHold(file.getRef(), hold, comment);
            transactionalFeature.nextTransaction();

            String[] eventsNames = listener.streamCapturedEvents().map(Event::getName).toArray(String[]::new);
            assertArrayEquals(expectedEvents, eventsNames);
            listener.streamCapturedEvents()
                    .forEach(ev -> assertEquals("Event: " + ev.getName(), comment,
                            ev.getContext().getProperties().get("comment")));
        }
    }

    @Test
    public void shouldNotifyEventsWhenModifyRetention() {
        session.makeRecord(file.getRef());
        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.DAY_OF_MONTH, 5);

        // current retention is null
        setRetentionAndCheckEvents(retainUntil, DocumentEventTypes.BEFORE_SET_RETENTION,
                DocumentEventTypes.AFTER_SET_RETENTION);

        // extend the retention
        retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.DAY_OF_MONTH, 25);
        setRetentionAndCheckEvents(retainUntil, DocumentEventTypes.BEFORE_EXTEND_RETENTION,
                DocumentEventTypes.AFTER_EXTEND_RETENTION);

        // current retention is indeterminate
        setRetentionAndCheckEvents(RETAIN_UNTIL_INDETERMINATE, DocumentEventTypes.BEFORE_EXTEND_RETENTION,
                DocumentEventTypes.AFTER_EXTEND_RETENTION);

        // modify an indeterminate once
        retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.DAY_OF_MONTH, 10);
        setRetentionAndCheckEvents(retainUntil, DocumentEventTypes.BEFORE_SET_RETENTION,
                DocumentEventTypes.AFTER_SET_RETENTION);
    }

    protected void setRetentionAndCheckEvents(Calendar retainUntil, String... expectedEvents) {
        try (CapturingEventListener listener = new CapturingEventListener(expectedEvents)) {
            session.setRetainUntil(file.getRef(), retainUntil, null);
            transactionalFeature.nextTransaction();

            String[] eventsNames = listener.streamCapturedEvents().map(Event::getName).toArray(String[]::new);
            assertArrayEquals(expectedEvents, eventsNames);
            String expectedComment = RETAIN_UNTIL_INDETERMINATE.compareTo(retainUntil) == 0 ? ""
                    : retainUntil.toInstant().toString();
            listener.streamCapturedEvents()
                    .forEach(ev -> assertEquals("Event: " + ev.getName(), expectedComment,
                            ev.getContext().getProperties().get("comment")));
        }
    }

    @Test
    public void shouldNotNotifyEventsWhenNoRetentionModified() {
        session.makeRecord(file.getRef());
        setRetentionAndCheckEvents(CoreSession.RETAIN_UNTIL_INDETERMINATE, DocumentEventTypes.BEFORE_SET_RETENTION,
                DocumentEventTypes.AFTER_SET_RETENTION);

        // update the retainUntil with the same value, no event should occur
        try (CapturingEventListener listener = new CapturingEventListener()) {
            session.setRetainUntil(file.getRef(), CoreSession.RETAIN_UNTIL_INDETERMINATE, null);
            transactionalFeature.nextTransaction();
            assertFalse(listener.hasBeenFired(DocumentEventTypes.BEFORE_SET_RETENTION));
            assertFalse(listener.hasBeenFired(DocumentEventTypes.AFTER_SET_RETENTION));
            assertFalse(listener.hasBeenFired(DocumentEventTypes.BEFORE_EXTEND_RETENTION));
            assertFalse(listener.hasBeenFired(DocumentEventTypes.AFTER_EXTEND_RETENTION));
        }
    }

}
