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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.node;

import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.jpdl.xml.Parsable;

/**
 * @author arussel
 *
 */
public class ForeachFork extends Node implements Parsable {
    private String listExpression;

    private String varName;

    private static final long serialVersionUID = 1L;

    @Override
    public void read(Element forkElement, JpdlXmlReader jpdlXmlReader) {
        Attribute listAttribute = forkElement.attribute("list");
        listExpression = listAttribute.getValue();
        Attribute varAttribute = forkElement.attribute("var");
        varName = varAttribute.getValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(ExecutionContext executionContext) {
        List list = (List) JbpmExpressionEvaluator.evaluate(listExpression, executionContext);
        executionContext.getToken();
        for (Object obj : list) {
            Token childToken = new Token(executionContext.getToken(), obj.toString());
            ExecutionContext childContext = new ExecutionContext(childToken);
            childContext.setVariable(varName, obj);
            leave(childContext);
        }
    }
}
