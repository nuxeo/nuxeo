/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.trackers.concurrent;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.trackers.files.FileEventTracker;

/**
 * Runtime events that be fired once a thread is about to enter in the nuxeo
 * runtime or leave it. Could be used for cleaning resource such as the
 * {@link FileEventTracker}.
 *
 * Producers should use the static {@link ThreadEvent#onEnter(Object, boolean)}
 * and {@link ThreadEvent#onLeave(Object)} factory methods and fire events by
 * invoking the event's {@link ThreadEvent#send()} method.
 *
 * Consumers should implements the {@link ThreadEventHandler} interface and
 * register it in the {@link EventService} using the {@link ThreadEventListener}
 * wrapper.
 *
 * @since 5.9.6
 * @author Stephane Lacoin at Nuxeo (aka matic)
 *
 */
public abstract class ThreadEvent extends Event {

    public ThreadEvent(Class<? extends ThreadEvent> type, Object source,
            Object data) {
        super(ThreadEvent.class.getName(), type.getSimpleName(), source, data);
    }

    protected static class EnterEvent extends ThreadEvent {

        public EnterEvent(Object source, boolean isLongRunning) {
            super(EnterEvent.class, source, isLongRunning);
        }

        public boolean isLongRunning() {
            return Boolean.valueOf((Boolean) getData());
        }

        @Override
        public void handle(ThreadEventHandler handler) {
            handler.onEnter(Boolean.valueOf((Boolean) getData()));
        }
    }

    protected static class LeaveEvent extends ThreadEvent {
        public LeaveEvent(Object source) {
            super(LeaveEvent.class, source, null);
        }

        @Override
        public void handle(ThreadEventHandler handler) {
            handler.onLeave();
        }
    }

    public abstract void handle(ThreadEventHandler handler);

    public void send() {
        Framework.getLocalService(EventService.class).sendEvent(this);
    }

    public static ThreadEvent onEnter(Object source, boolean isLongRunning) {
        return new EnterEvent(source, isLongRunning);
    }

    public static ThreadEvent onLeave(Object source) {
        return new LeaveEvent(source);
    }

    public static void listen(ThreadEventListener aListener) {
        Framework.getLocalService(EventService.class).addListener(
                ThreadEvent.class.getName(), aListener);
    }

    public static void ignore(ThreadEventListener aListener) {
        Framework.getLocalService(EventService.class).removeListener(
                ThreadEvent.class.getName(), aListener);
    }
}
