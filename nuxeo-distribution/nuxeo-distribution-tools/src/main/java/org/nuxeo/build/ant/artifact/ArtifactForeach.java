/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN
 */
package org.nuxeo.build.ant.artifact;

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Sequential;
import org.nuxeo.build.maven.graph.Node;

/**
 * Iterate through a artifact set and do action on it
 * 
 * It should be used like that : <artifact:foreach setref="bundles"
 * artifactJarPathProperty="path" > </artifact:foreach>
 * 
 * @author Sun Seng David TAN
 * 
 */
public class ArtifactForeach extends Sequential {

    public String target;

    public String property;

    public ArtifactSet artifactSet;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public ArtifactSet getArtifactSet() {
        return artifactSet;
    }

    public void setSetref(String setref) {
        artifactSet = (ArtifactSet) getProject().getReference(setref);

    }

    @Override
    public void execute() throws BuildException {
        for (Node node : artifactSet.getNodes()) {
            try {
                String canonialPath = null;

                try {
                    canonialPath = node.getFile().getCanonicalPath();
                } catch (IOException e) {
                    log("An error occured while getting artifact file canonial path", e, 1);
                }
                getProject().setProperty(property + ".file.path", canonialPath);
                getProject().setProperty(property + ".archetypeId", node.getArtifact().getArtifactId());
                getProject().setProperty(property + ".groupId", node.getArtifact().getGroupId());
                getProject().setProperty(property + ".version", node.getArtifact().getBaseVersion());
                super.execute();
            } catch (Throwable e) {
                log("Coulnd't execute the task for artifact " + node.getId(), e, 2);
                e.printStackTrace();
            }
        }

    }
}
