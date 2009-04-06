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
package org.nuxeo.ecm.core.client.sample;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;

import org.nuxeo.ecm.core.client.NuxeoApp;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage app classToRun");
            System.exit(1);
        }
        Collection<File> files = null;
        String bundles = System.getProperty("nuxeo.bundles");
        if (bundles != null) {
            files = NuxeoApp.getBundleFiles(new File("."), bundles, ":");
        }
        NuxeoApp app = new NuxeoApp();
        app.start();
        System.out.println("Deploying bundles: "+files);
        if (files != null) {
            app.deployBundles(files);
        }
        if (args.length > 0) {
            Class<?> klass = Class.forName(args[0]);
            Method main = klass.getMethod("main", new Class<?>[] {String[].class});
            String[] tmp = new String[args.length - 1];
            if (tmp.length > 0) {
                System.arraycopy(args, 1, tmp, 0, tmp.length);
            }
            main.invoke(null, new Object[]{tmp});
        }
        app.shutdown();
    }

}
