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
    public boolean isLocal() {
        return (flags & FLAG_LOCAL) != 0;
    }

    @Override
    public void setLocal(boolean isLocal) {
        if (isLocal) {
            flags |= FLAG_LOCAL;
        } else {
            flags &= ~FLAG_LOCAL;
        }
    }

    @Override
    public boolean isPublic() {
        return !isLocal();
    }

    @Override
    public void setPublic(boolean isPublic) {
        setLocal(!isPublic);
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

}
