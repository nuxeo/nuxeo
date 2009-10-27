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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.util.FileNameMapper;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * Ant filename mapper to remove version info from filename when copying
 * dependencies.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class VersionMapper implements FileNameMapper {
    private Pattern from;

    private String to;

    public String[] mapFileName(String sourceFileName) {
        if (from != null) {
            Matcher matcher = from.matcher(sourceFileName);
            if (!matcher.matches()) {
                return new String[] {sourceFileName};
            }
        }

        Artifact artifact = MavenClientFactory.getInstance().getGraph().getArtifactByFile(sourceFileName);
        if (artifact != null) {
            String ext = null;
            int p = sourceFileName.lastIndexOf('.');
            if (p > -1) {
                ext = sourceFileName.substring(p);
            }
            String path = artifact.getArtifactId();
            if (to != null) {
                path += to;
            }
            if (artifact.getClassifier() != null) {
                path += "-"+artifact.getClassifier();
            }
            if (ext != null) {
                path += ext;
            }
            return new String[] {path};
        }
        return new String[] { sourceFileName };
    }

    /**
     * Set the regex of the file names to remove versions.
     */
    public void setFrom(String from) {
        if (from != null) {
            this.from = Pattern.compile(from);
        }
    }

    /**
     * If not set the version will be removed otherwise replaced using the value of to.
     */
    public void setTo(String to) {
        this.to = to;
    }

}
