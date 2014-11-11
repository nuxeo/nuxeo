/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */

package org.nuxeo.ecm.core.api.thumbnail;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for contributed thumbnail factories.
 * @since 5.7
 */
@XObject("thumbnailFactory")
public class ThumbnailFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@docType")
    protected String docType;

    @XNode("@facet")
    protected String facet;

    @XNode("@factoryClass")
    private Class<? extends ThumbnailFactory> factoryClass;

    public String getName() {
        return name;
    }

    public String getDocType() {
        return docType;
    }

    public String getFacet() {
        return facet;
    }

    public ThumbnailFactory getFactory() throws InstantiationException,
            IllegalAccessException {
        return (ThumbnailFactory) factoryClass.newInstance();
    }

}
