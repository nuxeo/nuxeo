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
 */
package org.nuxeo.chemistry.shell;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractContext implements Context {

    protected final Application app;
    protected final Path path;
    
    public AbstractContext(Application app, Path path) {
        this.app = app;
        this.path = path;
    }
    
    public String pwd() {
        return path.toString();
    }
    
    public Path getPath() {
        return path;
    }
    
    public Application getApplication() {
        return app;
    }

    public Path resolvePath(String path) {
        if (!path.startsWith("/")) {
            return new Path(path);
        } else {
            return this.path.append(path);
        }        
    }

}
