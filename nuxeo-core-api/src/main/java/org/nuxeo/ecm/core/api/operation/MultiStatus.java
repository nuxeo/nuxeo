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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MultiStatus extends Status {

    private static final long serialVersionUID = 4645429673525324229L;

    protected Status[] children;

    public MultiStatus(int severity) {
        super(severity);
    }

    public MultiStatus(int severity, String message) {
        super(severity, message);
    }

    public MultiStatus(int severity, Throwable exception) {
        super(severity, exception);
    }

    @Override
    public boolean isMultiStatus() {
        return true;
    }

    public void addStatus(Status status) {
        //TODO
    }

    public Status[] getChildren() {
        return children;
    }

}
