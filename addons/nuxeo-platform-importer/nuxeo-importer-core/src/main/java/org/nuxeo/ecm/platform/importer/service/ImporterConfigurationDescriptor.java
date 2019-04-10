/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

}
