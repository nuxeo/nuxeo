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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.base;

import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

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

        public Builder(SourceNode sourceNode, String importWritePath,
                ImporterLogger log) {
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
            return new ImporterRunnerConfiguration(sourceNode, importWritePath,
                    log, skipRootContainerCreation, batchSize, nbThreads, jobName);
        }

    }

    public final SourceNode sourceNode;

    public final String importWritePath;

    public final boolean skipRootContainerCreation;

    public final int batchSize;

    public final int nbThreads;

    public final String jobName;

    public final ImporterLogger log;

    protected ImporterRunnerConfiguration(SourceNode sourceNode,
            String importWritePath, ImporterLogger log,
            boolean skipRootContainerCreation, int batchSize, int nbThreads,
            String jobName) {
        this.sourceNode = sourceNode;
        this.importWritePath = importWritePath;
        this.log = log;
        this.skipRootContainerCreation = skipRootContainerCreation;
        this.batchSize = batchSize;
        this.nbThreads = nbThreads;
        this.jobName = jobName;
    }

}
