/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.shell.automation;

import jline.Completor;

import org.nuxeo.ecm.automation.client.jaxrs.AutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.shell.CommandRegistry;
import org.nuxeo.ecm.shell.CommandType;
import org.nuxeo.ecm.shell.CompletorProvider;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.ValueAdapter;
import org.nuxeo.ecm.shell.automation.cmds.Connect;
import org.nuxeo.ecm.shell.automation.cmds.OperationCommandType;
import org.nuxeo.ecm.shell.automation.cmds.RemoteCommands;
import org.nuxeo.ecm.shell.cmds.GlobalCommands;
import org.nuxeo.ecm.shell.fs.FileSystemShell;
import org.nuxeo.ecm.shell.fs.cmds.FileSystemCommands;
import org.nuxeo.ecm.shell.impl.DefaultCommandType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class AutomationShell extends FileSystemShell implements ValueAdapter,
        CompletorProvider {

    public final static String AUTOMATION_NS = "automation";

    protected RemoteContext ctx;

    public AutomationShell() {
        addCompletorProvider(this);
        addValueAdapter(this);
        GlobalCommands.INSTANCE.addCommandType(DefaultCommandType.fromAnnotatedClass(Connect.class));
    }

    public HttpAutomationClient connect(String url, String username,
            String password) throws Exception {
        if (isConnected()) {
            disconnect();
        }
        HttpAutomationClient client = new HttpAutomationClient(url);
        Session session = client.getSession(username, password);
        ctx = new RemoteContext(this, client, session);

        // switch to automation command namespace
        addRegistry(RemoteCommands.INSTANCE);
        CommandRegistry reg = new CommandRegistry(GlobalCommands.INSTANCE,
                AUTOMATION_NS);
        // build automation registry
        buildCommands(reg, session);
        addRegistry(reg);

        setActiveRegistry(RemoteCommands.INSTANCE.getName());
        return client;
    }

    public boolean isConnected() {
        return ctx != null;
    }

    protected void buildCommands(CommandRegistry reg, Session session) {
        for (OperationDocumentation op : session.getOperations().values()) {
            if (!"Seam".equals(op.requires)) {
                OperationCommandType type = OperationCommandType.fromOperation(op);
                reg.addCommandType(type);// TODO
            }
        }
    }

    public void disconnect() {
        if (ctx != null) {
            ctx.getClient().shutdown();
            ctx.dispose();
            setActiveRegistry(FileSystemCommands.INSTANCE.getName());
            removeRegistry(RemoteCommands.INSTANCE.getName());
            removeRegistry(AUTOMATION_NS);
            removeContextObject(AutomationClient.class);
            removeContextObject(Session.class);
            ctx = null;
        }
    }

    public HttpAutomationClient getClient() {
        return ctx.getClient();
    }

    public Session getSession() {
        return ctx.getSession();
    }

    public RemoteContext getContext() {
        return ctx;
    }

    @Override
    public <T> T getContextObject(Class<T> type) {
        T result = super.getContextObject(type);
        if (result == null && AutomationClient.class.isAssignableFrom(type)) {
            throw new ShellException(
                    "You are not connected! Use the connect command to connect to a remote server first.");
        }
        return result;
    }

    public Completor getCompletor(Shell shell, CommandType cmd, Class<?> type) {
        if (DocRef.class.isAssignableFrom(type)) {
            return new DocRefCompletor(((AutomationShell) shell).getContext());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(Shell shell, Class<T> type, String value) {
        if (type == DocRef.class) {
            return (T) ctx.resolveRef(value);
        }
        return null;
    }

}
