/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.backend.compass.connection;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
@XObject("file")
public class FileConnectionDescriptor implements ConnectionConf {

    private static final long serialVersionUID = 1L;

    @XNode("@path")
    protected String path;

    protected String absolutePath;

    private static final String URL_PROTOCOL = "file://";

    public String getConnectionString() {
        if (absolutePath == null) {
            // XXX this doesn't take care of windows C:\... paths but oh well
            if (path.charAt(0) == '/') {
                absolutePath = path;
            } else {
                absolutePath = Framework.getRuntime().getHome().getPath()
                        + path;
            }
        }
        return URL_PROTOCOL + absolutePath;
    }

}
