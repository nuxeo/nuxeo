/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;

@XObject("importerConfig")
public class ImporterConfigurationDescriptor {

    @XNode("@sourceNodeClass")
    protected Class<? extends FileSourceNode> sourceNodeClass;

    @XNode("@importerLogClass")
    protected Class<? extends ImporterLogger> importerLogClass;

    @XNode("bulkMode")
    protected Boolean bulkMode;

    @XNode("documentModelFactory")
    protected DocumentModelFactory documentModelFactory;

    @XNode("repository")
    protected String repository;

    /**
     * @since 9.3
     */
    @XNode("enablePerfLogging")
    protected Boolean enablePerfLogging;

    @XObject("documentModelFactory")
    public static class DocumentModelFactory {

        @XNode("@documentModelFactoryClass")
        protected Class<? extends DefaultDocumentModelFactory> documentModelFactoryClass;

        @XNode("@leafType")
        protected String leafType;

        @XNode("@folderishType")
        protected String folderishType;

        public String getFolderishType() {
            return folderishType;
        }

        public String getLeafType() {
            return leafType;
        }

        public Class<? extends DefaultDocumentModelFactory> getDocumentModelFactoryClass() {
            return documentModelFactoryClass;
        }
    }

    public Class<?> getSourceNodeClass() {
        return sourceNodeClass;
    }

    public DocumentModelFactory getDocumentModelFactory() {
        return documentModelFactory;
    }

    public Class<? extends ImporterLogger> getImporterLog() {
        return importerLogClass;
    }

    public String getRepository() {
        return repository;
    }

    public Boolean getBulkMode() {
        return bulkMode;
    }

    /**
     * @since 9.3
     * @return true if the GenericMultiThreadImporter should log performance metrics, false otherwise
     */
    public Boolean getEnablePerfLogging() {
        return enablePerfLogging;
    }
}
