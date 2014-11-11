/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */

package org.nuxeo.ecm.platform.queue.core;

import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueExecutor;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;

/**
 * handle end of processing only if the static boolean variable is accessible
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
class FakeOrphansExecutor implements QueueExecutor {

    public static boolean shouldHandleEndOfProcessing = false;

    public void execute(QueueContent content, QueueHandler handler) {
        // do nothing

        if (shouldHandleEndOfProcessing) {
            handler.handleEndOfProcessing(content);
        }
    }
}