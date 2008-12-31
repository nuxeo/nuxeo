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
public class ObjectNameFactory {

    private ObjectNameFactory() {
    }

    public static final String NUXEO_DOMAIN_NAME = "nx";

    public static String formatName(String domainName, String typeName,
            String instanceName) {
        return String.format("%s:type=%s,name=%s", domainName, typeName,
                instanceName);
    }

    public static String formatName(String typeName, String instanceName) {
        return formatName(NUXEO_DOMAIN_NAME, typeName, instanceName);
    }

    public static String formatName(String instanceName) {
        return formatName("service", instanceName);
    }

    public static String formatTypeQuery(String typeName) {
        return formatTypeQuery(NUXEO_DOMAIN_NAME, typeName);
    }

    public static String formatTypeQuery(String domainName, String typeName) {
        return String.format("%s:type=%s,*", domainName, typeName);
    }

    private static final Pattern namePattern = Pattern.compile(".*:.*");

    public static boolean hasDomain(String value) {
        Matcher matcher = namePattern.matcher(value);
        return matcher.matches();
    }
    
    private static final Pattern avaPattern = Pattern.compile(".*=.*");
    
    public static boolean hasAttributeValueAssertion(String value) {
        Matcher matcher = avaPattern.matcher(value);
        return matcher.matches();
    }

    public static ObjectName getObjectName(String name) {
        String qualifiedName = name;
        if (!hasAttributeValueAssertion(qualifiedName)) {
            qualifiedName = "nx:type=service,name=" + name;
        } else if (!hasDomain(qualifiedName)) {
            qualifiedName = NUXEO_DOMAIN_NAME + ":" + qualifiedName;
        }
        
        try {
           return new ObjectName(qualifiedName);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(name
                    + " is not correct", e);
        }
    }
    
    public static ObjectName getObjectName(ObjectName name, String attribute, String value) {
        try {
            return new ObjectName(name.getCanonicalName() + "," + attribute + "=" + value);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(e);
        }
    }
}
