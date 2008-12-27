/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class ManagementNameFormatter {

    private ManagementNameFormatter() {
    }

    public static final String NUXEO_DOMAIN_NAME = "nx";

    public static String formatManagedName(String domainName, String typeName,
            String instanceName) {
        return String.format("%s:type=%s,name=%s", domainName, typeName,
                instanceName);
    }

    public static String formatManagedName(String typeName, String instanceName) {
        return formatManagedName(NUXEO_DOMAIN_NAME, typeName, instanceName);
    }

    public static String formatManagedName(String instanceName) {
        return formatManagedName("service", instanceName);
    }

    public static String formatManagedNames(String typeName) {
        return formatManagedNames(NUXEO_DOMAIN_NAME, typeName);
    }

    public static String formatManagedNames(String domainName, String typeName) {
        return String.format("%s:type=%s", domainName, typeName);
    }

    protected static final Pattern namePattern = Pattern.compile("(.*):(.*)");

    public static boolean isQualified(String value) {
        Matcher matcher = namePattern.matcher(value);
        return matcher.matches();
    }

    public static ObjectName getObjectName(String qualifiedName) {
        if (!isQualified(qualifiedName)) {
            throw new IllegalArgumentException(qualifiedName
                    + " is not a fully qualified name");
        }
        ObjectName name = null;
        try {
            name = new ObjectName(qualifiedName);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(qualifiedName
                    + " is not correct", e);
        }
        return name;
    }
}
