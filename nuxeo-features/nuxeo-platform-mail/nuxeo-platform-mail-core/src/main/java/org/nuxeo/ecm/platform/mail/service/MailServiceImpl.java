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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.mail.action.MailBoxActions;
import org.nuxeo.ecm.platform.mail.action.MailBoxActionsImpl;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipeDescriptor;
import org.nuxeo.ecm.platform.mail.fetcher.PropertiesFetcher;
import org.nuxeo.ecm.platform.mail.fetcher.PropertiesFetcherDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Alexandre Russel
 */
public class MailServiceImpl extends DefaultComponent implements MailService {

    private static final String SESSION_FACTORY = "sessionFactory";

    private static final String PROPERTIES_FETCHER = "propertiesFetcher";

    private static final String ACTION_PIPES = "actionPipes";

    private final Map</* fetcher name */String, Class<? extends PropertiesFetcher>> fetchers = new HashMap<String, Class<? extends PropertiesFetcher>>();

    private final Map</* session factory name */String, SessionFactoryDescriptor> sessionFactories = new HashMap<String, SessionFactoryDescriptor>();

    private final Map</* session factory name */String, PropertiesFetcher> configuredFetchers = new HashMap<String, PropertiesFetcher>();

    private final Map<String, MessageActionPipe> actionPipesRegistry = new HashMap<String, MessageActionPipe>();

    private final Map<String, MessageActionPipeDescriptor> actionPipeDescriptorsRegistry = new HashMap<String, MessageActionPipeDescriptor>();

    protected final Map<String, Session> sessions = new ConcurrentHashMap<>();

    static {
        setDecodeUTFFileNamesSystemProperty();
    }

    public void onApplicationStarted(ComponentContext context) {
        sessions.clear();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(SESSION_FACTORY)) {
            SessionFactoryDescriptor descriptor = (SessionFactoryDescriptor) contribution;
            registerSessionFactory(descriptor);
        } else if (extensionPoint.equals(PROPERTIES_FETCHER)) {
            PropertiesFetcherDescriptor descriptor = (PropertiesFetcherDescriptor) contribution;
            fetchers.put(descriptor.getName(), descriptor.getFetcher());
        } else if (extensionPoint.equals(ACTION_PIPES)) {
            MessageActionPipeDescriptor descriptor = (MessageActionPipeDescriptor) contribution;
            registerActionPipe(descriptor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // TODO deal with other extension points
        if (extensionPoint.equals(ACTION_PIPES)) {
            MessageActionPipeDescriptor descriptor = (MessageActionPipeDescriptor) contribution;
            actionPipesRegistry.remove(descriptor.getName());
        }
    }

    private void registerSessionFactory(SessionFactoryDescriptor descriptor) {
        sessionFactories.put(descriptor.getName(), descriptor);
    }

    private void registerActionPipe(MessageActionPipeDescriptor descriptor) {
        if (!descriptor.getOverride()) {
            MessageActionPipeDescriptor existingDescriptor = actionPipeDescriptorsRegistry.get(descriptor.getName());
            if (existingDescriptor != null) {
                descriptor.merge(existingDescriptor);
            }
        }
        actionPipeDescriptorsRegistry.put(descriptor.getName(), descriptor);
        actionPipesRegistry.put(descriptor.getName(), descriptor.getPipe());
    }

    private static void setDecodeUTFFileNamesSystemProperty() {
        String toDecodeTheFilenames = Framework.getRuntime().getProperty("mail.mime.decodefilename");
        if (toDecodeTheFilenames == null) {
            return;
        }
        toDecodeTheFilenames = toDecodeTheFilenames.trim().toLowerCase();
        if (toDecodeTheFilenames.equals("true") || toDecodeTheFilenames.equals("yes")) {
            System.setProperty("mail.mime.decodefilename", "true");
            return;
        }
        System.setProperty("mail.mime.decodefilename", "false");
    }

    public Store getConnectedStore(String name) throws MessagingException {
        return getConnectedStore(name, null);
    }

    public Store getConnectedStore(String name, Map<String, Object> context) throws MessagingException {
        Properties props = getProperties(name, context);
        Session session = newSession(props);
        Store store = session.getStore();
        store.connect(props.getProperty("user"), props.getProperty("password"));
        return store;
    }

    private Properties getProperties(String name, Map<String, Object> map) {
        return getFetcher(name).getProperties(map);
    }

    public Transport getConnectedTransport(String name) throws MessagingException {
        return getConnectedTransport(name, null);
    }

    public Transport getConnectedTransport(String name, Map<String, Object> context) throws MessagingException {
        Properties props = getProperties(name, context);
        Session session = newSession(props);
        Transport transport = session.getTransport();
        transport.connect(props.getProperty("user"), props.getProperty("password"));
        return transport;
    }

    public Session getSession(String name) {
        return getSession(name, null);
    }

    public Session getSession(String name, Map<String, Object> context) {
        Properties props = getProperties(name, context);
        return newSession(props);
    }

    public MailBoxActions getMailBoxActions(String factoryName, String folderName) throws MessagingException {
        return getMailBoxActions(factoryName, folderName, null);
    }

    public MailBoxActions getMailBoxActions(String factoryName, String folderName, Map<String, Object> context)
            throws MessagingException {
        Store store = getConnectedStore(factoryName, context);
        Folder folder = store.getFolder(folderName);
        return new MailBoxActionsImpl(folder, true);
    }

    public void sendMail(String text, String subject, String factory, Address[] recipients) {
        sendMail(text, subject, factory, recipients, null);
    }

    public void sendMail(String text, String subject, String factory, Address[] recipients, Map<String, Object> context) {
        Session session = getSession(factory, context);
        Message message = new MimeMessage(session);
        try {
            message.setFrom();
            message.setSubject(subject);
            message.setRecipients(Message.RecipientType.TO, recipients);
            message.setText(text);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public PropertiesFetcher getFetcher(String name) {
        PropertiesFetcher fetcher = configuredFetchers.get(name);
        if (fetcher == null) {
            String fetcherName = sessionFactories.get(name).getFetcherName();
            Class<? extends PropertiesFetcher> clazz = fetchers.get(fetcherName);
            SessionFactoryDescriptor descriptor = sessionFactories.get(name);
            try {
                fetcher = clazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            fetcher.configureFetcher(descriptor.getProperties());
            configuredFetchers.put(name, fetcher);
        }
        return fetcher;
    }

    public MessageActionPipe getPipe(String name) {
        return actionPipesRegistry.get(name);
    }

    protected Session newSession(Properties props) {
        // build a key for sessions cache
        String sessionKey = props.entrySet()
                                 .stream()
                                 .map(e -> e.getKey() + "#" + e.getValue())
                                 .sorted()
                                 .collect(Collectors.joining("-", "{", "}"));
        return sessions.computeIfAbsent(sessionKey, k -> EmailHelper.newSession(props));
    }
}
