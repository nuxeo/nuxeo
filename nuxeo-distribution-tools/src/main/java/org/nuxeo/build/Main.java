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
import java.util.ArrayList;

import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.MavenClient;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    
    
    public static void main(String[] args) throws Exception {

        AntClient ant = new AntClient();
        
        ArrayList<String> targets = new ArrayList<String>();
        String profiles = null;
        File buildFile = null;
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-f")) {
                buildFile = new File(args[i].substring(2));
            } else if (args[i].startsWith("-p")) {
                profiles = args[i].substring(2);
            } else {
                targets.add(args[i]);
            }
        }
        if (profiles != null) {
            MavenClient.getInstance().getAntProfileManager().activateProfiles(profiles);
        }

        if (buildFile == null) {
            buildFile = new File("build.xml"); 
        }
        
        buildFile = buildFile.getCanonicalFile();
        
        if (targets.isEmpty()) {
            ant.run(buildFile);    
        } else {
            ant.run(buildFile, targets);
        }
        
    }
    
    
}
