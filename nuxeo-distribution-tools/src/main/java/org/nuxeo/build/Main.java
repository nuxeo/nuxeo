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
package org.nuxeo.build;

import java.io.File;

import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.MavenClient;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    
    
    public static void main(String[] args) throws Exception {

        AntClient ant = new AntClient();
        
        String profiles = null;
        File buildFile = null;
        if (args.length == 0) {
           buildFile = new File("build.xml"); 
        } else if (args[0].equals("-f")) {
            if (args.length == 1) {
                System.err.println("Syntac Error. Usage: ...");
                System.exit(1);
            }
            buildFile = new File(args[1]);
            if (args.length > 2) {
                profiles = args[2];
            }
        } else {
            profiles = args[0];
        }
        if (profiles != null) {
            MavenClient.getInstance().getAntProfileManager().activateProfiles(profiles);
        }

        if (buildFile == null) {
            buildFile = new File("build.xml"); 
        }
        
        buildFile = buildFile.getCanonicalFile();
        
        ant.run(buildFile);
        
    }
    
    
}
