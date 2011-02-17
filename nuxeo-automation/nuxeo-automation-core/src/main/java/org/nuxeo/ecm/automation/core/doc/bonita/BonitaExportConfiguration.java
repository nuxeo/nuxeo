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

/**
 * @since 5.4.1
 */
public class BonitaExportConfiguration {

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

    public String getConnectorId(String operationId) {
        return "Nuxeo" + operationId + "Connector";
    }

    public String getSetterName(String fieldName) {
        String res = "set" + fieldName.substring(0).toUpperCase();
        if (fieldName.length() > 1) {
            res += fieldName.substring(1);
        }
        return res;
    }

    public String getGetterName(String fieldName) {
        String res = "get" + fieldName.substring(0).toUpperCase();
        if (fieldName.length() > 1) {
            res += fieldName.substring(1);
        }
        return res;
    }

}
