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

import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Sequential;
import org.nuxeo.build.maven.EmbeddedMavenClient;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * TODO offline setting is not working
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SettingsTask extends Sequential {

    public File file;
    public Repositories repos;
    public boolean offline = false;
    public boolean interactive = false;
    public boolean debug = false;


    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setFile(File settings) {
        this.file = settings;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }


    public void addRepositories(Repositories repos) {
        this.repos = repos;
    }

    @Override
    public void execute() throws BuildException {

        EmbeddedMavenClient maven = MavenClientFactory.getEmbeddedMaven();
        maven.setOffline(offline);
        maven.setInteractiveMode(interactive);

        if (debug) {
            if (maven.getLogger() == null) {
                maven.setLogger(new MavenEmbedderConsoleLogger());
            }
            maven.getLogger().setThreshold(MavenEmbedderLogger.LEVEL_DEBUG);
        } else {
            maven.setLogger(null);
        }

        try {
            if (!maven.isStarted()) {
                maven.setSettings(file);
                maven.start();
            }
        } catch (Exception e) {
            throw new BuildException("Failed to start maven", e);
        }

        for (Repository repo : repos.getRepositories()) {
            maven.addRemoteRepository(convertRepositoryToMavenModel(repo));
        }
    }

    public org.apache.maven.model.Repository convertRepositoryToMavenModel(Repository repo) {
        org.apache.maven.model.Repository r = new org.apache.maven.model.Repository();
        r.setId(repo.id);
        r.setLayout(repo.layout);
        r.setName(repo.name);
        r.setUrl(repo.url);
        RepositoryPolicy policy = new RepositoryPolicy();
        if (repo.snapshotsPolicy == null) {
            policy.setEnabled(true);
        } else {
            policy.setEnabled(repo.snapshotsPolicy.enabled);
            policy.setChecksumPolicy(repo.snapshotsPolicy.checksumPolicy);
            policy.setUpdatePolicy(repo.snapshotsPolicy.udpatePolicy);
            policy.setModelEncoding(repo.snapshotsPolicy.modelEncoding);
        }
        r.setSnapshots(policy);

        policy = new RepositoryPolicy();
        if (repo.releasesPolicy == null) {
            policy.setEnabled(true);
        } else {
            policy.setEnabled(repo.releasesPolicy.enabled);
            policy.setChecksumPolicy(repo.releasesPolicy.checksumPolicy);
            policy.setUpdatePolicy(repo.releasesPolicy.udpatePolicy);
            policy.setModelEncoding(repo.releasesPolicy.modelEncoding);
        }
        r.setReleases(policy);
        return r;
    }

}
