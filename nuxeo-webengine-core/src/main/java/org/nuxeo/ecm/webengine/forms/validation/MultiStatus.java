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

package org.nuxeo.ecm.webengine.forms.validation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MultiStatus implements Status, Iterable<Status>{

    protected List<Status> children;
    protected String message;
    protected boolean isOk = true;

    public MultiStatus() {
        this (null);
    }

    public MultiStatus(String message) {
        this.message = message;
        children = new ArrayList<Status>();
    }

    public void add(Status status) {
        children.add(status);
        if (!status.isOk()) {
            isOk = false;
        }
    }

    public int size() {
        return children.size();
    }

    public Iterator<Status> iterator() {
        return children.iterator();
    }

    public Status[] getChildren() {
        return children.toArray(new Status[children.size()]);
    }

    public String getField() {
        return null;
    }

    public String getMessage() {
        return message;
    }

    public boolean isMultiStatus() {
        return true;
    }

    public boolean isOk() {
        return isOk;
    }

    @Override
    public String toString() {
        return isOk ? "OK" : "Error: "+message;
    }
}
