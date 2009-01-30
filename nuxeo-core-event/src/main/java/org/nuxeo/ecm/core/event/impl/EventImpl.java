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

import java.util.EnumSet;
import java.util.Set;

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

    protected final Set<Flag> flags;


    public EventImpl(String name, EventContext ctx, Set<Flag> flags, long creationTime) {
        this.name = name;
        this.ctx = ctx;
        time = creationTime;
        if (flags == null) {
            flags = EnumSet.noneOf(Flag.class);
        }
        this.flags = flags;
    }

    public EventImpl(String name, EventContext ctx, Set<Flag> flags) {
        this(name, ctx, flags, System.currentTimeMillis());
    }

    public EventImpl(String name, EventContext ctx) {
        this(name, ctx, null);
    }


    public Set<Flag> getFlags() {
        return flags;
    }

    public EventContext getContext() {
        return ctx;
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }

    public void cancel() {
        flags.add(Flag.CANCEL);
    }

    public void markRollBack() {
        flags.add(Flag.ROOLBACK);
    }

    public boolean isMarkedForRollBack() {
        return flags.contains(Flag.ROOLBACK);
    }

    public boolean isCanceled() {
        return flags.contains(Flag.CANCEL);
    }

    public boolean isInline() {
        return flags.contains(Flag.INLINE);
    }

    public void setInline(boolean isInline) {
        if (isInline) {
            flags.add(Flag.INLINE);
        } else {
            flags.remove(Flag.INLINE);
        }
    }

    public boolean isCommitEvent() {
        return flags.contains(Flag.COMMIT);
    }

    public void setIsCommitEvent(boolean isCommit) {
        if (isCommit) {
            flags.add(Flag.COMMIT);
        } else {
            flags.remove(Flag.COMMIT);
        }
    }

    public boolean isLocal() {
        return flags.contains(Flag.LOCAL);
    }

    public void setLocal(boolean isLocal) {
        if (isLocal) {
            flags.add(Flag.LOCAL);
        } else {
            flags.remove(Flag.LOCAL);
        }
    }

    public boolean isPublic() {
        return !isLocal();
    }

    public void setPublic(boolean isPublic) {
        setLocal(!isPublic);
    }

}
