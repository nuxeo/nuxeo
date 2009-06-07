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

package org.nuxeo.common.server;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("webapp")
public class WebApplication {

    @XNode("root")
    protected String root;

    @XNode("webXml")
    protected String webXml;

    @XNode("@name")
    protected String name;

    @XNode("@path")
    protected String path;

    @XNode("@warPreprocessing")
    protected boolean warPreprocessing=false;

    public String getWebRoot() {
        return root;
    }

    public String getConfigurationFile() {
        return webXml;
    }

    public String getName() {
        return name;
    }

    public String getContextPath() {
        return path;
    }

    public boolean needsWarPreprocessing() {
        return warPreprocessing;
    }

}
