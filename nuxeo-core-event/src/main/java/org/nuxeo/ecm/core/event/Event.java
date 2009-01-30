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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event;

import java.io.Serializable;
import java.util.Set;

/**
 * A lightweight object used by core components to notify interested components
 * about events in core.
 * <p>
 * These events should be used by all core components not only by the
 * repository.
 * <p>
 * The events may specify a set of control flags that can be used to control the
 * visibility and the way post commit events are handled. There are 3 types of
 * visibility:
 * <ul>
 * <li>LOCAL - events that are considered being visible to the local machine.
 * <li>PUBLIC - the default - events visible on any machine. Clearing this flag
 * will avoid forwarding the event on remote machines (through JMS or other
 * messaging systems)
 * </ul>
 * There are 2 post commit control flags:
 * <ul>
 * <li>INLINE - if true the event will not be recorded as part of the post
 * commit event bundle. Defaults to false.
 * <li>COMMIT - the event will simulate a commit so that the post commit event
 * bundle will be fired. TYhe COMMIT flag is ignored while in a transaction.
 * </ul>
 *
 * More flags may be added in the future.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Event extends Serializable {

    enum Flag {
        LOCAL, //
        CANCEL, //
        COMMIT, //
        INLINE,
        ROOLBACK
    }

    /**
     * Gets the event name.
     * <p>
     * The name must be unique. It is recommended to use prefixes in the style
     * of java package names to differentiate between similar events that are
     * sent by different components.
     *
     * @return
     */
    String getName();

    /**
     * The time stamp when the event was raised.
     *
     * @return the time stamp as returned by {@link System#currentTimeMillis()}
     */
    long getTime();

    /**
     * Gets the event context.
     * <p>
     * Event contexts give access to the context in which the the event was
     * raised. Event contexts are usually identifying the operation that raised
     * the event. The context is exposing data objects linked to the event like
     * documents and also may give access to the operation that raised the event
     * allowing thus to canceling the operation, to record time spent to set the
     * result status etc.
     *
     * @return the event context
     */
    EventContext getContext();

    /**
     * Gets the set of event flags
     *
     * @return the event flags
     */
    Set<Flag> getFlags();

    /**
     * Cancels this event.
     * <p>
     * This can be used by event listeners to exit the event notification.
     * Remaining event listeners will no more be notified. Note that this is not
     * canceling the underlying operation if any.
     */
    void cancel();

    /**
     * Checks whether the event was canceled.
     *
     * @return true if canceled, false otherwise.
     */
    boolean isCanceled();


    /**
     * Marks transaction for RollBack
     * <p>
     * This will exit the event listeners loop and throw a RuntimeException
     * In JTA container, this will make the global transaction rollback.
     *
     */
    void markRollBack();

    /**
     * Checks whether the event was marked for RollBack
     *
     * @return true if rolledback, false otherwise.
     */
    boolean isMarkedForRollBack();

    /**
     * Whether this event must not be added to a bundle. An event is not inline
     * by default.
     *
     * @return true if the event must be omitted from event bundles, false
     *         otherwise.
     */
    boolean isInline();

    /**
     * Set the inline flag.
     *
     * @param isInline true if the event must not be recorded as part of the
     *            transaction
     * @see #isInline()
     */
    void setInline(boolean isInline);

    /**
     * Tests whether or not this is a commit event. A commit event is triggering
     * the post commit notification and then is reseting the recorded events.
     *
     * @return true if a commit event false otherwise
     */
    boolean isCommitEvent();

    /**
     * Set the commit flag.
     *
     * @param isCommit
     * @see #isCommitEvent()
     */
    void setIsCommitEvent(boolean isCommit);

    /**
     * Tests if this event is local.
     * <p>
     * Local events events are of interest only on the local machine.
     *
     * @return true if private false otherwise
     */
    boolean isLocal();

    /**
     * Sets the local flag.
     *
     * @param isLocal
     * @see #isLocal()
     */
    void setLocal(boolean isLocal);

    /**
     * Tests if this event is public.
     * <p>
     * Public events are of interest to everyone.
     *
     * @return true if public false otherwise
     */
    boolean isPublic();

    /**
     * Set the public flag.
     *
     * @param isPublic
     * @see #isPublic()
     */
    void setPublic(boolean isPublic);

}
