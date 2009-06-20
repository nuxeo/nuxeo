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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.resources.FileResource;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.graph.AttachmentNode;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactFile extends FileResource {

    protected Node node;
    public String key;
    public boolean strict;
    public String classifier;

    public void setKey(String pattern) {
        int p = pattern.lastIndexOf(';');
        if (p > -1) {
            key = pattern.substring(0, p);
            classifier = pattern.substring(p+1);
        } else {
            key = pattern;
        }
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public Node getNode() {
        if (node == null) {
            if (key.indexOf(':') == -1) { // only artifact Id
                ArtifactDescriptor ad = new ArtifactDescriptor();
                ad.artifactId = key;
                node = MavenClient.getInstance().getGraph().findNode(ad);
            } else {
                node = MavenClient.getInstance().getGraph().findFirst(key);
            }
            if (node == null) {
                throw new BuildException("Artifact with pattern "+key+" was not found in graph");
            } else {
                if (classifier != null)  {
                    // we need to create a virtual node that points to the attachement
                    node = new AttachmentNode(node, classifier);
                }
            }
        }
        return node;
    }

    @Override
    public File getFile() {
        if (isReference()) {
            return ((FileResource) getCheckedRef()).getFile();
        }
        return getNode().getFile();
    }

    @Override
    public File getBaseDir() {
        return isReference()
                ? ((FileResource) getCheckedRef()).getBaseDir()
                : getFile().getParentFile();
    }

}
