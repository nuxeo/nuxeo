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
 *
 */
public class EventImpl implements Event {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected int flags;
    protected long time;
    protected EventContext ctx;


    public EventImpl(String name, EventContext ctx) {
        this(name, ctx, 0);
    }

    public EventImpl(String name, EventContext ctx, int flags) {
        this.name = name;
        this.ctx = ctx;
        this.time = System.currentTimeMillis();
        this.flags = flags;
    }

    public void setFlags(int flags) {
        this.flags |= flags;
    }

    public int getFlags() {
        return flags;
    }

    public boolean isFlagSet(int flags) {
        return (this.flags & flags) == flags;
    }

    public void clearFlags(int flags) {
        this.flags &= ~flags;
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
        setFlags(CANCEL);
    }

    public boolean isCanceled() {
        return isFlagSet(CANCEL);
    }

    public boolean isInline() {
        return isFlagSet(INLINE);
    }

    public void setInline(boolean inline) {
        if (inline) {
            setFlags(INLINE);
        } else {
            clearFlags(INLINE);
        }
    }

    public boolean isCommitEvent() {
        return isFlagSet(COMMIT);
    }

    public void setIsCommitEvent(boolean isCommit) {
        if (isCommit) {
            setFlags(COMMIT);
        } else {
            clearFlags(COMMIT);
        }
    }

    public boolean isLocal() {
        return isFlagSet(LOCAL);
    }

    public void setLocal(boolean isLocal) {
        if (isLocal) {
            setFlags(LOCAL);
        } else {
            clearFlags(LOCAL);
        }
    }

    public boolean isPublic() {
        return !isLocal();
    }

    public void setPublic(boolean isPublic) {
        setLocal(!isPublic);
    }

}
