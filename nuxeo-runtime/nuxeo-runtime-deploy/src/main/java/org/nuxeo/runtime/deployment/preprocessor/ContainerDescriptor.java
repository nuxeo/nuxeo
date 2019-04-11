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

package org.nuxeo.runtime.deployment.preprocessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandProcessor;
import org.nuxeo.runtime.deployment.preprocessor.install.DOMCommandsParser;
import org.w3c.dom.DocumentFragment;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("container")
public class ContainerDescriptor {

    private static final Log log = LogFactory.getLog(ContainerDescriptor.class);

    @XNode("@name")
    public String name;

    @XNodeMap(value = "template", key = "@name", type = HashMap.class, componentType = TemplateDescriptor.class)
    public Map<String, TemplateDescriptor> templates;

    @XNodeList(value = "directory", type = ArrayList.class, componentType = String.class)
    public List<String> directories;

    // the container directory
    public File directory;

    // registered fragments
    public final FragmentRegistry fragments = new FragmentRegistry();

    // sub containers
    public final List<ContainerDescriptor> subContainers = new ArrayList<>();

    public CommandProcessor install;

    public CommandProcessor uninstall;

    public CommandContext context;

    /**
     * The files to process. If this is not null, directories specified in the configuration are ignored.
     */
    public File[] files;

    @XContent("install")
    public void setInstallCommands(DocumentFragment df) {
        try {
            install = DOMCommandsParser.parse(df);
        } catch (IOException e) {
            log.error("Failed to set install commands");
        }
    }

    @XContent("uninstall")
    public void setUninstallCommands(DocumentFragment df) {
        try {
            uninstall = DOMCommandsParser.parse(df);
        } catch (IOException e) {
            log.error("Failed to set uninstall commands");
        }
    }

}
