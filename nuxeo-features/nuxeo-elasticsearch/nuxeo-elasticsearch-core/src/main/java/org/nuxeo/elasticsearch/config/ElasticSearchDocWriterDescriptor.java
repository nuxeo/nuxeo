/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mcedica@nuxeo.com
 */
package org.nuxeo.elasticsearch.config;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;

/**
 * 
 * @since 7.2
 */
@XObject("writer")
public class ElasticSearchDocWriterDescriptor {

    @XNode("@class")
    protected Class<JsonESDocumentWriter> klass;

    public Class<JsonESDocumentWriter> getKlass() {
        return klass;
    }
}
