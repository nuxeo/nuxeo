/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event;

import java.io.Serializable;

/**
 * A lightweight object used by core components to notify interested components about events in core.
 * <p>
 * These events should be used by all core components not only by the repository.
 * <p>
 * There are 2 post commit control flags:
 * <ul>
 * <li>INLINE - if true the event will not be recorded as part of the post commit event bundle. Defaults to false.
 * <li>COMMIT - the event will simulate a commit so that the post commit event bundle will be fired. TYhe COMMIT flag is
 * ignored while in a transaction.
 * </ul>
 * More flags may be added in the future.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Event extends Serializable {

    // we don't use an EnumSet, as they use far too much memory
    int FLAG_NONE = 0;

    int FLAG_CANCEL = 1;

    int FLAG_ROLLBACK = 2;

    int FLAG_COMMIT = 4;

    int FLAG_INLINE = 16;

    int FLAG_IMMEDIATE = 32;

    int FLAG_BUBBLE_EXCEPTION = 64;

    /**
     * Gets the event name.
     * <p>
     * The name must be unique. It is recommended to use prefixes in the style of java package names to differentiate
     * between similar events that are sent by different components.
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
     * Event contexts give access to the context in which the the event was raised. Event contexts are usually
     * identifying the operation that raised the event. The context is exposing data objects linked to the event like
     * documents and also may give access to the operation that raised the event allowing thus to canceling the
     * operation, to record time spent to set the result status etc.
     *
     * @return the event context
     */
    EventContext getContext();

    /**
     * Gets the set of event flags
     *
     * @return the event flags
     */
    int getFlags();

    /**
     * Cancels this event.
     * <p>
     * This can be used by event listeners to exit the event notification. Remaining event listeners will no more be
     * notified. Note that this is not canceling the underlying operation if any.
     */
    void cancel();

    /**
     * Checks whether the event was canceled.
     *
     * @return true if canceled, false otherwise.
     */
    boolean isCanceled();

    /**
     * Marks the event to bubble the Exception thrown by a listener.
     * <p>
     * This will exit the event listeners loop. The transaction won't be rollbacked, but the Exception will be thrown by
     * the {@link EventService}.
     *
     * @since 5.7
     */
    void markBubbleException();

    /**
     * Returns {@code true} if the event was marked to bubble the Exception, {@code false} otherwise.
     *
     * @since 5.7
     */
    boolean isBubbleException();

    /**
     * Marks transaction for RollBack
     * <p>
     * This will exit the event listeners loop and throw a RuntimeException In JTA container, this will make the global
     * transaction rollback.
     */
    void markRollBack();

    /**
     * Marks transaction for RollBack
     * <p>
     * This will exit the event listeners loop and throw a RuntimeException In JTA container, this will make the global
     * transaction rollback.
     *
     * @param message message that explains the reason of the Rollback
     * @param exception associated Exception that explains the Rollback if any
     * @since 5.6
     */
    void markRollBack(String message, Exception exception);

    /**
     * Checks whether the event was marked for RollBack
     *
     * @return true if rolled back, false otherwise.
     */
    boolean isMarkedForRollBack();

    /**
     * Returns the Exception associated the RollBack if any
     *
     * @return the Exception associated the RollBack if any
     * @since 5.6
     */
    Exception getRollbackException();

    /**
     * Returns the message associated to the RollBack if any
     *
     * @return the message associated to the RollBack if any
     * @since 5.6
     */
    String getRollbackMessage();

    /**
     * Whether this event must not be added to a bundle. An event is not inline by default.
     *
     * @return true if the event must be omitted from event bundles, false otherwise.
     */
    boolean isInline();

    /**
     * Set the inline flag.
     *
     * @param isInline true if the event must not be recorded as part of the transaction
     * @see #isInline()
     */
    void setInline(boolean isInline);

    /**
     * Tests whether or not this is a commit event. A commit event is triggering the post commit notification and then
     * is reseting the recorded events.
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
     * @deprecated since 10.3
     */
    @Deprecated
    default boolean isLocal() {
        return false;
    }

    /**
     * @deprecated since 10.3
     */
    @Deprecated
    default void setLocal(boolean isLocal) {
        // deprecated
    }

    /**
     * @deprecated since 10.3
     */
    @Deprecated
    default boolean isPublic() {
        return true;
    }

    /**
     * @deprecated since 10.3
     */
    @Deprecated
    default void setPublic(boolean isPublic) {
        // deprecated
    }

    /**
     * Tests if event is Immediate
     * <p>
     * Immediate events are sent in bundle without waiting for a commit
     *
     * @return true if event is immediate, false otherwise
     */
    boolean isImmediate();

    /**
     * Sets the immediate flag.
     */
    void setImmediate(boolean immediate);

}
