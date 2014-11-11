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

package org.nuxeo.ecm.webengine;

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("mapping")
public class WebApplicationMapping {

    protected Path path;
    protected Path rootPath;

    protected DocumentRef docRoot;

    @XNode("@webapp")
    protected String webApp;

    @XNode("@default")
    protected boolean isDefault;


    /**
     *
     */
    public WebApplicationMapping() {
        // TODO Auto-generated constructor stub
    }

    public WebApplicationMapping(String path, String webapp, String docRoot) {
        this.webApp = webapp;
        setPathAsString(path);
        if (docRoot != null) {
            setDocRoot(docRoot);
        }
    }

    /**
     * @param isDefault the isDefault to set.
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * @return the isDefault.
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * @return the path.
     */
    public Path getPath() {
        return path;
    }


    /**
     * @return the webApp.
     */
    public String getWebApp() {
        return webApp;
    }

    /**
     * @return the docRoot.
     */
    public DocumentRef getDocRoot() {
        return docRoot;
    }


    @XNode("@document")
    public void setDocRoot(String docRoot) {
        this.rootPath = new Path(docRoot).makeAbsolute().removeTrailingSeparator();
        this.docRoot = new PathRef(rootPath.toString());
    }

    /**
     * @return the rootPath.
     */
    public Path getRootPath() {
        return rootPath;
    }

    /**
     * @param webApp the webApp to set.
     */
    public void setWebApp(String webApp) {
        this.webApp = webApp;
    }

    /**
     * @param path the path to set.
     */
    public void setPath(Path path) {
        this.path = path.makeAbsolute().removeTrailingSeparator();
    }

    /**
     * @param pathAsString the pathAsString to set.
     */
    @XNode("@path")
    public void setPathAsString(String pathAsString) {
        this.path = new Path(pathAsString).makeAbsolute().removeTrailingSeparator();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return false;
        }
        if (obj  == null) {
            return false;
        }
        if (obj instanceof WebApplicationMapping) {
            WebApplicationMapping wmap = (WebApplicationMapping)obj;
            if (webApp.equals(wmap.webApp) && wmap.path.equals(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (docRoot != null) {
            return webApp+":"+path.toString()+":"+(docRoot.toString());
        } else {
            return webApp+":"+path.toString()+":";
        }
    }

}
