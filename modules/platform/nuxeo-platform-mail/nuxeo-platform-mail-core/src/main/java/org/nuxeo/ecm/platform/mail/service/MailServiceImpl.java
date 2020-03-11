/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.mail.action.MailBoxActions;
import org.nuxeo.ecm.platform.mail.action.MailBoxActionsImpl;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipeDescriptor;
import org.nuxeo.ecm.platform.mail.fetcher.PropertiesFetcher;
import org.nuxeo.ecm.platform.mail.fetcher.PropertiesFetcherDescriptor;
import org.nuxeo.mail.MailSessionBuilder;
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

    /**
     * Fetchers aggregated by name.
     */
    private final Map<String, Class<? extends PropertiesFetcher>> fetchers = new HashMap<>();

    /**
     * Session factories aggregated by name.
     */
    private final Map<String, SessionFactoryDescriptor> sessionFactories = new HashMap<>();

    /**
     * Fetchers aggregated by session factory name.
     */
    private final Map<String, PropertiesFetcher> configuredFetchers = new HashMap<>();

    private final Map<String, MessageActionPipe> actionPipesRegistry = new HashMap<>();

    private final Map<String, MessageActionPipeDescriptor> actionPipeDescriptorsRegistry = new HashMap<>();

    protected final Map<String, Session> sessions = new ConcurrentHashMap<>();

    static {
        setDecodeUTFFileNamesSystemProperty();
    }

    @Override
    public void stop(ComponentContext context) {
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

    @Override
    public Store getConnectedStore(String name) throws MessagingException {
        return getConnectedStore(name, null);
    }

    @Override
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

    @Override
    public Transport getConnectedTransport(String name) throws MessagingException {
        return getConnectedTransport(name, null);
    }

    @Override
    public Transport getConnectedTransport(String name, Map<String, Object> context) throws MessagingException {
        Properties props = getProperties(name, context);
        Session session = newSession(props);
        Transport transport = session.getTransport();
        transport.connect(props.getProperty("user"), props.getProperty("password"));
        return transport;
    }

    @Override
    public Session getSession(String name) {
        return getSession(name, null);
    }

    @Override
    public Session getSession(String name, Map<String, Object> context) {
        Properties props = getProperties(name, context);
        return newSession(props);
    }

    @Override
    public MailBoxActions getMailBoxActions(String factoryName, String folderName) throws MessagingException {
        return getMailBoxActions(factoryName, folderName, null);
    }

    @Override
    public MailBoxActions getMailBoxActions(String factoryName, String folderName, Map<String, Object> context)
            throws MessagingException {
        Store store = getConnectedStore(factoryName, context);
        Folder folder = store.getFolder(folderName);
        return new MailBoxActionsImpl(folder, true);
    }

    @Override
    public void sendMail(String text, String subject, String factory, Address[] recipients) {
        sendMail(text, subject, factory, recipients, null);
    }

    @Override
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
            throw new NuxeoException("An error occurred while sending a mail", e);
        }
    }

    @Override
    public PropertiesFetcher getFetcher(String name) {
        PropertiesFetcher fetcher = configuredFetchers.get(name);
        if (fetcher == null) {
            String fetcherName = sessionFactories.get(name).getFetcherName();
            Class<? extends PropertiesFetcher> clazz = fetchers.get(fetcherName);
            SessionFactoryDescriptor descriptor = sessionFactories.get(name);
            try {
                fetcher = clazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Unable to init properties fetcher: " + name, e);
            }
            fetcher.configureFetcher(descriptor.getProperties());
            configuredFetchers.put(name, fetcher);
        }
        return fetcher;
    }

    @Override
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
        return sessions.computeIfAbsent(sessionKey, k -> MailSessionBuilder.fromProperties(props).build());
    }
}
