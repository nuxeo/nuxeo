/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.connect.update.task.standalone.commands;

import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.w3c.dom.Element;

/**
 * Command for managing the configuration. It allows to set a property, add or remove a template.
 *
 * @since 5.5
 */
public class Config extends AbstractCommand {

    public static final String ID = "config";

    private String addtemplate;

    private String rmtemplate;

    private String set;

    public Config() {
        this(ID);
    }

    protected Config(String id) {
        super(id);
    }

    @Override
    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (addtemplate != null) {
            writer.attr("addtemplate", addtemplate);
        }
        if (rmtemplate != null) {
            writer.attr("rmtemplate", rmtemplate);
        }
        if (set != null) {
            writer.attr("set", set);
        }
        writer.end();
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        Config rollback = new Config();
        ConfigurationGenerator cg = new ConfigurationGenerator();
        cg.init();
        try {
            if (addtemplate != null) {
                cg.addTemplate(addtemplate);
                rollback.rmtemplate = addtemplate;
            }
            if (rmtemplate != null) {
                cg.rmTemplate(rmtemplate);
                rollback.addtemplate = rmtemplate;
            }
            if (set != null) {
                String[] newValue = set.split("=", 2);
                String previousValue = cg.setProperty(newValue[0], (newValue[1].length() > 0 ? newValue[1] : null));
                if (previousValue == null) {
                    previousValue = "";
                }
                rollback.set = newValue[0] + "=" + previousValue;
            }
        } catch (ConfigurationException e) {
            throw new PackageException(e);
        }
        return rollback;
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status) throws PackageException {
        if (addtemplate == null && rmtemplate == null && set == null) {
            status.addError("Cannot execute command in installer."
                    + " Invalid config syntax: neither addtemplate, rmtemplate " + "or set was specified.");
        }
        if (set != null && !set.contains("=")) {
            status.addError("Invalid config syntax: badly-formed property " + set);
        }
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("addtemplate");
        if (v.length() > 0) {
            addtemplate = v;
        }
        v = element.getAttribute("rmtemplate");
        if (v.length() > 0) {
            rmtemplate = v;
        }
        v = element.getAttribute("set");
        if (v.length() > 0) {
            set = v;
        }
    }

}
