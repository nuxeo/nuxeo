/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for Core Service versionRemovalPolicy extension point configuration.
 *
 * @author Florent Guillaume
 */
@XObject("policy")
public class CoreServicePolicyDescriptor {

    @XNode("@class")
    protected String klass;

    public String getKlass() {
        return klass;
    }

}
