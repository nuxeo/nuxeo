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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.Completor;

import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationInput;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.DocRefCompletor;
import org.nuxeo.ecm.shell.fs.FileCompletor;
import org.nuxeo.ecm.shell.impl.AbstractCommandType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class OperationCommandType extends AbstractCommandType {

    public final static int TYPE_VOID = 0;

    public final static int TYPE_DOC = 1;

    public final static int TYPE_BLOB = 2;

    public final static int TYPE_DOCS = 3;

    public final static int TYPE_BLOBS = 4;

    protected OperationDocumentation op;

    protected int inputType = 0;

    public boolean hasVoidInput() {
        return inputType == 0;
    }

    public boolean hasBlobInput() {
        return inputType == 2;
    }

    public boolean hasDocumentInput() {
        return inputType == 1;
    }

    public int getInputType() {
        return inputType;
    }

    public static OperationCommandType fromOperation(OperationDocumentation op) {
        int inputType = 0;
        HashMap<String, Token> params = new HashMap<String, Token>();
        ArrayList<Token> args = new ArrayList<Token>();

        Token tok = new Token();
        tok.name = "-void";
        tok.help = "If void the server will not return the result back";
        tok.isRequired = false;
        tok.setter = new OperationParamSetter(tok.name, "boolean");
        params.put(tok.name, tok);
        tok = new Token();
        tok.name = "-ctx";
        tok.help = "Can be used to inject context properties in Java properties format";
        tok.isRequired = true;
        tok.setter = new OperationParamSetter(tok.name, "string");
        params.put(tok.name, tok);

        for (Param param : op.getParams()) {
            tok = new Token();
            tok.name = "-" + param.name;
            tok.help = "";
            tok.isRequired = true;
            OperationParamSetter os = new OperationParamSetter(param.name,
                    param.getType());
            tok.setter = os;
            tok.completor = os.completor;
            params.put(tok.name, tok);
        }

        inputType = getOperationInputType(op);
        if (inputType == TYPE_DOC || inputType == TYPE_DOCS) {
            tok = new Token();
            tok.index = 0;
            tok.isRequired = false;
            tok.name = "the input document(s)";
            tok.setter = new DocInputSetter();
            tok.completor = DocRefCompletor.class;
            args.add(tok);
        } else if (inputType == TYPE_BLOB || inputType == TYPE_BLOBS) {
            tok = new Token();
            tok.index = 0;
            tok.isRequired = true;
            tok.name = "the input file(s)";
            tok.setter = new BlobInputSetter();
            tok.completor = FileCompletor.class;
            args.add(tok);
        }
        return new OperationCommandType(inputType, op, params, args);
    }

    public static int getOperationInputType(OperationDocumentation op) {
        for (int i = 0; i < op.signature.length; i += 2) {
            if ("void".equals(op.signature[i])) {
                return TYPE_VOID;
            }
            if ("document".equals(op.signature[i])) {
                return TYPE_DOC;
            }
            if ("blob".equals(op.signature[i])) {
                return TYPE_BLOB;
            }
            if ("documents".equals(op.signature[i])) {
                return TYPE_DOCS;
            }
            if ("bloblist".equals(op.signature[i])) {
                return TYPE_BLOBS;
            }
        }
        return TYPE_VOID;
    }

    public OperationCommandType(int inputType, OperationDocumentation op,
            Map<String, Token> params, List<Token> args) {
        super(OperationCommand.class, null, params, args);
        this.op = op;
        this.inputType = inputType;
    }

    public String getHelp() {
        return op.description;
    }

    public String[] getAliases() {
        return new String[0];
    }

    public String getName() {
        return op.id;
    }

    @Override
    protected Runnable createInstance(Shell shell) throws Exception {
        OperationCommand cmd = (OperationCommand) cmdClass.newInstance();
        cmd.init(this, shell, op);
        return cmd;
    }

    public static class OperationParamSetter implements Setter {
        protected String name;

        protected Class<?> type;

        protected Class<? extends Completor> completor;

        public OperationParamSetter(String name, String type) {
            this.name = name;
            if ("document".equals(type)) {
                this.type = DocRef.class;
                this.completor = DocRefCompletor.class;
            } else if ("blob".equals(type)) {
                this.type = File.class;
                this.completor = FileCompletor.class;
            } else {
                this.type = String.class;
            }
        }

        public Class<?> getType() {
            return type;
        }

        public void set(Object obj, Object value) throws ShellException {
            if (value instanceof File) {
                value = new FileBlob((File) value);
            }
            ((OperationCommand) obj).setParam(name, value);
        }
    }

    public static class DocInputSetter implements Setter {

        public Class<?> getType() {
            return DocRef.class;
        }

        public void set(Object obj, Object value) throws ShellException {
            ((OperationCommand) obj).request.setInput((OperationInput) value);
        }
    }

    public static class BlobInputSetter implements Setter {
        public Class<?> getType() {
            return File.class;
        }

        public void set(Object obj, Object value) throws ShellException {
            ((OperationCommand) obj).request.setInput(new FileBlob((File) value));
        }

    }

}
