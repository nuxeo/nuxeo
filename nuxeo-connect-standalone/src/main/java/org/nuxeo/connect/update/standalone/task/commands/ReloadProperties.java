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
package org.nuxeo.connect.update.standalone.task.commands;

import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Flush all nuxeo caches.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ReloadProperties extends PostInstallCommand {

    public static final String ID = "reload-properties";

    public ReloadProperties() {
        super(ID);
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
        // do nothing
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        try {
            Framework.getRuntime().reloadProperties();
        } catch (Exception e) {
            throw new PackageException("Failed to reload properties", e);
        }
        return new ReloadProperties();
    }

    public void readFrom(Element element) throws PackageException {
    }

    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        writer.end();
    }
}
