/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
