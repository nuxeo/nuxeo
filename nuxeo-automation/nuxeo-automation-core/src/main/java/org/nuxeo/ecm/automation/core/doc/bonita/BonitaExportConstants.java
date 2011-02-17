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

import java.util.ArrayList;
import java.util.List;

/**
 * @since 5.4.1
 */
public class BonitaExportConstants {

    public static final String ENCODING = "UTF-8";

    // reserved field names for login
    static String NUXEO_LOGIN_USERNAME = "nuxeoLoginUserName";

    static String NUXEO_LOGIN_USERPASSWORD = "nuxeoLoginUserPassword";

    static String NUXEO_AUTOMATION_URL = "nuxeoAutomationUrl";

    // reserved field names for inputs
    static String NUXEO_AUTOMATION_DOCUMENT = "nuxeoAutomationDocument";

    static String NUXEO_AUTOMATION_DOCUMENTS = "nuxeoAutomationDocuments";

    static String NUXEO_AUTOMATION_BLOB = "nuxeoAutomationBlob";

    static String NUXEO_AUTOMATION_BLOBS = "nuxeoAutomationBlobs";

    // reserved field name for output
    static String NUXEO_AUTOMATION_RESULT = "nuxeoAutomationResult";

    public static String getDefaultConnectorsPackage() {
        return "org.bonitasoft.connectors.nuxeo";
    }

    public static List<String> getDefaultImports() {
        List<String> importsList = new ArrayList<String>();
        importsList.add("java.util.List");
        importsList.add("org.nuxeo.ecm.automation.client.jaxrs.Session");
        importsList.add("org.nuxeo.ecm.automation.client.jaxrs.model.DocRef");
        importsList.add("org.ow2.bonita.connector.core.ConnectorError");
        return importsList;
    }

    public static String getDefaultAbstractConnectorClass() {
        return "AbstractNuxeoProcessConnector";
    }

}
