/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     slacoin
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import org.nuxeo.ecm.core.api.DocumentRef;

public class UnlockOperation extends  Operation<String> {

    private static final long serialVersionUID = 1L;
    protected final DocumentRef ref;

    public UnlockOperation(DocumentRef ref) {
        super("__UNLOCK__");
        this.ref = ref;
    }

    @Override
    public String doRun(ProgressMonitor montior) throws Exception {
       session.removeLock(ref);
       addModification(new Modification(ref, Modification.STATE));
       return ref + " is unlocked";
    }

}
