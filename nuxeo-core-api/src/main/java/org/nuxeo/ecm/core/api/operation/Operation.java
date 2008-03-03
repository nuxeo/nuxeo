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
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Operation<T> implements Serializable {

    private static ThreadLocal<Operation<?>> command = new ThreadLocal<Operation<?>>(); // the current command

    public final static String START_EVENT = "commandStarted";
    public final static String TERMINATE_EVENT = "commandTerminated";

    public final static int NONE = 0;
    public final static int RUNNING = 1;
    public final static int TERMINATED = 2;

    // internal flags from 1 to 256 (8 flags)
    public final static int PRIVATE       = 1; // an internal command - cannot be created by users
    public final static int URGENT       = 2; // this command needs urgent feedback - post notification should not be queued - they should be done as quick as possible
    public final static int READ_ONLY = 4; // this command make modifications on storage
    public final static int UPDATE_STATE = 8; // this command make modifications on document state
    public final static int UPDATE_CONTENT = 16; // this command make modifications on storage content
    public final static int UPDATE_STRUCTURE = 32; // this command make modifications on storage structure
    public final static int BLOCK_JMS  = 64;  // if set do not send jms events for this command

    /**
     * Whether ot not the data field contains keyed data.
     */
    public static final int KEYED_DATA = 128;

    private transient ModificationSet modifs = null;
    private int state = NONE;
    private boolean isOk = true;
    protected int flags = NONE;
    // this key can be used to identify the identity if the command invoker. If null
    protected Object data;
    protected String name;
    protected Object[] args;
    protected transient Operation<?> parent;
    protected transient CoreSession session;


    public static Operation<?> getCurrent() {
        return command.get();
    }

    public static Operation<?>[] getStack() {
        Operation<?> cmd = command.get();
        if (cmd == null) {
            return new Operation<?>[0];
        } else if (cmd.parent == null) {
            return new Operation<?>[] {cmd};
        } else {
            ArrayList<Operation<?>> cmds = new ArrayList<Operation<?>>();
            cmd.fillCommandStack(cmds);
            return cmds.toArray(new Operation<?>[cmds.size()]);
        }
    }

    public static Operation<?>[] printStack(PrintStream out) {
        Operation<?> cmd = command.get();
        if (cmd == null) {
            return new Operation<?>[0];
        } else if (cmd.parent == null) {
            return new Operation<?>[] {cmd};
        } else {
            ArrayList<Operation<?>> cmds = new ArrayList<Operation<?>>();
            cmd.fillCommandStack(cmds);
            return cmds.toArray(new Operation<?>[cmds.size()]);
        }
    }

    public Operation(String name) {
        this.name = name;
    }

    public Operation(String name, int flags) {
        this.name = name;
        this.flags = flags;
    }

    public Operation(String name, int flags, Object ... args) {
        this.name = name;
        this.args = args;
        this.flags = flags;
    }

    /**
     * @return the isOk.
     */
    public boolean isOk() {
        return isOk;
    }

    /**
     * @return the state.
     */
    public int getState() {
        return state;
    }

    /**
     * @return the flags.
     */
    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags |= flags;
    }

    public void clearFlags(int flags) {
        this.flags &= ~flags;
    }

    public boolean isRunning() {
        return state == RUNNING;
    }

    public boolean isTerminated() {
        return state == TERMINATED;
    }

    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return command arguments.
     */
    public Object[] getArguments() {
        return args;
    }

    /**
     * @return the parent.
     */
    public Operation<?> getParent() {
        return parent;
    }

    public CoreSession getSession() {
        return session;
    }

    public T run(CoreSession session, ProgressMonitor monitor, Object ... args) throws Exception {
        if (state != NONE) {
            throw new IllegalStateException("Command was already executed");
        }
        this.session = session;
        this.args = args;
        isOk = true;
        boolean terminatedOk = false;
        T result = null;
        start();
        if (monitor != null) monitor.started(this);
        try {
            result = doRun(monitor);
            terminatedOk = true;
        } finally {
            isOk = terminatedOk;
            end();
            if (monitor != null) monitor.terminated(this);
        }
        return result;
    }


    private final void start() {
        state = RUNNING;
        parent = command.get();
        command.set(this);
    }

    private final void end() {
        command.set(parent);
        state = TERMINATED;
    }

    public List<Operation<?>> getCommandStack() {
        ArrayList<Operation<?>> cmds = new ArrayList<Operation<?>>();
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
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" [ ").append(Arrays.toString(args)).append(" ]");
        return sb.toString();
    }

    public boolean isReadOnly() {
        return (flags & READ_ONLY) == READ_ONLY;
    }

    public boolean isModifyingStructure() {
        return (flags & UPDATE_STRUCTURE) == UPDATE_STRUCTURE;
    }

    public boolean isModifyingContent() {
        return (flags & UPDATE_CONTENT) == UPDATE_CONTENT;
    }

    public boolean isModifyingState() {
        return (flags & UPDATE_STATE) == UPDATE_STATE;
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

    public abstract T doRun(ProgressMonitor montior) throws Exception;
    protected abstract void initModificationSet(ModificationSet modifs);

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
