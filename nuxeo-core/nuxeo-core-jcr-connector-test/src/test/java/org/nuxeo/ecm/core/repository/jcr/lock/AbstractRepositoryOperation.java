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
 *     matic
 */
package org.nuxeo.ecm.core.repository.jcr.lock;

import java.util.Random;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public abstract class AbstractRepositoryOperation implements RepositoryOperation {
    
    @SuppressWarnings("unchecked")
    public static RepositoryOperation getRandomOperation(Random random, CoreSession session, DocumentRef ref) {
        Class<? extends AbstractRepositoryOperation> types[] = 
            new Class[] { LockDocumentOperation.class, UnlockDocumentOperation.class };
        try {
            return types[random.nextInt()%types.length].getConstructor(CoreSession.class,DocumentRef.class).newInstance(session,ref);
        } catch (Exception e) {
            throw new RuntimeException("Check code", e);
        }
    }
    
    protected CoreSession session;
    protected DocumentModel doc;
    
    protected AbstractRepositoryOperation(CoreSession session, DocumentModel doc) {
        this.session = session;
        this.doc = doc;
    }
    
    public void operateWith() throws ClientException {
        doOperate();
    }
    
    protected abstract void doOperate() throws ClientException;
}