/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
       session.unlock(ref);
       addModification(new Modification(ref, Modification.STATE));
       return ref + " is unlocked";
    }

}
