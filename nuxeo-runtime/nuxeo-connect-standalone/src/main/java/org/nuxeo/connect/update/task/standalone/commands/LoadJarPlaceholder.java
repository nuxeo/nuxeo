/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.standalone.commands;

import java.io.File;
import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Element;

/**
 * Install bundle, flush any application cache and perform Nuxeo preprocessing on the bundle. The inverse of this
 * command is Undeploy.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LoadJarPlaceholder extends AbstractCommand {

    public static final String ID = "load-jar";

    protected File file;

    public LoadJarPlaceholder() {
        super(ID);
    }

    public LoadJarPlaceholder(File file) {
        super(ID);
        this.file = file;
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status) throws PackageException {
        // do nothing
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        // standalone mode: nothing to do
        return new UnloadJarPlaceholder(file);
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("file");
        if (v.length() > 0) {
            file = new File(v);
            guardVars.put("file", file);
        }
    }

    @Override
    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        writer.end();
    }
}
