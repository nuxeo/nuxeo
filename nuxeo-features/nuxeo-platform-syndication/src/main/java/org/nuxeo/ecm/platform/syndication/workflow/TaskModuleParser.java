/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TaskModuleParser.java 25203 2007-09-20 11:15:06Z sfermigier $
 */

package org.nuxeo.ecm.platform.syndication.workflow;

import org.jdom.Element;
import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;
import com.sun.syndication.io.impl.DateParser;

public class TaskModuleParser implements ModuleParser {

    private static final Namespace TASK_NS = Namespace.getNamespace("task",
            TaskModule.URI);

    public String getNamespaceUri() {
        return TaskModule.URI;
    }

    public Module parse(Element dcRoot) {
        boolean foundSomething = false;
        TaskModule fm = new TaskModuleImpl();

        Element e = dcRoot.getChild("dueDate", TASK_NS);
        if (e != null) {
            foundSomething = true;
            fm.setDueDate(DateParser.parseW3CDateTime(e.getText()));
        }
        e = dcRoot.getChild("startDate", TASK_NS);
        if (e != null) {
            foundSomething = true;
            fm.setDueDate(DateParser.parseW3CDateTime(e.getText()));
        }
        e = dcRoot.getChild("directive", TASK_NS);
        if (e != null) {
            foundSomething = true;
            fm.setDirective(e.getText());
        }
        e = dcRoot.getChild("name", TASK_NS);
        if (e != null) {
            foundSomething = true;
            fm.setName(e.getText());
        }
        e = dcRoot.getChild("description", TASK_NS);
        if (e != null) {
            foundSomething = true;
            fm.setDescription(e.getText());
        }
        e = dcRoot.getChild("comment", TASK_NS);
        if (e != null) {
            foundSomething = true;
            fm.setComment(e.getText());
        }
        return foundSomething ? fm : null;
    }

}
