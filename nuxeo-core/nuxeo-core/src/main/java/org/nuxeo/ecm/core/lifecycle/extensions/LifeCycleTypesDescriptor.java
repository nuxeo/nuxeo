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
 * $Id: LifeCycleTypesDescriptor.java 20625 2007-06-17 07:21:00Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Life cycle types mapping descriptor.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject(value = "type")
public class LifeCycleTypesDescriptor {
    @XNode("@name")
    protected String name;

    @XNode("@noRecursionForTransitions")
    protected String noRecursionForTransitions;

    @XNode
    protected String type;

    public String getDocumentType() {
        return name;
    }

    public String getNoRecursionForTransitions() {
        return noRecursionForTransitions;
    }

    public String getLifeCycleName() {
        return type;
    }

}
