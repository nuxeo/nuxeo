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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.doc.bonita;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;

/**
 * Exporter for the Java part of a Bonita connector
 *
 * @since 5.4.1
 */
public class BonitaJavaExporter {

    protected final BonitaOperationDocumentation bonitaOperation;

    protected final OperationDocumentation operation;

    public BonitaJavaExporter(BonitaOperationDocumentation bonitaOperation) {
        super();
        this.bonitaOperation = bonitaOperation;
        this.operation = bonitaOperation.getOperation();
    }

    public String run() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter fw = new OutputStreamWriter(out,
                BonitaExportConstants.ENCODING);
        fw.write(writePackage(BonitaExportConstants.getDefaultConnectorsPackage()));
        for (String importLib : BonitaExportConstants.getDefaultImports()) {
            fw.write(writeImport(importLib));
        }
        fw.write(writeClass(bonitaOperation.getConnectorId(operation.id),
                writeClassBody()));
        fw.close();
        return out.toString();
    }

    public String writeClass(String className, StringBuffer classBody) {
        return String.format("public class %s extends %s {\n  %s \n}\n",
                className,
                BonitaExportConstants.getDefaultAbstractConnectorClass(),
                classBody);
    }

    // TODO map params types (Eg. pass string -> String)
    public StringBuffer writeClassBody() {
        StringBuffer classBody = new StringBuffer();
        for (Param param : operation.getParams()) {
            classBody.append(writePrivateClassMember(param.type, param.name));
        }
        for (Param param : operation.getParams()) {
            classBody.append(writeSingleParameterSetMethod(param.type,
                    param.name));
        }
        writeVoidProcessSessionMethod(classBody, operation.id,
                operation.getParams(), null, null, null, null);
        return classBody;
    }

    // TODO map params types (Eg. pass string -> String)
    public StringBuffer writeSingleParameterSetMethod(String paramType,
            String param) {
        StringBuffer buff = new StringBuffer("public void ");
        return buff.append("set" + param.substring(0, 1).toUpperCase()
                + param.substring(1) + "(" + paramType + " " + param + ") {\n "
                + "this." + param + " = " + param + ";\n}\n");
    }

    public String writePrivateClassMember(String varType, String varName) {
        return String.format("private %s  %s;\n", varType, varName);
    }

    public String writeImport(String importLib) {
        return String.format("import %s;\n", importLib);
    }

    public String writePackage(String importPackage) {
        return String.format("package %s;\n", importPackage);
    }

    // TODO fix this for all input types and output
    // TODO map params types (Eg. pass string -> String)
    public StringBuffer writeVoidProcessSessionMethod(StringBuffer buff,
            String operationId, List<Param> params, String inputType,
            Object input, Object output, Object outputType) {
        buff.append("public ");
        buff = (output == null ? buff.append("void") : buff.append("Object"));
        buff.append(" processSession(Session session) throws Exception {\n");

        StringBuffer methodCall = new StringBuffer("session.newRequest(\""
                + operationId + "\")");
        StringBuffer inputParamBuff = new StringBuffer(" ");
        if (input != null) {
            if (!(outputType instanceof java.util.List<?>)) {
                // instance new DocRef or new Blob; but Blob seems not
                // supported
                inputParamBuff.append(".setInput(new DocRef(" + input + "))");
            }
        }
        StringBuffer paramsBuff = new StringBuffer();
        for (Param param : params) {
            // assume that they have the same name
            paramsBuff.append(".set(" + "\"" + param.name + "\"" + ","
                    + param.name + ")");
        }
        methodCall.append(inputParamBuff);
        methodCall.append(paramsBuff);
        methodCall.append(".execute();\n");
        buff.append(methodCall);
        return buff;
    }
}
