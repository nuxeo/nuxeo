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

import org.nuxeo.runtime.model.ComponentName;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class ObjectNameFactory {

    public static final String NUXEO_DOMAIN_NAME = "org.nuxeo";

    private ObjectNameFactory() {
    }

    public static String formatQualifiedName(String domainName, String instanceName) {
        int lastDotIndex = instanceName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String packageName = instanceName.substring(0, lastDotIndex);
            instanceName = instanceName.substring(lastDotIndex + 1);
            return formatQualifiedName(domainName, packageName, instanceName);
        }
        return String.format("%s:name=%s", domainName, instanceName);
    }

    public static String formatQualifiedName(String domainName, String packageName, String instanceName) {
        return String.format("%s:package=%s,name=%s", domainName, packageName, instanceName);
    }

    public static String formatQualifiedName(String name) {
        return formatQualifiedName(NUXEO_DOMAIN_NAME, name);
    }

    public static String formatQualifiedName(ComponentName componentName) {
        return formatQualifiedName(componentName.getType(), componentName.getName());
    }

    public static String formatMetricQualifiedName(ComponentName name, String metricName) {
        return formatQualifiedName(name) + ",metric=" + metricName;
    }

    public static String formatMetricQualifiedName(String name, String type) {
        if (NUXEO_DOMAIN_NAME.equals(name)) {
            name ="root";
        }
        if (name.startsWith(NUXEO_DOMAIN_NAME)) {
            name = name.substring(NUXEO_DOMAIN_NAME.length()+1);
        }
        return String.format("%s:name=%s,type=%s", NUXEO_DOMAIN_NAME, name, type);
    }

    public static String formatAVAs(String... avas) {
        StringBuffer buffer = new StringBuffer();
        for (String ava : avas) {
            if (buffer.length() > 0) {
                buffer.append(",");
            }
            buffer.append(ava);
        }
        return buffer.toString();
    }


    public static String removeDotPart(String name) {
        int lastDotPos = name.lastIndexOf('.');
        if (lastDotPos != -1) {
            name = name.substring(lastDotPos + 1);
        }
        return name;
    }

    public static String formatQuery(String domainName) {
        return String.format("%s:name=*,*", domainName);
    }


    private static final Pattern namePattern = Pattern.compile(".*:.*");

    public static boolean hasDomain(String value) {
        if (value==null) {
            return false;
        }
        Matcher matcher = namePattern.matcher(value);
        return matcher.matches();
    }

    private static final Pattern avaPattern = Pattern.compile(".*=.*");

    public static boolean hasAttributeValueAssertion(String value) {
        if (value==null) {
            return false;
        }
        Matcher matcher = avaPattern.matcher(value);
        return matcher.matches();
    }

    public static boolean isQualified(String name) {
        return hasDomain(name) && hasAttributeValueAssertion(name);
    }

    public static String getQualifiedName(String name) {
        String qualifiedName = name;
        if (!hasAttributeValueAssertion(qualifiedName)) {
            qualifiedName = NUXEO_DOMAIN_NAME + ":name=" + name;
        } else if (!hasDomain(qualifiedName)) {
            qualifiedName = NUXEO_DOMAIN_NAME + ":" + qualifiedName;
        }
        return qualifiedName;
    }

    public static ObjectName getObjectName(String name) {
        String qualifiedName = getQualifiedName(name);
        try {
            return new ObjectName(qualifiedName);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(name + " is not correct", e);
        }
    }

    public static ObjectName getObjectName(String name, String avas) {
        String qualifiedName = getQualifiedName(name) + "," + avas;
        try {
            return new ObjectName(qualifiedName);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(name + " is not correct", e);
        }
    }

}
