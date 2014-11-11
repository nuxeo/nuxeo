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
 *     <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

/**
 * Perform a ping operation. Can be used in remote client (NxRCP or NxShell) to
 * test connectivity.
 *
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 *
 */
public class PingOperation extends Operation {

    public PingOperation() {
        super("__PING__");
    }

    @Override
    public Object doRun(ProgressMonitor montior) throws Exception {
        // do nothing
        return null;
    }

}
