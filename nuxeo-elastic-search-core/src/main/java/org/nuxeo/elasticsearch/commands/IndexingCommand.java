/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.commands;


/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class IndexingCommand {

    public static final String INDEX = "ESIndex";

    public static final String UPDATE = "ESReIndex";

    public static final String UPDATE_SECURITY = "ESReIndexSecurity";

    public static final String DELETE = "ESUnIndex";

    protected String name;

    protected boolean sync;

    protected boolean recurse;

    public IndexingCommand(String command, boolean sync, boolean recurse) {
        this.name=command;
        this.sync=sync;
        this.recurse=recurse;
    }

    public void update(IndexingCommand other) {
        update(other.sync, other.recurse);
    }
    public void update(boolean sync, boolean recurse) {
        this.sync = this.sync || sync;
        this.recurse = this.recurse || recurse;
    }

    public boolean isSync() {
        return sync;
    }

    public boolean isRecurse() {
        return recurse;
    }

    public String getName() {
        return name;
    }

}
