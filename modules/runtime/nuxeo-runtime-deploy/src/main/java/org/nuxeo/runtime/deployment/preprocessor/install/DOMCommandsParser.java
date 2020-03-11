/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.install;

import java.io.IOException;
import java.util.List;

import org.nuxeo.common.utils.FileNamePattern;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.common.utils.PathFilterSet;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.AppendCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.CopyCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.DeleteCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.MkdirCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.MkfileCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.MoveCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.PropertyCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.UnzipCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.commands.ZipCommand;
import org.nuxeo.runtime.deployment.preprocessor.install.filters.ExcludeFilter;
import org.nuxeo.runtime.deployment.preprocessor.install.filters.IncludeFilter;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class DOMCommandsParser {

    // Utility class
    private DOMCommandsParser() {
    }

    public static CommandProcessor parse(Node element) throws IOException {

        CommandProcessor cmdp = new CommandProcessorImpl();
        List<Command> cmds = cmdp.getCommands();

        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String name = node.getNodeName();
                if (name.equals("copy")) {
                    cmds.add(parseCopy((Element) node));
                } else if (name.equals("unzip")) {
                    cmds.add(parseUnzip((Element) node));
                } else if (name.equals("mkdir")) {
                    cmds.add(parseMkdir((Element) node));
                } else if (name.equals("delete")) {
                    cmds.add(parseDelete((Element) node));
                } else if (name.equals("mkfile")) {
                    cmds.add(parseMkfile((Element) node));
                } else if (name.equals("append")) {
                    cmds.add(parseAppend((Element) node));
                } else if (name.equals("zip")) {
                    cmds.add(parseZip((Element) node));
                } else if (name.equals("property")) {
                    cmds.add(parseProperty((Element) node));
                }
            }
            node = node.getNextSibling();
        }

        return cmdp;
    }

    public static PropertyCommand parseProperty(Element element) {
        String name = element.getAttribute("name");
        String value = element.getAttribute("value");
        if (value == null || value.length() == 0) {
            value = element.getTextContent();
        }
        return new PropertyCommand(name, value);
    }

    public static CopyCommand parseCopy(Element element) {
        String from = element.getAttribute("from");
        String to = element.getAttribute("to");
        PathFilter filter = readPathFilter(element);
        return new CopyCommand(new Path(from), new Path(to), filter);
    }

    public static MoveCommand parseMove(Element element) {
        String from = element.getAttribute("from");
        String to = element.getAttribute("to");
        PathFilter filter = readPathFilter(element);
        return new MoveCommand(new Path(from), new Path(to), filter);
    }

    public static AppendCommand parseAppend(Element element) {
        String from = element.getAttribute("from");
        String to = element.getAttribute("to");
        boolean addNewLine = false;
        String addNewLineStr = element.getAttribute("addNewLine").trim();
        if (addNewLineStr.length() > 0) {
            addNewLine = Boolean.parseBoolean(addNewLineStr);
        }
        FileNamePattern pattern = null;
        String patternStr = element.getAttribute("pattern").trim();
        if (patternStr.length() > 0) {
            pattern = new FileNamePattern(patternStr);
        }
        return new AppendCommand(new Path(from), new Path(to), addNewLine, pattern);
    }

    public static UnzipCommand parseUnzip(Element element) {
        String from = element.getAttribute("from");
        String to = element.getAttribute("to");
        String prefix = element.getAttribute("prefix");
        if (prefix != null && prefix.trim().length() == 0) {
            prefix = null;
        }
        PathFilter filter = readPathFilter(element);
        return new UnzipCommand(new Path(from), new Path(to), filter, prefix);
    }

    public static ZipCommand parseZip(Element element) {
        String from = element.getAttribute("from");
        String to = element.getAttribute("to");
        String prefix = element.getAttribute("prefix");
        PathFilter filter = readPathFilter(element);
        return new ZipCommand(new Path(from), new Path(to), prefix, filter);
    }

    public static MkdirCommand parseMkdir(Element element) {
        String path = element.getAttribute("path");
        return new MkdirCommand(new Path(path));
    }

    public static DeleteCommand parseDelete(Element element) {
        String path = element.getAttribute("path");
        return new DeleteCommand(new Path(path));
    }

    public static MkfileCommand parseMkfile(Element element) throws IOException {
        String path = element.getAttribute("path");
        DocumentFragment df = DOMSerializer.getContentAsFragment(element);
        if (df != null) {
            String content = DOMSerializer.toString(df);
            return new MkfileCommand(new Path(path), content.getBytes());
        }
        return new MkfileCommand(new Path(path), null);
    }

    public static PathFilterSet readPathFilter(Element element) {
        PathFilterSet filters = new PathFilterSet();
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = node.getNodeName();
                if (nodeName.equals("include")) {
                    String value = node.getTextContent();
                    if (value != null) {
                        filters.add(new IncludeFilter(new Path(value.trim())));
                    }
                } else if (nodeName.equals("exclude")) {
                    String value = node.getTextContent();
                    if (value != null) {
                        filters.add(new ExcludeFilter(new Path(value.trim())));
                    }
                }
            }
            node = node.getNextSibling();
        }
        return filters.isEmpty() ? null : filters;
    }

}
