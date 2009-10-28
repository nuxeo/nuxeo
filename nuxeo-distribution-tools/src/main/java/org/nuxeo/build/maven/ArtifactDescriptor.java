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
package org.nuxeo.build.maven;

import org.apache.maven.artifact.Artifact;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactDescriptor {

    public String groupId;
    public String artifactId;
    public String version;
    public String type = "jar";
    public String classifier;
    public String scope = "compile";

    public static ArtifactDescriptor emptyDescriptor() {
        ArtifactDescriptor ad = new ArtifactDescriptor();
        ad.scope = null;
        ad.type = null;
        return ad;
    }
    
    public ArtifactDescriptor() {
    }

    public ArtifactDescriptor(String groupId, String artifactId, String version, String type, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
    }

    public ArtifactDescriptor(String expr) {
        parse(expr);
    }

    public void parse(String expr) {
        int p = 0;
        int q = expr.indexOf(':', p);
        if (q == -1) {
            groupId = expr.substring(p);
            return;
        }
        groupId = expr.substring(p, q);

        p = q+1;
        q = expr.indexOf(':', p);
        if (q == -1) {
            artifactId = expr.substring(p);
            return;
        }
        artifactId = expr.substring(p, q);

        p = q+1;
        q = expr.indexOf(':', p);
        if (q == -1) {
            version = expr.substring(p);
            return;
        }
        version = expr.substring(p, q);

        p = q+1;
        q = expr.indexOf(':', p);
        if (q == -1) {
            type = expr.substring(p);
            return;
        }
        type = expr.substring(p, q);

        p = q+1;
        q = expr.indexOf(':', p);
        if (q == -1) {
            classifier = expr.substring(p);
            return;
        }
        classifier = expr.substring(p, q);

        p = q+1;
        q = expr.indexOf(':', p);
        if (q == -1) {
            scope = expr.substring(p);
            return;
        }
        scope = expr.substring(p, q);
    }

    public Artifact toBuildArtifact() {
        return MavenClientFactory.getInstance().getArtifactFactory().createBuildArtifact(
                groupId, artifactId, version, type);
    }
    
    public String getNodeKeyPattern() {
        if (groupId != null) {
             StringBuilder buf = new StringBuilder();
             buf.append(groupId);
             if (artifactId != null) {
                 buf.append(':').append(artifactId);
                 if (version != null) {
                     buf.append(':').append(version);
                     if (type != null) {
                         buf.append(':').append(type);    
                     }
                 }
             }
             return buf.toString();
        }
        return null;
    }

}
