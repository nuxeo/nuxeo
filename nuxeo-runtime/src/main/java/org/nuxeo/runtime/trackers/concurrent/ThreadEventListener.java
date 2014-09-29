package org.nuxeo.runtime.trackers.concurrent;

import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class ThreadEventListener implements EventListener {

    protected final ThreadEventHandler handler;

    protected boolean installed;

    public ThreadEventListener(ThreadEventHandler anHandler) {
        handler = anHandler;
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event anEvent) {
        ((ThreadEvent) anEvent).handle(handler);
    }

    public boolean isInstalled() {
        return installed;
    }

    public boolean uninstall() {
        if (!installed) {
            return false;
        }
        ThreadEvent.ignore(this);
        return true;
    }

    public boolean install() {
        if (installed) {
            return false;
        }
        ThreadEvent.listen(this);
        return true;
    }

}
