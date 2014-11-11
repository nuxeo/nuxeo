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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository;

//import javax.ejb.EJBAccessException;

import org.nuxeo.ecm.core.api.CoreSession;


/**
 * A special exception handler that deals with exceptions related to a repository session
 * <p>
 * Exceptions throwns when opening a session are handled separately using
 * {@link RepositoryExceptionHandler#handleException(Throwable)}
 * <p>
 * All other exceptions {@link RepositoryExceptionHandler#handleException(Throwable)} are handled using
 *  {@link RepositoryExceptionHandler#handleOpenException(RepositoryInstance, Throwable)}
 *  This type of exceptions is handled separatelly because it may be due to an authentication failure
 *  and this way we give the oportunity to a client application to retry the authentication
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// Not used.
public interface RepositoryExceptionHandler {

    /**
     * Handle exceptions other than thos fired at session creation
     * @param t the exception to handle
     * @throws Exception if don't know how to hanle rethrow the exception to the caller
     */
    void handleException(Throwable t) throws Exception;

    /**
     * When a session open fails this method is invoked by passing the current
     * repository instance and the exception that occured.
     * <p>
     * This method must handle authentication failures and retry to login and getting the session created.
     *  If the session is opened successfuly it must be returned
     * to the caller otherwise a null value  must be returned so that the caller will rethrown the exception - so finally
     * it will be catched by the {@link RepositoryExceptionHandler#handleException(Throwable)}
     * <p>
     * This ghives a chance to clients to display login dialogs when a session creation failed because
     * of an authentication failure - usually a {@link EJBAccessException}
     * <p>
     * Example of usage
     * <pre>
     * <code>
     * if (t instanceof EJBAccessException) {
     *   int ret = showLoginDialog();
     *   if (ret == OK) {
     *      // do login if not already done in the dialog
     *      // Framework.login(username, password);
     *      // and retry the creation of a new session
     *      return repository.getSession();
     *   }
     * }
     * return null;
     *
     * </code>
     * </pre>
     *
     * @param repository the repository instance that attempted to create a session
     *
     * @param t the re-trhown exception if session cannot be openened
     * @return the session if any was opened otherwise the exception is re-thrown
     */
    CoreSession handleAuthenticationFailure(Repository repository, Throwable t) throws Exception;

}
