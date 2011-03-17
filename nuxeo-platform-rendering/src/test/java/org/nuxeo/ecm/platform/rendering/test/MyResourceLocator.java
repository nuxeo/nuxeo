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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.test;

import java.io.File;
import java.net.URL;

import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MyResourceLocator implements ResourceLocator {

    public URL getResourceURL(String key) {
        return TestFreemarkerRendering.class.getClassLoader().getResource(key);
    }

    public File getResourceFile(String key) {
        return null;
    }

}
