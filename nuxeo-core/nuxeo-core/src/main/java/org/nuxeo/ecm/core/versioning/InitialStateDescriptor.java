/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to contribute the initial version state of a document.
 * 
 * @author Laurent Doguin
 * @since 5.4.2
 */
@XObject("initialState")
public class InitialStateDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@minor")
    protected int minor = 0;

    @XNode("@major")
    protected int major = 0;

    public int getMinor() {
        return minor;
    }

    public int getMajor() {
        return major;
    }

}
