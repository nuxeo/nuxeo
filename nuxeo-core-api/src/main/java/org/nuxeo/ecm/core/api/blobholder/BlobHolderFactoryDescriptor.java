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
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for contributed factories.
 *
 * @author tiry
 */
@XObject("blobHolderFactory")
public class BlobHolderFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@docType")
    protected String docType;

    @XNode("@class")
    private Class adapterClass;

    public String getName() {
        return name;
    }

    public String getDocType() {
        return docType;
    }

    public BlobHolderFactory getFactory() throws InstantiationException,
            IllegalAccessException {
        return (BlobHolderFactory) adapterClass.newInstance();
    }

}
