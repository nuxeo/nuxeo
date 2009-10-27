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
package org.nuxeo.build.ant.artifact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tools.ant.Project;
import org.nuxeo.build.ant.profile.AntProfileManager;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactSetParser {

    protected Project project;
    protected AntProfileManager profileMgr;

    public ArtifactSetParser(Project project) {
        this.project = project;
        this.profileMgr = MavenClientFactory.getInstance().getAntProfileManager();
    }

    public void parse(File src, Collection<Node> nodes) throws IOException {
        parse(new BufferedReader(new FileReader(src)), nodes);
    }

    public void parse(BufferedReader reader, Collection<Node> nodes) throws IOException {
        String line = reader.readLine();
        while (line != null) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }
            line = project.replaceProperties(line);
            if (line.startsWith("?")) { // a profile
                String profile = line.substring(1).trim();
                if (profile.length() == 0) { // default profile
                    line = reader.readLine();
                    continue;
                } else if (profileMgr.isProfileActive(profile)) {
                    line = reader.readLine();
                    continue;
                } else { // skip this profile content
                    readToNextActiveProfile(reader);
                }
            } else if (line.startsWith("@")) { // include another file
                File file = project.resolveFile(line.substring(1).trim());
                parse(file, nodes);
            } else if (line.startsWith("!")) { // remove from already collected nodes
                //TODO
            } else {
                int p = line.lastIndexOf('?');
                if (p > -1) {
                    List<String> profiles = split(line.substring(p+1), ',');
                    if (!profileMgr.isAnyProfileActive(profiles)) {
                        line = reader.readLine();
                        continue;
                    }
                    line = line.substring(0, p);
                }
                ArtifactFile af = new ArtifactFile();
                af.setKey(line);
                nodes.add(af.getNode());
            }
            line = reader.readLine();
        }
    }

    protected String readToNextActiveProfile(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        while (line != null) {
            line = line.trim();
            if (line.startsWith("?")) {
                String profile = line.substring(1).trim();
                if (!profileMgr.isProfileActive(profile)) {
                    return readToNextActiveProfile(reader);
                } else {
                    return profile;
                }
            }
            line = reader.readLine();
        }
        return null;
    }


    protected List<String> split(String text, char ch) {
        ArrayList<String> result = new ArrayList<String>();
        int p = 0;
        int q = text.indexOf(ch, p);
        while (q > -1) {
            result.add(text.substring(p, q).trim());
            p = q+1;
            q = text.indexOf(ch, p);
        }
        if (p == 0) {
            result.add(text.trim());
        } else {
            result.add(text.substring(p).trim());
        }
        return result;
    }

}
