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

/**
 * Informations about an handled content. Content is orphaned when the server that was processing
 * it has died. Such content should be manually re-initiated on another server or just removed from the queue.
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueInfo<C extends Serializable> {

    /**
     * An content can be in the following state:
     * <dl>
     * <dt>Handled</dt>
     * <dd>content is currently handled by system</dd>
     * <dt>Orphaned</dt>
     * <dd>content known by system but not handled</dd>
     * <dt>Blacklisted</dt>
     * <dd>Content is processed and should not be handled anymore</dd>
     * </dl>
     *
     **/
    public enum State {

        Handled,
        Orphaned,
        Blacklisted

    }

    /**
     * Names the info (queueName:...)
     */
    URI getName();

    /**
     * Names the content owner
     *
     * @return the owner name
     */

    URI getOwnerName();

    /**
     * Gives back the handled content
     *
     * @return the handled content
     */
    C getContent();

    /**
     * Names  the server handling the content.
     */
    URI getServerName();

    /**
     * Gets the state.
     *
     * @return the status
     */
    State getState();

    /**
     * Checks if the content should not be processed anymore
     *
     *@ return true, if is blacklisted
     */
    boolean isBlacklisted();

    /**
     * Checks if is orphaned.
     *
     * @return true, if is orphaned
     */
    boolean isOrphaned();

    /**
     * Checks if is handled
     *
     * @return true, if is handled
     */
    boolean isHandled();
    /**
     * Gets the first handling date.
     *
     * @return the first handling date
     */
    Date getFirstHandlingTime();

    /**
     * Gets the last handling date.
     *
     * @return the last handling date
     */
    Date getLastHandlingTime();

    /**
     * Get the blacklist date
     *
     * @return the blacklist date
     */
    Date getBlacklistTime();

    /**
     * Gets the handling count.
     *
     * @return the handling count
     */
    int getHandlingCount();

    /**
     * Retry handling of content
     */
    QueueInfo<C> retry();

    /**
     * Blacklist  content
     */
    QueueInfo<C> blacklist();



}
