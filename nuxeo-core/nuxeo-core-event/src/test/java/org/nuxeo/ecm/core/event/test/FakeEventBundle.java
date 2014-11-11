/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.test;

import java.rmi.dgc.VMID;

import org.nuxeo.ecm.core.event.impl.EventBundleImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FakeEventBundle extends EventBundleImpl {

    private static final long serialVersionUID = 1L;

    public void setVMID(VMID vmid) {
        this.vmid = vmid;
    }

}
