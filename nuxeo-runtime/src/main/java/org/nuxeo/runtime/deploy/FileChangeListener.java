/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.deploy;

// FIXME: interface has changed and this example is no more appropriate.
/**
 * An example of listener implementation:
 * <pre>
 * public class MyListener implements FileChangeListener {
 *   long lastNotif = 0;
 *   public void fileChanged(File file, long since, long now) {
 *     if (now == lastNotifFlush) return;
 *     if (isIntersetedInFile(file)) {
 *       lastNotif = now;
 *       flushCache(); // flush internal cache because file on disk changed
 *     }
 *   }
 * }
 * </pre>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface FileChangeListener {

    /**
     * Notifies that the given file changed.
     *
     * @param entry
     * @param now the time stamp when the change was detected.
     *      This value can be used as a notification ID by listeners to avoid
     *      multiple processing for notification that will send multiple events
     */
    void fileChanged(FileChangeNotifier.FileEntry entry, long now) throws Exception;

}
