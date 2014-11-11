/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.work.api.Work;

/**
 * A {@link WorkHolder} adapts a {@link Work} to {@link Runnable} for queuing
 * and execution by a {@link ThreadPoolExecutor}.
 * <p>
 * Calls (indirectly) {@link Work#work} and {@link Work#cleanUp}.
 *
 * @see Work
 * @see Work#work
 * @see Work#cleanUp
 * @see AbstractWork
 * @since 5.8
 */
public class WorkHolder implements Runnable {

    private final Work work;

    public WorkHolder(Work work) {
        this.work = work;
    }

    public static Work getWork(Runnable r) {
        return ((WorkHolder) r).work;
    }

    @Override
    public void run() {
        work.run();
    }

}
