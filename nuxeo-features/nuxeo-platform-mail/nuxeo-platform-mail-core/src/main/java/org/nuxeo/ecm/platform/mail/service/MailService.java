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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.service;

import java.util.Map;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;

import org.nuxeo.ecm.platform.mail.action.MailBoxActions;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.fetcher.PropertiesFetcher;

/**
 * @author Alexandre Russel
 */
public interface MailService {

    /**
     * Returns an actions pipe configured with this factory.
     * <p>
     * The actions will start from this folder. The context is be passed to the
     * sessionFactory to be able to find the session. Use the default session if
     * context is not used.
     */
    MailBoxActions getMailBoxActions(String factoryName,
            String folderName) throws Exception;

    MailBoxActions getMailBoxActions(String factoryName,
            String folderName, Map<String, Object> context) throws Exception;

    /**
     * Gets the pipe of actions for given name
     */
    MessageActionPipe getPipe(String name);

    /**
     * Sends a mail using the setting of this factory to this recipients.
     * <p>
     * The context is passed to the sessionFactory to be able to find the
     * session. Use the default session if context is not used. This template is
     * used, replacing variables with the ones from this variables.
     */
    void sendMail(String text, String subject, String factory,
            Address[] recipients) throws Exception;

    void sendMail(String text, String subject, String factory,
            Address[] recipients, Map<String, Object> context) throws Exception;

    /**
     * Returns a connected store for this factory. The store needs to be closed
     * after use.
     *
     * @param name The name of the factory that provides the properties.
     * @return the store.
     */
    Store getConnectedStore(String name) throws Exception;

    Store getConnectedStore(String name, Map<String, Object> context)
            throws Exception;

    /**
     * Returns a connected transport for this factory. The transport needs to be
     * closed after use.
     *
     * @param name the name of the factory that provides the properties.
     * @return the transport.
     */
    Transport getConnectedTransport(String name) throws Exception;

    Transport getConnectedTransport(String name, Map<String, Object> context)
            throws Exception;

    /**
     * Returns a session for this factory, using the context to find the session
     * or the default if no context is provided.
     */
    Session getSession(String name) throws Exception;

    Session getSession(String name, Map<String, Object> context)
            throws Exception;

    PropertiesFetcher getFetcher(String name) throws Exception;

}
