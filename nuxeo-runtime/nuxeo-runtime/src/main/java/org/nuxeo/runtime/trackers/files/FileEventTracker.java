/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.trackers.files;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileCleaningTracker;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.trackers.concurrent.ThreadEventHandler;
import org.nuxeo.runtime.trackers.concurrent.ThreadEventListener;

/**
 * Files event tracker which delete files once the runtime leave the threads or
 * at least once the associated marker object is garbaged.
 *
 * Note: for being backward compatible you may disable the thread events
 * tracking by black-listing the default configuration component
 * "org.nuxeo.runtime.trackers.files.threadstracking.config" in the runtime.
 *
 * This could be achieved by editing the "blacklist" file in your
 * 'config' directory or using the @{link BlacklistComponent} annotation
 * on your test class.
 *
 * @author Stephane Lacoin at Nuxeo (aka matic)
 * @since 6.0
 * @see ThreadEventHandler
 */
public class FileEventTracker extends DefaultComponent {

    protected class GCDelegate implements FileEventHandler {
        protected FileCleaningTracker delegate = new FileCleaningTracker();

        @Override
        public void onFile(File file, Object marker) {
            delegate.track(file, marker);
        }
    }

    protected class ThreadDelegate implements FileEventHandler {

        protected final boolean isLongRunning;

        protected final Thread owner = Thread.currentThread();

        protected final Set<File> files = new HashSet<File>();

        protected ThreadDelegate(boolean isLongRunning) {
            this.isLongRunning = isLongRunning;
        }

        @Override
        public void onFile(File file, Object marker) {
            if (!owner.equals(Thread.currentThread())) {
                return;
            }
            if (isLongRunning) {
                gc.onFile(file, marker);
            }
            files.add(file);
        }

    }

    @XObject("enableThreadsTracking")
    public static class EnableThreadsTracking {

    }

    protected final GCDelegate gc = new GCDelegate();

    protected static FileEventTracker self;

    protected final ThreadLocal<ThreadDelegate> threads = new ThreadLocal<>();

    protected final ThreadEventListener threadsListener = new ThreadEventListener(
            new ThreadEventHandler() {

                @Override
                public void onEnter(boolean isLongRunning) {
                    setThreadDelegate(isLongRunning);
                }

                @Override
                public void onLeave() {
                    resetThreadDelegate();
                }

            });

    protected final FileEventListener filesListener = new FileEventListener(
            new FileEventHandler() {

                @Override
                public void onFile(File file, Object marker) {
                    onContext().onFile(file, marker);
                }
            });

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        self = this;
        filesListener.install();
        setThreadDelegate(false);
    }

    @Override
    public int getApplicationStartedOrder() {
    	return Integer.MAX_VALUE;
    }
    
    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        resetThreadDelegate();
        Framework.addListener(new RuntimeServiceListener() {
			
			@Override
			public void handleEvent(RuntimeServiceEvent event) {
				if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
					return;
				}
				Framework.removeListener(this);
				setThreadDelegate(false);
			}
		});
    }
    
    @Override
    public void deactivate(ComponentContext context) throws Exception {
    	resetThreadDelegate();
        if (Framework.getService(EventService.class) != null) {
            if (threadsListener.isInstalled()) {
                threadsListener.uninstall();
            }
            filesListener.uninstall();
        }
        self = null;
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof EnableThreadsTracking) {
            threadsListener.install();
        } else {
            super.registerContribution(contribution, extensionPoint,
                    contributor);
        }

    }

    protected FileEventHandler onContext() {
        FileEventHandler actual = threads.get();
        if (actual == null) {
            actual = gc;
        }
        return actual;
    }

    protected void setThreadDelegate(boolean isLongRunning) {
        if (threads.get() != null) {
            throw new IllegalStateException("Thread delegate already installed");
        }
        threads.set(new ThreadDelegate(isLongRunning));
    }

    protected void resetThreadDelegate() throws IllegalStateException {
        ThreadDelegate actual = threads.get();
        if (actual == null) {
            throw new IllegalStateException("Thread delegate not installed");
        }
        try {
            for (File file : actual.files) {
                file.delete();
            }
        } finally {
            threads.remove();
        }
    }

}
