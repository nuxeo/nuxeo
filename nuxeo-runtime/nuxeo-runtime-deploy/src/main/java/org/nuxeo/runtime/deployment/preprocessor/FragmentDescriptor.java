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
import org.nuxeo.runtime.deployment.preprocessor.install.CommandProcessor;
import org.nuxeo.runtime.deployment.preprocessor.install.DOMCommandsParser;
import org.nuxeo.runtime.deployment.preprocessor.template.TemplateContribution;
import org.w3c.dom.DocumentFragment;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("fragment")
public class FragmentDescriptor {

    private static final Log log = LogFactory.getLog(FragmentDescriptor.class);

    /**
     * Marker used for better control on requirements. see "all" marker in FragmentRegistry
     */
    public static final FragmentDescriptor ALL = new FragmentDescriptor("all", true);

    // the name is the name of the XML fragment file for XML fragments
    // or the name of the JAR container for archive or directory fragments
    @XNode("@name")
    public String name;

    @XNode("@version")
    public int version = 0;

    public String fileName;

    public String filePath;

    /**
     * The start level is used to control bundle start order. The following levels are defined:
     * <ul>
     * <li>0 - system level - used by the OSGi framework itself
     * <li>1 - runtime level - used by nuxeo-runtime bundles
     * <li>2 - core level - used for core bundles
     * <li>3 - platform level - used for platform service bundles
     * <li>4 - presentation level - used for UI service bundles (e.g. seam components etc)
     * <li>5 - UI level -used for UI bundles (e.g. war / web, widgets contribs)
     * <li>6 - user level
     * </ul>
     * The start level is overwritten by the one specified at MANIFEST level using the Nuxeo-StartLevel header. If the
     * start header is missing it will be initialized from the OSGi Bundle-Category (if any) as follows:
     * <ul>
     * <li><code>nuxeo-framework</code>
     * <li><code>nuxeo-runtime</code>
     * <li><code>nuxeo-service</code>
     * <li><code>nuxeo-core</code>
     * <li><code>nuxeo-platform</code>
     * <li><code>nuxeo-presentation</code>
     * <li><code>nuxeo-ui</code>
     * <li><code>nuxeo-plugin</code>
     * </ul>
     * If the start level could not be computed then the default value of 6 (user level) is used The recommended method
     * of specifying the start level is to use the <code>Bundle-Category</code> since start level numbering may change
     * (this header has the advantage of using symbolic names)
     */
    @XNode("@startLevel")
    @Deprecated
    public int startLevel;

    @XNodeList(value = "extension", type = TemplateContribution[].class, componentType = TemplateContribution.class)
    public TemplateContribution[] contributions;

    @XNodeList(value = "require", type = ArrayList.class, componentType = String.class)
    public List<String> requires;

    @XNodeList(value = "requiredBy", type = String[].class, componentType = String.class)
    public String[] requiredBy;

    @XNodeMap(value = "template", key = "@name", type = HashMap.class, componentType = TemplateDescriptor.class)
    public Map<String, TemplateDescriptor> templates;

    public CommandProcessor install;

    public CommandProcessor uninstall;

    protected boolean isMarker;

    /**
     *
     */
    public FragmentDescriptor() {
        // TODO Auto-generated constructor stub
    }

    public FragmentDescriptor(String name, boolean isMarker) {
        this.name = name;
        this.isMarker = isMarker;
    }

    public boolean isMarker() {
        return isMarker;
    }

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

    @Override
    public String toString() {
        return name + " [" + fileName + ']';
    }

}
