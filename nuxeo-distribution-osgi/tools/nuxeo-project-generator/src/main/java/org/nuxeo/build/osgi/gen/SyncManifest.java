/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.osgi.gen;

import java.io.File;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SyncManifest {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: SyncManifest nuxeoRoot pom osgiRoot");
            return;
        }
        File nuxeoRoot = new File(args[0]).getCanonicalFile();
        File pom = new File(args[1]).getCanonicalFile();
        File osgiRoot = new File(args[2]).getCanonicalFile();

        PomLoader loader = new PomLoader(pom);
        for (File file : loader.getModuleFiles()) {
            ProjectGenerator gen = new ProjectGenerator(nuxeoRoot, pom, osgiRoot, file);
            File mf = gen.getManifest();
            File mf2sync = gen.getSourceManifest();
            System.out.println(mf + " -> " + mf2sync);
            FileUtils.copy(mf, mf2sync);
        }

    }

}
