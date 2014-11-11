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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectUtils;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.tools.ant.BuildException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.nuxeo.build.ant.profile.AntProfileManager;
import org.nuxeo.build.maven.graph.Graph;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("unchecked")
public class EmbeddedMavenClient extends MavenEmbedder implements MavenClient {

    protected Graph graph;

    protected File settingsFile;
    private  List<ArtifactRepository> remoteRepos;

    protected AntProfileManager profileMgr = new AntProfileManager();


    public EmbeddedMavenClient() {
        this (null);
    }

    public EmbeddedMavenClient(ClassLoader loader) {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = EmbeddedMavenClient.class.getClassLoader();
            }
        }
        setClassLoader(loader);
        graph = new Graph(this);
    }

    public List<Profile> getActiveProfiles() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public MavenProjectHelper getProjectHelper() {
        try {
            return (MavenProjectHelper) embedder.lookup(MavenProjectHelper.ROLE);
        } catch (Exception e) {
            throw new BuildException("Failed to lookup maven project helper");
        }
    }

    public AntProfileManager getAntProfileManager() {
        return profileMgr;
    }

    public boolean isStarted() {
        return embedder != null;
    }

    public Graph getGraph() {
        return graph;
    }

    public ArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    public Embedder getPlexusEmbedder() {
        return embedder;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public MavenProjectBuilder getProjectBuilder() {
        return mavenProjectBuilder;
    }

    public void setSettings(File settingsFile) {
        this.settingsFile = settingsFile;
    }

    /**
     * Default implementation is not flexible enough
     */
    @Override
    protected void createMavenSettings() throws MavenEmbedderException,
            ComponentLookupException {
        if (settingsFile != null) {
            settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );
            try {
                settings = settingsBuilder.buildSettings(settingsFile);
            } catch (IOException e) {
                throw new MavenEmbedderException("Error creating settings.", e);
            } catch (XmlPullParserException e) {
                throw new MavenEmbedderException("Error creating settings.", e);
            }
        } else {
            super.createMavenSettings();
        }
        settings.setOffline(offline);
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        if (remoteRepos == null) {
            try {
                remoteRepos = buildRepositoriesFromProfiles();
                addDefaultRepositories();
            } catch (Exception e) {
                e.printStackTrace();
                remoteRepos = new ArrayList<ArtifactRepository>();
            }
        }
        return remoteRepos;
    }

    protected void addDefaultRepositories() {
        Repository repo = new Repository();
        repo.setId("central");
        repo.setUrl("http://repo1.maven.org/maven2");
        repo.setLayout("default");
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(false);
        repo.setSnapshots(policy);
        addRemoteRepository(repo);
    }

    public void addRemoteRepository(Repository repo) {
        try {
            ArtifactRepository arepo = ProjectUtils.buildArtifactRepository(
                    repo, artifactRepositoryFactory,
                    getPlexusEmbedder().getContainer());
            getRemoteRepositories().add(arepo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resolve(Artifact artifact) throws ArtifactNotFoundException {
        resolve(artifact, getRemoteRepositories());
    }

    public void resolve(Artifact artifact, List<ArtifactRepository> remoteRepositories) throws ArtifactNotFoundException {
        try {
            super.resolve(artifact, remoteRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ArtifactRepository> buildRepositoriesFromProfiles()
            throws Exception {
        ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
        List<Profile> profiles = getProfileManager().getActiveProfiles();
        for (Profile profile : profiles) {
            List<Repository> prepos = profile.getRepositories();
            for (Repository mavenRepo : prepos) {
                ArtifactRepository artifactRepo = null;
                try {
                    artifactRepo = ProjectUtils.buildArtifactRepository(
                            mavenRepo, artifactRepositoryFactory,
                            getPlexusEmbedder().getContainer());
                } catch (InvalidRepositoryException e) {
                    throw new Exception("Faield to build profile repositories",
                            e);
                }
                repos.add(artifactRepo);
            }
        }
        return repos;
    }

    public List<ArtifactRepository> buildArtifactRepositories(Model model) throws Exception {
        ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
        List<Repository> mavenRepos =  model.getRepositories();
        for (Repository mavenRepo : mavenRepos) {
            ArtifactRepository artifactRepo = null;
            try {
                artifactRepo = ProjectUtils.buildArtifactRepository(
                        mavenRepo, artifactRepositoryFactory,
                        getPlexusEmbedder().getContainer());
            } catch (InvalidRepositoryException e) {
                throw new Exception("Faield to build profile repositories",
                        e);
            }
            repos.add(artifactRepo);
        }
        return repos;
    }


    public Model readModel( String projectId, File pom, boolean strict) throws Exception {
        Reader reader = new FileReader(pom);
        try {
            return readModel(projectId, pom.getAbsolutePath(), reader, strict);
        } finally {
            reader.close();
        }
    }

    public Model readModel( String projectId, URL pom, boolean strict) throws Exception {
        Reader reader = new InputStreamReader(pom.openStream());
        try {
            return readModel(projectId, pom.toExternalForm(), reader, strict);
        } finally {
            reader.close();
        }
    }


    public Model readModel( String projectId,
            String pomLocation,
            Reader reader,
            boolean strict )
    throws IOException, InvalidProjectModelException
    {
        String modelSource = IOUtil.toString( reader );

        if ( modelSource.indexOf( "<modelVersion>" + DefaultMavenProjectBuilder.MAVEN_MODEL_VERSION ) < 0 )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, "Not a v" + DefaultMavenProjectBuilder.MAVEN_MODEL_VERSION  + " POM." );
        }

        StringReader sReader = new StringReader( modelSource );

        try
        {
            return modelReader.read( sReader, strict );
        }
        catch ( XmlPullParserException e )
        {
            throw new InvalidProjectModelException( projectId, pomLocation,
                    "Parse error reading POM. Reason: " + e.getMessage(), e );
        }
    }

}
