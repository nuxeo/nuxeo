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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import java.util.ArrayList;
import java.util.List;

public class ArtifactLabel implements Comparable<ArtifactLabel> {

    protected final String id;

    protected String label;

    protected String simpleId;

    public ArtifactLabel(String id, String label, String simpleId) {
        this.id = id;
        this.label = label;
        if (simpleId == null) {
            this.simpleId = label;
        } else {
            this.simpleId = simpleId;
        }
    }

    public String getId() {
        return id;
    }

    public String getSimpleId() {
        return simpleId;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int compareTo(ArtifactLabel other) {
        return label.compareTo(other.label);
    }

    public static ArtifactLabel createLabelFromService(String service) {
        String[] parts = service.split("\\.");
        String label = parts[parts.length - 1];
        return new ArtifactLabel(service, label, null);
    }

    protected static String removePrefix(String name, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
            }
        }
        return name;
    }

    public static ArtifactLabel createLabelFromComponent(String component) {
        String label = component;
        List<String> prefixes = new ArrayList<String>();
        prefixes.add("org.nuxeo.ecm.platform.web.common.");
        prefixes.add("org.nuxeo.ecm.platform.ui.web.");
        prefixes.add("org.nuxeo.ecm.platform.");
        prefixes.add("org.nuxeo.ecm.core.");
        prefixes.add("org.nuxeo.ecm.");
        prefixes.add("org.nuxeo.");
        prefixes.add("webapp.");
        prefixes.add("webengine.");
        prefixes.add("api.");
        label = removePrefix(component, prefixes);
        return new ArtifactLabel(component, label, null);
    }

    public static ArtifactLabel createLabelFromExtensionPoint(String extensionPoint) {
        String[] parts = extensionPoint.split("--");
        String component = parts[0];
        String ep = parts[1];
        return new ArtifactLabel(extensionPoint, ep, component);
    }

    public static ArtifactLabel createLabelFromContribution(String contribution) {
        String[] parts = contribution.split("\\.");
        String label = parts[parts.length - 1];
        return new ArtifactLabel(contribution, label, null);
    }

}
