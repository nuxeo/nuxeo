/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.JMException;
import javax.management.ObjectName;

import org.nuxeo.runtime.model.ComponentName;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class ObjectNameFactory {

    public static final String NUXEO_DOMAIN_NAME = "org.nuxeo";

    private ObjectNameFactory() {
    }

    public static String formatQualifiedName(String domainName, String typeName, String instanceName) {
        int lastDotIndex = instanceName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String packageName = instanceName.substring(0, lastDotIndex);
            instanceName = instanceName.substring(lastDotIndex + 1);
            return formatQualifiedName(domainName, typeName, packageName, instanceName);
        }
        return String.format("%s:name=%s,type=%s", domainName, instanceName, typeName);
    }

    public static String formatQualifiedName(String domainName, String typeName, String packageName, String instanceName) {
        return String.format("%s:package=%s,name=%s,type=%s", domainName, packageName, instanceName, typeName);
    }

    public static String formatQualifiedName(String typeName, String instanceName) {
        return formatQualifiedName(NUXEO_DOMAIN_NAME, typeName, instanceName);
    }

    public static String formatQualifiedName(ComponentName componentName) {
        return formatQualifiedName(componentName.getType(), componentName.getName());
    }

    public static String formatQualifiedName(String instanceName) {
        return formatQualifiedName("service", instanceName);
    }

    public static String formatMetricQualifiedName(ComponentName name, String metricName) {
        return formatQualifiedName(name) + ",metric=" + metricName + ",management=metric";
    }

    public static String formatMetricQualifiedName(String name, String type) {
        if (NUXEO_DOMAIN_NAME.equals(name)) {
            name = "root";
        }
        if (name.startsWith(NUXEO_DOMAIN_NAME)) {
            name = name.substring(NUXEO_DOMAIN_NAME.length() + 1);
        }
        return String.format("%s:name=%s,type=%s,management=metric", NUXEO_DOMAIN_NAME, name, type);
    }

    public static String formatAVAs(String... avas) {
        StringBuilder sb = new StringBuilder();
        for (String ava : avas) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(ava);
        }
        return sb.toString();
    }

    public static String formatInventoryQualifiedName(ComponentName name) {
        return formatQualifiedName(name) + ",management=inventory";
    }

    public static String formatProbeQualifiedName(ComponentName name) {
        return formatQualifiedName(name) + ",management=probe";
    }

    public static String removeDotPart(String name) {
        int lastDotPos = name.lastIndexOf('.');
        if (lastDotPos != -1) {
            name = name.substring(lastDotPos + 1);
        }
        return name;
    }

    public static String formatShortName(ObjectName name) {
        String shortName = removeDotPart(name.getKeyProperty("name"));
        String typeName = name.getKeyProperty("type");
        if (!typeName.equals("service")) {
            shortName += "-" + typeName;
        }
        Map<String, String> keys = name.getKeyPropertyList();
        for (Map.Entry<String, String> keyEntry : keys.entrySet()) {
            String key = keyEntry.getKey();
            String value = keyEntry.getValue();
            if (key.equals("name")) {
                continue;
            }
            if (key.equals("type") && value.equals("service")) {
                continue;
            }
            shortName += "-" + keyEntry.getValue();
        }
        return shortName;
    }

    public static String formatShortName(String name) {
        ObjectName objectName = getObjectName(name);
        return formatShortName(objectName);
    }

    public static String formatTypeQuery(String typeName) {
        return formatTypeQuery(NUXEO_DOMAIN_NAME, typeName);
    }

    public static String formatTypeQuery(String domainName, String typeName) {
        return String.format("%s:type=%s,*", domainName, typeName);
    }

    private static final Pattern namePattern = Pattern.compile(".*:.*");

    public static boolean hasDomain(String value) {
        if (value == null) {
            return false;
        }
        Matcher matcher = namePattern.matcher(value);
        return matcher.matches();
    }

    private static final Pattern avaPattern = Pattern.compile(".*=.*");

    public static boolean hasAttributeValueAssertion(String value) {
        if (value == null) {
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
            qualifiedName = NUXEO_DOMAIN_NAME + ":name=" + name + ",type=service";
        } else if (!hasDomain(qualifiedName)) {
            qualifiedName = NUXEO_DOMAIN_NAME + ":" + qualifiedName;
        }
        return qualifiedName;
    }

    public static ObjectName getObjectName(String name) {
        String qualifiedName = getQualifiedName(name);
        try {
            return new ObjectName(qualifiedName);
        } catch (JMException e) {
            throw ManagementRuntimeException.wrap(name + " is not correct", e);
        }
    }

    public static ObjectName getObjectName(String name, String avas) {
        String qualifiedName = getQualifiedName(name) + "," + avas;
        try {
            return new ObjectName(qualifiedName);
        } catch (JMException e) {
            throw ManagementRuntimeException.wrap(name + " is not correct", e);
        }
    }

    public static String formatMetricShortName(String name) {
        return name + "-metric";
    }

    public static String formatInventoryShortName(String name) {
        return name + "-inventory";
    }

    public static String formatProbeShortName(String name) {
        return name + "-probe";
    }

}
