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

package org.nuxeo.ecm.webengine.session;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractComponent implements Component {

    protected String name;
    private boolean isLive;

    public String getName() {
        return name;
    }

    public boolean isLive() {
        return isLive;
    }

    public void initialize(UserSession session, String name)
            throws SessionException {
        if (this.isLive) {
            throw new InvalidStateException(this, "initialize");
        }
        this.name = name;
        doInitialize(session, name);
        this.isLive = true;
    }

    public void destroy(UserSession session) throws SessionException {
        if (!isLive) {
            throw new InvalidStateException(this, "destroy");
        }
        doDestroy(session);
        this.name = null;
        this.isLive = false;
    }


    public void doInitialize(UserSession session, String name) throws SessionException {
        // do nothing by default
    }

    public void doDestroy(UserSession session) throws SessionException {
        // do nothing by default
    }

}
