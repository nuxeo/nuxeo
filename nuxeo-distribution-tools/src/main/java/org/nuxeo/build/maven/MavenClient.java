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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.nuxeo.build.ant.profile.AntProfileManager;
import org.nuxeo.build.maven.graph.Graph;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface MavenClient {

    public MavenProjectHelper getProjectHelper();

    public AntProfileManager getAntProfileManager();

    public Graph getGraph();

    public ArtifactResolver getArtifactResolver();

    public ArtifactFactory getArtifactFactory();

    //public ProfileManager getProfileManager();

    public List<Profile> getActiveProfiles();
    
    public MavenProjectBuilder getProjectBuilder();

    public ArtifactRepository getLocalRepository();

    public List<ArtifactRepository> getRemoteRepositories();

    public void resolve(Artifact artifact) throws ArtifactNotFoundException;

    public void resolve(Artifact artifact, List<ArtifactRepository> remoteRepositories) throws ArtifactNotFoundException;

    //public MavenEmbedderLogger getLogger();

}
