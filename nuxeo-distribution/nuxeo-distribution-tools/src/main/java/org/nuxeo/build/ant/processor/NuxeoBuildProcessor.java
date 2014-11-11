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
package org.nuxeo.build.ant.processor;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoBuildProcessor extends Task {

    protected String src;
    protected String bundles = "bundles";
    protected String libdir = "lib";
    
    public void setSrc(String src) {
        this.src = src;
    }

    public void setBundlesDir(String bundles) {
        this.bundles = bundles;
    }

    public void setLibDir(String libdir) {
        this.libdir = libdir;
    }

    @Override
    public void execute() throws BuildException {
        if (src == null) {
            throw new BuildException("src attribute is not optional");
        }
        File wd = new File(src);
        try {
            ProcessorLoader loader = ProcessorLoader.newInstance(getClass().getClassLoader(), wd, bundles, libdir);
            loader.run(wd);
        } catch (Exception e) {
            throw new BuildException("Failed to run preprocessor", e);
        }
    }
    
    
    
}
