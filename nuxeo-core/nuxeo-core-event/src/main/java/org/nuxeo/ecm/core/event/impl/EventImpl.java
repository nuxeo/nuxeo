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
package org.nuxeo.ecm.core.event.impl;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;

/**
 * Event implementation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventImpl implements Event {

    private static final long serialVersionUID = 1L;

    protected final String name;

    protected final long time;

    protected final EventContext ctx;

    protected int flags;

    protected Exception rollbackException;

    protected String rollbackMessage;

    public EventImpl(String name, EventContext ctx, int flags, long creationTime) {
        this.name = name;
        this.ctx = ctx;
        time = creationTime;
        this.flags = flags;
    }

    public EventImpl(String name, EventContext ctx, int flags) {
        this(name, ctx, flags, System.currentTimeMillis());
    }

    public EventImpl(String name, EventContext ctx) {
        this(name, ctx, FLAG_NONE);
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public EventContext getContext() {
        return ctx;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void cancel() {
        flags |= FLAG_CANCEL;
    }

    @Override
    public void markRollBack() {
        flags |= FLAG_ROLLBACK;
    }

    @Override
    public void markRollBack(String message, Exception exception) {
        markRollBack();
        if (message == null && exception != null) {
            message = exception.getMessage();
        }
        rollbackMessage = message;
        rollbackException = exception;
    }

    @Override
    public void markBubbleException() {
        flags |= FLAG_BUBBLE_EXCEPTION;
    }

    @Override
    public boolean isBubbleException() {
        return (flags & FLAG_BUBBLE_EXCEPTION) != 0;
    }

    @Override
    public boolean isMarkedForRollBack() {
        return (flags & FLAG_ROLLBACK) != 0;
    }

    @Override
    public boolean isCanceled() {
        return (flags & FLAG_CANCEL) != 0;
    }

    @Override
    public boolean isInline() {
        return (flags & FLAG_INLINE) != 0;
    }

    @Override
    public void setInline(boolean isInline) {
        if (isInline) {
            flags |= FLAG_INLINE;
        } else {
            flags &= ~FLAG_INLINE;
        }
    }

    @Override
    public boolean isCommitEvent() {
        return (flags & FLAG_COMMIT) != 0;
    }

    @Override
    public void setIsCommitEvent(boolean isCommit) {
        if (isCommit) {
            flags |= FLAG_COMMIT;
        } else {
            flags &= ~FLAG_COMMIT;
        }
    }

    @Override
    public boolean isImmediate() {
        return (flags & FLAG_IMMEDIATE) != 0;
    }

    @Override
    public void setImmediate(boolean immediate) {
        if (immediate) {
            flags |= FLAG_IMMEDIATE;
        } else {
            flags &= ~FLAG_IMMEDIATE;
        }
    }

    @Override
    public Exception getRollbackException() {
        return rollbackException;
    }

    @Override
    public String getRollbackMessage() {
        return rollbackMessage;
    }

}
