package org.nuxeo.runtime.trackers.concurrent;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

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
        Framework.getService(EventService.class).sendEvent(this);
    }

    public static ThreadEvent onEnter(Object source, boolean isLongRunning) {
        return new EnterEvent(source, isLongRunning);
    }

    public static ThreadEvent onLeave(Object source) {
        return new LeaveEvent(source);
    }

    public static void listen(ThreadEventListener aListener) {
        Framework.getService(EventService.class).addListener(
                ThreadEvent.class.getName(), aListener);
    }

    public static void ignore(ThreadEventListener aListener) {
        Framework.getService(EventService.class).removeListener(
                ThreadEvent.class.getName(), aListener);
    }
}
