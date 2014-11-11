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
package org.nuxeo.ecm.platform.queue.api;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.Map;

// FIXME: what does "orphaned" mean?
/**
 * Infos about an handled content. Orphaned
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueItem {

    /**
     * Gets the handled content.
     *
     * @return the handled content
     */
    QueueContent getHandledContent();

    /**
     * Identifies the server handling the content.
     */
    URI getHandlingServerID();

    /**
     * Gets the status.
     *
     * @return the status
     */
    QueueItemState getStatus();

    /**
     * Checks if is orphaned.
     *
     * @return true, if is orphaned
     */
    boolean isOrphaned();

    /**
     * Gets the first handling date.
     *
     * @return the first handling date
     */
    Date getFirstHandlingDate();

    /**
     * Gets the last handling date.
     *
     * @return the last handling date
     */
    Date getLastHandlingDate();

    /**
     * Gets the handling count.
     *
     * @return the handling count
     */
    int getHandlingCount();

    /**
     * Gets the additionalnfos.
     *
     * @return the additionalnfos
     */
    Map<String, Serializable> getAdditionalnfos();

}
