/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.core.search.backend.testing;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;


/**
 * Indexable resources used as proxy in testing data.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class FakeIndexableResource implements IndexableResource {

    private static final long serialVersionUID = 1L;

    private final IndexableResourceConf conf;

    public FakeIndexableResource(IndexableResourceConf conf) {
        this.conf = conf;
    }

    public IndexableResourceConf getConfiguration() {
        return conf;
    }

    public String getName() {
        return conf.getName();
    }

    public Serializable getValueFor(String indexableDataName)
            throws IndexingException {
        return null;
    }

    public String computeId() {
        return null;
    }

    public ACP computeAcp() {
        return null;
    }

}
