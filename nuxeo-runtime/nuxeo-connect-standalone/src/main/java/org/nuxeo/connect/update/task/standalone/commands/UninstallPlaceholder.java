/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Un-Deploy an OSGi bundle from the running platform. The bundle is specified
 * using the absolute bundle file path. The inverse of this command is the
 * Deploy command.
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UninstallPlaceholder extends AbstractCommand {

    public static final String ID = "uninstall";

    protected File file;

    public UninstallPlaceholder() {
        super(ID);
    }

    public UninstallPlaceholder(File file) {
        super(ID);
        this.file = file;
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
        if (file == null) {
            status.addError("Invalid uninstall syntax: No file specified");
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        // standalone mode: nothing to do
        return new InstallPlaceholder(file);
    }

    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("file");
        if (v.length() > 0) {
            file = new File(v);
            guardVars.put("file", file);
        }
    }

    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        writer.end();
    }
}
