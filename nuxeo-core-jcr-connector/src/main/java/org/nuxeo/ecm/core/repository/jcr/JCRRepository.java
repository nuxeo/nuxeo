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

import java.io.Serializable;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.schema.NXSchema;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings({"SuppressionAnnotation"})
public class JCRRepository extends RepositoryImpl implements Repository {

    private static final Log log = LogFactory.getLog(JCRRepository.class);

    private final Map sessions;

    private final SchemaManager typeMgr = NXSchema.getSchemaManager();

    private final SecurityManager securityManager;

    private final String name;

    private boolean initialized = false;

    // for debug
    private int startedSessions = 0;
    private int closedSessions = 0;

    public JCRRepository(RepositoryDescriptor descriptor, RepositoryConfig config)
            throws IllegalAccessException, InstantiationException, RepositoryException {
        super(config);
        if (descriptor.getSecurityManagerClass() == null) {
            securityManager = new JCRSecurityManager();
        } else {
            securityManager = descriptor.getSecurityManager();
        }
        sessions = new ReferenceMap();
        name = descriptor.getName();
    }

    public String getName() {
        return name;
    }

    public static JCRRepository create(RepositoryDescriptor descriptor)
            throws Exception {
        RepositoryConfig config = RepositoryConfig.create(
                descriptor.getConfigurationFile(), descriptor.getHomeDirectory());
        return new JCRRepository(descriptor, config);
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public javax.jcr.Repository jcrRepository() {
        return this;
    }

    // TODO cache sessions and return same session when context is he same
    public org.nuxeo.ecm.core.model.Session getSession(Map<String, Serializable> ctx)
            throws DocumentException {
        if (!initialized) {
            initialize();
            if (ctx != null) {
                ctx.put("REPOSITORY_FIRST_ACCESS", Boolean.TRUE);
            }
        }
        return new JCRSession(this, null, ctx);
    }

    public org.nuxeo.ecm.core.model.Session getSession(long sessionId)
            throws DocumentException {
        return getCachedSession(sessionId);
    }

    @SuppressWarnings("unchecked")
    private synchronized void cacheSession(
            org.nuxeo.ecm.core.model.Session session) {
        if (log.isDebugEnabled()) {
            log.debug("Pooling session: " + session.getSessionId());
        }
        sessions.put(session.getSessionId(), session);
    }

    private synchronized void uncacheSession(
            org.nuxeo.ecm.core.model.Session session) {
        long sid = session.getSessionId();
        if (log.isDebugEnabled()) {
            log.debug("Removing pooled session: " + sid);
        }
        sessions.remove(sid);
    }

    private synchronized org.nuxeo.ecm.core.model.Session getCachedSession(
            Long sid) {
        org.nuxeo.ecm.core.model.Session session = (org.nuxeo.ecm.core.model.Session) sessions.get(sid);
        if (log.isDebugEnabled()) {
            log.debug("Lookup session using sid: " + sid + " > " + session);
        }
        return session;
    }

    @SuppressWarnings("unchecked")
    private synchronized org.nuxeo.ecm.core.model.Session[] getCachedSessions() {
        return (org.nuxeo.ecm.core.model.Session[]) sessions.values().toArray(
                new org.nuxeo.ecm.core.model.Session[sessions.size()]);
    }

    public synchronized org.nuxeo.ecm.core.model.Session[] getOpenedSessions()
            throws DocumentException {
        return getCachedSessions();
    }

    public SchemaManager getTypeManager() {
        return typeMgr;
    }

    public void initialize() throws DocumentException {
        try {
            // register ecm types
            Session sess = login();
            BuiltinTypes.registerTypes(typeMgr, sess.getWorkspace());
            sess.logout();
            initialized = true;
        } catch (Exception e) {
            throw new DocumentException("Failed to initialize repository", e);
        }
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
    }

    void aboutToCloseSession(org.nuxeo.ecm.core.model.Session session) {
        // remove session from pool
        uncacheSession(session);
    }

    void sessionClosed(org.nuxeo.ecm.core.model.Session session) {
        // ignoring
        closedSessions++;
    }

    void sessionStarted(org.nuxeo.ecm.core.model.Session session) {
        // put session ion session pool
        cacheSession(session);
        startedSessions++;
    }

    public int getStartedSessionsCount() {
        return startedSessions;
    }

    public int getClosedSessionsCount() {
        return closedSessions;
    }

    public int getActiveSessionsCount() {
        return sessions.size();
    }


    // These lines are only for debug JCA (transactions)
    // uncomment them to change the jackrabbit XASessionImpl

    // /* (non-Javadoc)
    // * @see
    // org.apache.jackrabbit.core.RepositoryImpl#createSessionInstance(org.apache.jackrabbit.core.security.AuthContext,
    // org.apache.jackrabbit.core.config.WorkspaceConfig)
    // */
    // @Override
    // protected SessionImpl createSessionInstance(AuthContext loginContext,
    // WorkspaceConfig wspConfig) throws AccessDeniedException,
    // RepositoryException {
    // return new XASessionWrapper(this, loginContext, wspConfig);
    // }
    //
    // /* (non-Javadoc)
    // * @see
    // org.apache.jackrabbit.core.RepositoryImpl#createSessionInstance(javax.security.auth.Subject,
    // org.apache.jackrabbit.core.config.WorkspaceConfig)
    // */
    // @Override
    // protected SessionImpl createSessionInstance(Subject subject,
    // WorkspaceConfig wspConfig) throws AccessDeniedException,
    // RepositoryException {
    // return new XASessionWrapper(this, subject, wspConfig);
    // }

}
