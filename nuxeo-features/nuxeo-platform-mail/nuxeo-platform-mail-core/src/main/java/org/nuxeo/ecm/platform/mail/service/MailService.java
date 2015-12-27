/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.service;

import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
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
     * The actions will start from this folder. The context is be passed to the sessionFactory to be able to find the
     * session. Use the default session if context is not used.
     */
    MailBoxActions getMailBoxActions(String factoryName, String folderName) throws MessagingException;

    MailBoxActions getMailBoxActions(String factoryName, String folderName, Map<String, Object> context)
            throws MessagingException;

    /**
     * Gets the pipe of actions for given name
     */
    MessageActionPipe getPipe(String name);

    /**
     * Sends a mail using the setting of this factory to this recipients.
     * <p>
     * The context is passed to the sessionFactory to be able to find the session. Use the default session if context is
     * not used. This template is used, replacing variables with the ones from this variables.
     */
    void sendMail(String text, String subject, String factory, Address[] recipients);

    void sendMail(String text, String subject, String factory, Address[] recipients, Map<String, Object> context);

    /**
     * Returns a connected store for this factory. The store needs to be closed after use.
     *
     * @param name The name of the factory that provides the properties.
     * @return the store.
     */
    Store getConnectedStore(String name) throws MessagingException;

    Store getConnectedStore(String name, Map<String, Object> context) throws MessagingException;

    /**
     * Returns a connected transport for this factory. The transport needs to be closed after use.
     *
     * @param name the name of the factory that provides the properties.
     * @return the transport.
     */
    Transport getConnectedTransport(String name) throws MessagingException;

    Transport getConnectedTransport(String name, Map<String, Object> context) throws MessagingException;

    /**
     * Returns a session for this factory, using the context to find the session or the default if no context is
     * provided.
     */
    Session getSession(String name);

    Session getSession(String name, Map<String, Object> context);

    PropertiesFetcher getFetcher(String name);

}
