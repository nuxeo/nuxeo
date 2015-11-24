/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.server.handler;

import java.io.IOException;

import net.customware.gwt.dispatch.server.ActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.ActionException;
import net.customware.gwt.dispatch.shared.Result;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.ByteArraySource;
import org.nuxeo.runtime.services.streaming.InputStreamSource;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * This class abstracts all the job for getting the CoreSession, make sure
 * session is cleaned after being used and all Tx stuff goes well
 *
 * @author Stéphane Fourrier
 */
public abstract class AbstractActionHandler<T extends AbstractAction<R>, R extends Result>
        implements ActionHandler<T, R> {

    private static final Log log = LogFactory.getLog(AbstractActionHandler.class);

    public final R execute(T action, ExecutionContext context)
            throws ActionException {
        CoreSession session = openSession(action.getRepositoryName());

        if (session != null) {
            try {
                R result = doExecute(action, context, session);
                session.save();
                return result;
            } catch (Exception e) {
                String message = "Error occured during action... rollbacking : "
                        + e.getMessage();
                log.error(message, e);
                throw new ActionException(message, e);
            } finally {
                CoreInstance.getInstance().close(session);
            }
        } else {
            throw new ActionException("Unable to open session");
        }
    }

    public void rollback(T action, R result, ExecutionContext context)
            throws ActionException {
        TransactionHelper.setTransactionRollbackOnly();
    }

    /**
     * Real job takes place here
     *
     * @throws Exception
     */
    protected abstract R doExecute(T action, ExecutionContext context,
            CoreSession session) throws Exception;

    protected Space getSpaceFromId(String spaceId, CoreSession session)
            throws ClientException {
        SpaceManager spaceManager = getSpaceManager();
        return spaceManager.getSpaceFromId(spaceId, session);
    }

    protected SpaceManager getSpaceManager() throws ClientException {
        try {
            return Framework.getService(SpaceManager.class);
        } catch (Exception e) {
            throw new ClientException("Unable to get Space Manager", e);
        }
    }

    protected CoreSession openSession(String repositoryName)
            throws ActionException {
        try {
            RepositoryManager m = Framework.getService(RepositoryManager.class);
            return m.getRepository(repositoryName).open();
        } catch (Exception e) {
            throw new ActionException("Unable to get session", e);
        }
    }

    protected static Blob getBlob(FileItem item)  {
        StreamSource src;
        if (item.isInMemory()) {
            src = new ByteArraySource(item.get());
        } else {
            try {
                src = new InputStreamSource(item.getInputStream());
            } catch (IOException e) {
                throw WebException.wrap("Failed to get blob data", e);
            }
        }
        String ctype = item.getContentType();
        StreamingBlob blob = new StreamingBlob(src,
                ctype == null ? "application/octet-stream" : ctype);
        blob.setFilename(item.getName());
        try {
            blob.persist();
        } catch (IOException e) {
            throw WebException.wrap("Failed to persist blob data", e);
        }
        return blob;
    }

}
