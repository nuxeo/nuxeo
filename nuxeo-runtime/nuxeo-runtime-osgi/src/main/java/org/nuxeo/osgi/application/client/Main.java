/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.osgi.application.client;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;

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
        app.init();
        System.out.println("Deploying bundles: "+files);
        if (files != null) {
            app.deployBundles(files);
        }
        app.start();
        if (args.length > 0) {
            Class<?> klass = Class.forName(args[0]);
            Method main = klass.getMethod("main", String[].class);
            String[] tmp = new String[args.length - 1];
            if (tmp.length > 0) {
                System.arraycopy(args, 1, tmp, 0, tmp.length);
            }
            main.invoke(null, new Object[]{tmp});
        }
        app.shutdown();
    }

}
