/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.queue.api;

import java.io.Serializable;

/**
 * When a new content comes into the queue, the user is call-backed using this interface
 * for processing the content. It's the user authority to notify the queue through the provided
 * handler about the content processing termination.
 */
public interface QueueProcessor<C extends Serializable> {

    /**
     * The user call-back entry point
     *
     * @param content
     * @param handler
     */
    void process(QueueInfo<C> info);

}
