package org.nuxeo.runtime.trackers.files;

import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class FileEventListener implements EventListener {

    protected final FileEventHandler handler;

    public FileEventListener(FileEventHandler anHandler) {
        handler = anHandler;
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event anEvent) {
        ((FileEvent)anEvent).handle(handler);
    }

    public void install() {
        FileEvent.listen(this);
    }

    public void uninstall() {
        FileEvent.ignore(this);
    }
}
