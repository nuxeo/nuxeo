/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.build.ant.artifact;

import java.io.File;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.graph.Node;

/**
 * Attaches the artifact to Maven.
 *
 * @author Kohsuke Kawaguchi
 */
public class AttachArtifactTask extends Task {

    private File file;

    private String classifier;

    private String type;

    private String target;

    /**
     * The file to be treated as an artifact.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Optional classifier. If left unset, the task will
     * attach the main artifact.
     */
    public void setClassifier(String classifier) {
        if (classifier != null && classifier.length() == 0)
            classifier = null;
        this.classifier = classifier;
    }

    public void setTarget(String artifactKey) {
        this.target = artifactKey;
    }

    /**
     * Artifact type. Think of it as a file extension.
     * Optional, and if omitted, infered from the file extension.
     */
    public void setType(String type) {
        this.type = type;
    }

    public void execute() throws BuildException {
        final MavenClient maven = MavenClientFactory.getInstance();

        if (target == null) {
            throw new BuildException("Target artifact not set");
        }
        final Node node = maven.getGraph().findFirst(target, true);
        if (node == null) {
            throw new BuildException("No such artofact found: "+target);
        }

        if(classifier==null) {
            if(type!=null) {
                throw new BuildException("type is set but classifier is not set");
            }
            log("Attaching main file "+file+" to artifact "+target, Project.MSG_INFO);
            node.getPom().getArtifact().setFile(file);

            // Even if you define ArtifactHandlers as components, often because of the
            // initialization order, a proper ArtifactHandler won't be discovered.
            // so force our own ArtifactHandler that gets the extension right.
            ArtifactHandler handler = new ArtifactHandler() {
                public String getExtension() {
                    return AttachArtifactTask.this.getExtension(file.getName());
                }

                public String getDirectory() {
                    return null;
                }

                public String getClassifier() {
                    return null;
                }

                public String getPackaging() {
                    return node.getPom().getPackaging();
                }

                public boolean isIncludesDependencies() {
                    return false;
                }

                public String getLanguage() {
                    return null;
                }

                public boolean isAddedToClasspath() {
                    return false;
                }
            };
            node.getPom().getArtifact().setArtifactHandler(handler);
        } else {
            log("Attaching "+file+" as an attached artifact to "+target, Project.MSG_INFO);

            String type = this.type;
            if(type==null)  {
                type = getExtension(file.getName());
            }
            maven.getProjectHelper().attachArtifact(node.getPom(),type,classifier,file);
        }
    }

    private String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        return name.substring(idx+1);
    }
}
