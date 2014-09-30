package org.nuxeo.runtime.trackers.files;

import java.io.File;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

public class FileEvent extends Event {

    protected FileEvent(Object source, File aFile, Object aMarker) {
        super(FileEvent.class.getName(), FileEvent.class
            .getName(), source, new Object[] { aFile, aMarker });
    }

    public static void listen(FileEventListener aListener) {
        Framework.getLocalService(EventService.class).addListener(FileEvent.class.getName(), aListener);
    }

    public static void ignore(FileEventListener aListener) {
        Framework.getLocalService(EventService.class).removeListener(FileEvent.class.getName(), aListener);
    }

    public void send() {
        Framework.getLocalService(EventService.class).sendEvent(this);
    }

    public void handle(FileEventHandler handler) {
        handler.onFile(getFile(), getMarker());
    }

    protected File getFile() {
        return (File)((Object[])getData())[0];
    }

    protected Object getMarker() {
        return ((Object[])getData())[1];
    }

    public static FileEvent onFile(Object source, File aFile, Object aMarker) {
        return new FileEvent(source, aFile, aMarker);
    }
}
