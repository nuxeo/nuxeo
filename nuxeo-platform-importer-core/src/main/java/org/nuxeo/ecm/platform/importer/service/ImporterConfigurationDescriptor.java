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

@XObject("importerConfig")
public class ImporterConfigurationDescriptor {

    @XNode("@sourceNode")
    protected Class<?> sourceNodeClass;

    @XNode("@importerLog")
    protected Class<?> importerLog;

    @XNode("@documentModelFactory")
    protected DocumentModelFactory documentModelFactory;

    @XObject("documentModelFactory")
    public static class DocumentModelFactory {

        @XNode("@documentModelFactoryClass")
        protected Class<?> documentModelFactoryClass;

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

        public Class<?> getDocumentModelFactoryClass() {
            return documentModelFactoryClass;
        }
    }

    public Class<?> getSourceNodeClass() {
        return sourceNodeClass;
    }

    public DocumentModelFactory getDocumentModelFactory() {
        return documentModelFactory;
    }

    public Class<?> getImporterLog() {
        return importerLog;
    }

}