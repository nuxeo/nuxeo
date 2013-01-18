/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.template;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.template.importer.TemplateBundleActivator;

public class Activator extends TemplateBundleActivator {

    public static InputStream getResourceAsStream(String path)
            throws IOException {
        URL url = getResource(path);
        return url != null ? url.openStream() : null;
    }
}
