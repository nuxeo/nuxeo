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

import java.text.ParseException;

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.runtime.model.Adaptable;

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

    @XNode("permission")
    protected  GuardDescriptor guardDescriptor;

    private Guard guard;


    public WebApplicationMapping() {
    }

    public WebApplicationMapping(String path, String webapp, String docRoot) {
        this.webApp = webapp;
        setPathAsString(path);
        if (docRoot != null) {
            setDocRoot(docRoot);
        }
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Path getPath() {
        return path;
    }

    public String getWebApp() {
        return webApp;
    }

    public DocumentRef getDocRoot() {
        return docRoot;
    }

    @XNode("@document")
    public void setDocRoot(String docRoot) {
        this.rootPath = new Path(docRoot).makeAbsolute().removeTrailingSeparator();
        this.docRoot = new PathRef(rootPath.toString());
    }

    public Path getRootPath() {
        return rootPath;
    }

    public void setWebApp(String webApp) {
        this.webApp = webApp;
    }

    public void setPath(Path path) {
        this.path = path.makeAbsolute().removeTrailingSeparator();
    }

    @XNode("@path")
    public void setPathAsString(String pathAsString) {
        this.path = new Path(pathAsString).makeAbsolute().removeTrailingSeparator();
    }

    public GuardDescriptor getGuardDescriptor() {
        return guardDescriptor;
    }

    public void setGuardDescriptor(GuardDescriptor guard) {
        this.guardDescriptor = guard;
    }

    public void checkPermission(Adaptable adaptable) throws WebSecurityException {
        if (!getGuard().check(adaptable)) {
            throw new WebSecurityException("Access Restricted");
        }
    }

    public Guard getGuard() {
        if (guard == null) {
            try {
                guard = guardDescriptor != null? guardDescriptor.getGuard() : Guard.DEFAULT;
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }
        return guard;
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
