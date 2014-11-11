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
import java.util.List;

import org.apache.tools.ant.taskdefs.email.EmailAddress;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.EmbeddedMavenClient;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        MavenClientFactory.setInstance(new EmbeddedMavenClient());
        AntClient ant = new AntClient();

        List<String> targets = new ArrayList<String>();
        String profiles = null;
        File buildFile = null;
        for (String arg : args) {
            if (arg.startsWith("-f")) {
                buildFile = new File(arg.substring(2));
            } else if (arg.startsWith("-p")) {
                profiles = arg.substring(2);
            } else {
                targets.add(arg);
            }
        }
        if (profiles != null) {
            MavenClientFactory.getInstance().getAntProfileManager().activateProfiles(profiles);
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
