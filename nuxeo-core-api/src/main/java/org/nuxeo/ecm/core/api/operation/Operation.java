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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Operation<T> implements Serializable {

    private static final long serialVersionUID = -9191284967153046156L;

    // the current command
    private static final ThreadLocal<Operation<?>> operation = new ThreadLocal<Operation<?>>();

    public static final String START_EVENT = "commandStarted";
    public static final String TERMINATE_EVENT = "commandTerminated";

    /**
     * Operation flags.
     * The first 2 bits are reserved to operation execution state
     * The first 8 bits are reserved (bits 0-7)
     */

    // No flags are set - operation was not yet started
    public static final int NONE = 0;

    // set if operation is running (bit 0)
    public static final int RUNNING = 1;
    // set if operation completed (bit 1)
    public static final int TERMINATED = 2;

    // an internal operation - not triggered directly by clients
    public static final int INTERNAL = 4;
    // priority flag - if set this operation is urgent (should not be queued, neither it's completion notification)
    public static final int URGENT = 8;
    // this operation may run asynchronously - this is a hint
    public static final int ASYNC = 16;
    // operation completion notification should not be sent over JMS
    public static final int BLOCK_JMS = 32;
    // Whether or not the data field contains keyed data.
    public static final int KEYED_DATA = 64;
    // block children events
    public static final int BLOCK_CHILD_NOTIFICATIONS = 128;
    // reserved by the core for future use
    public static final int RESERVED = 256;

    /**
     * User flags may be used by clients to set custom flags on the operation.
     * These flags must use only the range of bits from 8 to 31 (the first byte is reserved for core use)
     */
    // mask for user flags
    public static final int USER_FLAGS = 0XFFFE00;

    /**
     * A convenience method to compute the correct user flag from the 0 based representation of that bit
     * @param n
     * @return
     */
    public static int USER_FLAG(int n) { return n << 16; }

    protected final String name;
    protected int flags;
    protected Object data;
    protected Status status = Status.STATUS_OK;
    protected T result;
    private ModificationSet modifs;
    protected transient Operation<?> parent;
    protected transient CoreSession session;


    protected Operation(String name, int flags) {
        this.name = name;
        this.flags = flags;
    }

    protected Operation(String name) {
        this(name, NONE);
    }

    public static Operation<?> getCurrent() {
        return operation.get();
    }

    public static Operation<?>[] getStack() {
        Operation<?> cmd = operation.get();
        if (cmd == null) {
            return new Operation<?>[0];
        } else if (cmd.parent == null) {
            return new Operation<?>[] {cmd};
        } else {
            List<Operation<?>> cmds = new ArrayList<Operation<?>>();
            cmd.fillCommandStack(cmds);
            return cmds.toArray(new Operation<?>[cmds.size()]);
        }
    }

    public static Operation<?>[] printStack(PrintStream out) {
        Operation<?> cmd = operation.get();
        if (cmd == null) {
            return new Operation<?>[0];
        } else if (cmd.parent == null) {
            return new Operation<?>[] {cmd};
        } else {
            List<Operation<?>> cmds = new ArrayList<Operation<?>>();
            cmd.fillCommandStack(cmds);
            return cmds.toArray(new Operation<?>[cmds.size()]);
        }
    }

    public final Status getStatus() {
        return status;
    }

    public final int getFlags() {
        return flags;
    }

    public final void setFlags(int flags) {
        this.flags |= flags;
    }

    public final void clearFlags(int flags) {
        this.flags &= ~flags;
    }

    public final boolean isRunning() {
        return (flags & RUNNING) == RUNNING;
    }

    public final boolean isTerminated() {
        return (flags & TERMINATED) == TERMINATED;
    }

    public final boolean isFlagSet(int flag) {
        return (flags & flag) == flag;
    }

    public final T getResult() {
        return result;
    }

    public final String getName() {
        return name;
    }

    /**
     * TODO impl this?
     * @param args
     */
//    public void setArguments(Object ... args) {
//
//    }
//
//    /**
//     * TODO impl this?
//     * @return command arguments.
//     */
//    public Object[] getArguments() {
//        return null;
//    }

    /**
     * @return the parent.
     */
    public final Operation<?> getParent() {
        return parent;
    }

    public final CoreSession getSession() {
        return session;
    }

    public T run(CoreSession session, OperationHandler handler,
            ProgressMonitor monitor) {
        if (isRunning()) {
            throw new IllegalStateException("Command was already executed");
        }
        this.session = session;
        result = null;
        start();
        boolean isNotificationEnabled = isNotificationEnabled();
        if (handler != null && isNotificationEnabled) {
            handler.startOperation(this);
        }
        if (monitor != null) {
            monitor.started(this);
        }
        try {
            result = doRun(monitor);
        } catch (Throwable t) {
            status = new Status(Status.ERROR, t);
        } finally {
            if (handler != null && isNotificationEnabled) {
                handler.endOperation(this);
            }
            end();
            if (monitor != null) {
                monitor.terminated(this);
            }
        }
        return result;
    }

    private boolean isNotificationEnabled() {
        while (parent != null) {
            if (parent.isFlagSet(BLOCK_CHILD_NOTIFICATIONS)) {
                return false; // notifications were blocked by a parent
            }
            parent = parent.parent;
        }
        // notifications are enabled
        return true;
    }

    private void start() {
        setFlags(RUNNING);
        parent = operation.get();
        operation.set(this);
    }

    private void end() {
        operation.set(parent);
        setFlags(TERMINATED);
    }

    public List<Operation<?>> getCommandStack() {
        List<Operation<?>> cmds = new ArrayList<Operation<?>>();
        fillCommandStack(cmds);
        return cmds;
    }

    public void fillCommandStack(List<Operation<?>> cmds) {
        if (parent != null) {
            fillCommandStack(cmds);
        }
      cmds.add(this);
    }

    public void printCommandStack(PrintStream out) {
        if (parent != null) {
            parent.printCommandStack(out);
        }
        out.println(toString());
    }

    @Override
    public String toString() {
        return name;
    }

    public void addModification(Modification modif) {
        if (modifs == null) {
            modifs = new ModificationSet();
            initModificationSet(modifs);
        }
        modifs.add(modif);
    }

    public void addModification(DocumentRef ref, int modifType) {
        if (modifs == null) {
            modifs = new ModificationSet();
            initModificationSet(modifs);
        }
        modifs.add(ref, modifType);
    }

    public ModificationSet getModifications() {
        if (modifs == null) {
            modifs = new ModificationSet();
            initModificationSet(modifs);
        }
        return modifs;
    }

    protected void initModificationSet(ModificationSet modifs) {
        // do nothing by default
    }

    public abstract T doRun(ProgressMonitor montior) throws Exception;


    // application data support

    public Object getData () {
        return (flags & KEYED_DATA) != 0 ? ((Object []) data) [0] : data;
    }

    public Object getData (String key) {
        if (key == null) {
            throw new IllegalArgumentException("Data Key must not be null");
        }
        if ((flags & KEYED_DATA) != 0) {
            Object [] table = (Object []) data;
            for (int i=1; i<table.length; i+=2) {
                if (key.equals(table[i])) {
                    return table[i + 1];
                }
            }
        }
        return null;
    }

    public void setData(Object value) {
        if ((flags & KEYED_DATA) != 0) {
            ((Object []) data) [0] = value;
        } else {
            data = value;
        }
    }

    public void setData(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Data Key must not be null");
        }
        int index = 1;
        Object [] table = null;
        if ((flags & KEYED_DATA) != 0) {
            table = (Object []) data;
            while (index < table.length) {
                if (key.equals(table[index])) {
                    break;
                }
                index += 2;
            }
        }
        if (value != null) {
            if ((flags & KEYED_DATA) != 0) {
                if (index == table.length) {
                    Object [] newTable = new Object [table.length + 2];
                    System.arraycopy (table, 0, newTable, 0, table.length);
                    data = table = newTable;
                }
            } else {
                table = new Object [3];
                table [0] = data;
                data = table;
                flags |= KEYED_DATA;
            }
            table [index] = key;
            table [index + 1] = value;
        } else {
            if ((flags & KEYED_DATA) != 0) {
                if (index != table.length) {
                    int length = table.length - 2;
                    if (length == 1) {
                        data = table [0];
                        flags &= ~KEYED_DATA;
                    } else {
                        Object [] newTable = new Object [length];
                        System.arraycopy (table, 0, newTable, 0, index);
                        System.arraycopy (table, index + 2, newTable, index, length - index);
                        data = newTable;
                    }
                }
            }
        }
    }

}
