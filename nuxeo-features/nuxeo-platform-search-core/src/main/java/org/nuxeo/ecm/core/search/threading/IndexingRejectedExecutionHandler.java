/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.threading;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public class IndexingRejectedExecutionHandler implements
        RejectedExecutionHandler {

    private static final Log log = LogFactory.getLog(IndexingRejectedExecutionHandler.class);

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        Task task = (Task) r;
        log.debug("Unable to execute the "
                + task.getClass().getSimpleName()
                + " for "
                + (task.getResources() == null ? "document: "
                        + task.getDocumentRef() : "resources: "
                        + task.getResources()));
    }
}
