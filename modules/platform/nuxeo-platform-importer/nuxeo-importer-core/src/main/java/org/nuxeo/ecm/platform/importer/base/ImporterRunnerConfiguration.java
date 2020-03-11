/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.base;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

/**
 * Hold the configuration of an ImporterRunner.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ImporterRunnerConfiguration {

    public static class Builder {
        private SourceNode sourceNode;

        private String importWritePath;

        private ImporterLogger log;

        private boolean skipRootContainerCreation = false;

        private int batchSize = 50;

        private int nbThreads = 5;

        private String jobName;

        private String repository=null;

        public Builder(SourceNode sourceNode, String importWritePath, ImporterLogger log) {
            this.sourceNode = sourceNode;
            this.importWritePath = importWritePath;
            this.log = log;
        }

        public Builder skipRootContainerCreation(Boolean skipRootContainerCreation) {
            if (skipRootContainerCreation != null) {
                this.skipRootContainerCreation = skipRootContainerCreation;
            }
            return this;
        }

        public Builder repository(String repo) {
            this.repository = repo;
            return this;
        }

        public Builder batchSize(Integer batchSize) {
            if (batchSize != null) {
                this.batchSize = batchSize;
            }
            return this;
        }

        public Builder nbThreads(Integer nbThreads) {
            if (nbThreads != null) {
                this.nbThreads = nbThreads;
            }
            return this;
        }

        public Builder jobName(String jobName) {
            if (jobName != null) {
                this.jobName = jobName;
            }
            return this;
        }

        public ImporterRunnerConfiguration build() {
            return new ImporterRunnerConfiguration(repository, sourceNode, importWritePath, log, skipRootContainerCreation,
                    batchSize, nbThreads, jobName);
        }

    }

    public final SourceNode sourceNode;

    public final String importWritePath;

    public final boolean skipRootContainerCreation;

    public final int batchSize;

    public final int nbThreads;

    public final String jobName;

    public final ImporterLogger log;

    public final String repositoryName;

    protected ImporterRunnerConfiguration(String repositoryName, SourceNode sourceNode, String importWritePath, ImporterLogger log,
            boolean skipRootContainerCreation, int batchSize, int nbThreads, String jobName) {
        this.sourceNode = sourceNode;
        this.importWritePath = importWritePath;
        this.log = log;
        this.skipRootContainerCreation = skipRootContainerCreation;
        this.batchSize = batchSize;
        this.nbThreads = nbThreads;
        this.jobName = jobName;
        if  (repositoryName!=null) {
            this.repositoryName = repositoryName;
        } else {
            this.repositoryName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
        }
    }

}
