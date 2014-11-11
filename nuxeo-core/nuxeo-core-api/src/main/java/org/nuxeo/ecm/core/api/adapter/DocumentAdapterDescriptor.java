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
 * $Id$
 */

package org.nuxeo.ecm.core.api.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("adapter")
public class DocumentAdapterDescriptor {

    private static final Log log = LogFactory.getLog(DocumentAdapterDescriptor.class);

    @XNode("@facet")
    private String facet;

    @XNode("@class")
    private Class itf;

    private DocumentAdapterFactory factory;


    public DocumentAdapterDescriptor() {
    }

    public DocumentAdapterDescriptor(String facet, Class itf, DocumentAdapterFactory factory) {
        this.facet = facet;
        this.itf = itf;
        this.factory = factory;
    }

    /**
     * Used by XMap to set the factory.
     */
    @XNode("@factory")
    void setFactory(Class<DocumentAdapterFactory> factoryClass) throws Throwable {
        try {
            factory = factoryClass.newInstance();
        } catch (Throwable e) {
            log.error("ERROR instantiating document adapter factory class!");
            throw e;
        }
    }

    public DocumentAdapterFactory getFactory() {
        return factory;
    }

    public void setFactory(DocumentAdapterFactory factory) {
        this.factory = factory;
    }

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public Class getInterface() {
        return itf;
    }

    public void setInterface(Class itf) {
        this.itf = itf;
    }

    @Override
    public String toString() {
        return facet + ": " + itf;
    }

}
