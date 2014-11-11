/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileResourceResolver implements ResourceResolver {

    protected File file;

    public FileResourceResolver(String path) {
        file = new File(Framework.expandVars(path));
    }

    public FileResourceResolver(File file) {
        this.file = file;
    }

    @Override
    public URL getResource(String name) {
        File f = new File(file, name);
        if (f.isFile()) {
            try {
                return f.toURI().toURL();
            } catch(IOException e) {
                return null;
            }
        }
        return null;
    }

}
