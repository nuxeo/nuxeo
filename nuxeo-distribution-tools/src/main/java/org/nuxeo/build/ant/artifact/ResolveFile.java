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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.resources.FileResource;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResolveFile extends FileResource {

    public String key;
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


    protected File resolveFile() throws ArtifactNotFoundException {
        MavenClient maven = MavenClientFactory.getInstance();
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        Artifact arti = null;
        if (classifier != null) {
            arti = maven.getArtifactFactory().createArtifactWithClassifier(
                ad.groupId, ad.artifactId, ad.version, ad.type, classifier);
        } else {
            arti = maven.getArtifactFactory().createArtifact(
                    ad.groupId, ad.artifactId, ad.version, ad.scope, ad.type);
        }
        MavenClientFactory.getInstance().resolve(arti);
        return arti.getFile();
    }
    
    @Override
    public File getFile() {
        if (isReference()) {
            return ((FileResource) getCheckedRef()).getFile();
        }
        try {
            return resolveFile();
        } catch (ArtifactNotFoundException e) {
            throw new BuildException("Failed to resolve file: "+key+"; classifier: "+classifier, e);
        }
    }

    @Override
    public File getBaseDir() {
        return isReference()
                ? ((FileResource) getCheckedRef()).getBaseDir()
                : getFile().getParentFile();
    }

}
