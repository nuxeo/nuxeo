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

import org.nuxeo.ecm.automation.OperationDocumentation;

/**
 * Exporter for the Java part of a Bonita connector
 *
 * @since 5.4.1
 */
public class BonitaJavaExporter {

    protected final BonitaExportConfiguration configuration;

    protected final OperationDocumentation operation;

    public BonitaJavaExporter(BonitaExportConfiguration configuration,
            OperationDocumentation operation) {
        super();
        this.configuration = configuration;
        this.operation = operation;
    }

    public String run() {
        // TODO
        return "java class content";
    }

}
