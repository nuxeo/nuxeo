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
 * Contributors:    Stephane Lacoin at Nuxeo (aka matic),
 *                  Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.api;

/**
 * Route content to dedicated queues.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueHandler {

    /**
     * Handle content.
     * 
     * @param content the content
     */
    void handleNewContent(QueueContent content);

    /**
     * Handle content if unknown.
     * 
     * @param content the content
     */
    void handleNewContentIfUnknown(QueueContent content) throws QueueException;

    /**
     * Handle end of processing (remove content from queue).
     * 
     * @param content the content
     */
    void handleEndOfProcessing(QueueContent content);

}
