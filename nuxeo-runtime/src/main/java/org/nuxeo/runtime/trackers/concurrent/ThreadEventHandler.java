package org.nuxeo.runtime.trackers.concurrent;


public interface ThreadEventHandler  {

    void onEnter(boolean isLongRunning);

    void onLeave();

}
