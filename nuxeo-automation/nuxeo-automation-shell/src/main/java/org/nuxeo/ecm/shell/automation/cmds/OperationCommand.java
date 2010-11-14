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
package org.nuxeo.ecm.shell.automation.cmds;

import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blobs;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.RemoteContext;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class OperationCommand implements Runnable {

    public static final String ATTR_VOID = "-void";

    public static final String ATTR_SCHEMAS = "-schemas";

    public static final String ATTR_CTX = "-ctx";

    protected Session session;

    protected Shell shell;

    protected OperationDocumentation op;

    protected OperationRequest request;

    protected OperationCommandType type;

    public OperationCommand() {
    }

    public void init(OperationCommandType type, Shell shell,
            OperationDocumentation op) {
        try {
            this.type = type;
            this.shell = shell;
            this.session = shell.getContextObject(Session.class);
            this.op = op;
            this.request = session.newRequest(op.id);
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }

    public void run() {
        try {
            if (request.getInput() == null) {
                if (type.hasDocumentInput()) {
                    request.setInput(shell.getContextObject(RemoteContext.class).getDocument());
                }
            }
            Object result = request.execute();
            if (result instanceof Document) {
                Cat.print(shell.getConsole(), (Document) result);
            } else if (result instanceof Documents) {
                for (Document doc : (Documents) result) {
                    shell.getConsole().println(
                            doc.getPath() + " - " + doc.getTitle());
                }
            } else if (result instanceof FileBlob) {
                shell.getConsole().println(
                        ((FileBlob) result).getFile().getAbsolutePath());
            } else if (result instanceof Blobs) {
                for (Blob blob : (Blobs) result) {
                    shell.getConsole().println(
                            ((FileBlob) blob).getFile().getAbsolutePath());
                }
            }
        } catch (RemoteException e) {
            throw new ShellException(e.getStatus() + " - " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }

    public void setParam(String name, Object value) {
        if (ATTR_SCHEMAS.equals(name)) {
            request.setHeader(Constants.HEADER_NX_SCHEMAS, (String) value);
        } else if (ATTR_VOID.equals(name)) {
            request.setHeader(Constants.HEADER_NX_VOIDOP, (String) value); // TODO
        } else if (ATTR_CTX.equals(name)) {
            for (String v : StringUtils.split(value.toString(), ',', true)) {
                String[] pair = StringUtils.split(v.toString(), '=', true);
                request.setContextProperty(pair[0], pair[1]);
            }
        } else {
            request.set(name, value);
        }
    }

}
