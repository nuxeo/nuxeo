/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.XASessionImpl;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.AuthContext;

/**
 * A class to wrap the XASessionImpl of jackrabbit for debugging purpose.
 * Use it only for debug. See also JCRRepository changes on how
 * to enable using this class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XASessionWrapper extends XASessionImpl {

    private static final Log log = LogFactory.getLog(XASessionWrapper.class);

    public JCRSession ecmSession;

    /**
     * Creates a new instance of this class.
     *
     * @param rep repository
     * @param loginContext login context containing authenticated subject
     * @param wspConfig workspace configuration
     * @throws AccessDeniedException if the subject of the given login context
     *             is not granted access to the specified workspace
     * @throws RepositoryException if another error occurs
     */
    protected XASessionWrapper(RepositoryImpl rep, AuthContext loginContext,
                               WorkspaceConfig wspConfig)
            throws RepositoryException {
        super(rep, loginContext, wspConfig);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param rep repository
     * @param subject authenticated subject
     * @param wspConfig workspace configuration
     * @throws AccessDeniedException if the given subject is not granted access
     *             to the specified workspace
     * @throws RepositoryException if another error occurs
     */
    protected XASessionWrapper(RepositoryImpl rep, Subject subject,
                               WorkspaceConfig wspConfig)
            throws RepositoryException {

        super(rep, subject, wspConfig);
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        log.debug("starting tx ...." + this);
        //Thread.dumpStack();
        super.start(xid, flags);
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        log.debug("ending tx ...." + this);
        //Thread.dumpStack();
        super.end(xid, flags);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        log.debug("preparing tx ...." + this);
        return super.prepare(xid);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        log.debug("commiting tx ...." + this);
        super.commit(xid, onePhase);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        log.debug("rollback tx ...." + this);
        super.rollback(xid);
    }

    @Override
    public String toString() {
        String ecmSessionString = ecmSession == null ? "N/A"
                : String.valueOf(ecmSession.getSessionId());
        return Integer.toHexString(System.identityHashCode(this)) + "; ecm session id: " + ecmSessionString;
    }

}
