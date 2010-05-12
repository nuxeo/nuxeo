/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.apidoc.browse;

import java.util.ArrayList;
import java.util.List;

public class ArtifactLabel implements Comparable<ArtifactLabel>{

    protected String id;

    protected String label;

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public ArtifactLabel(String id, String label) {
        this.id=id;
        this.label = label;
    }

    @Override
    public String toString() {
        return getLabel();
    }


    public int compareTo(ArtifactLabel other) {
        return getLabel().compareTo(other.getLabel());
    }


    public static ArtifactLabel createLabelFromService(String service) {
        String[] parts = service.split("\\.");
        String label = parts[parts.length-1];
        return new ArtifactLabel(service, label);
    }

    protected static String removePrefix(String name, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return name.replace(prefix, "");
            }
        }
        return name;
    }

    public static ArtifactLabel createLabelFromComponent(String component) {

        String label = component;
        List<String> prefixes = new ArrayList<String>();
        prefixes.add("org.nuxeo.ecm.platform.");
        prefixes.add("org.nuxeo.ecm.core.");
        prefixes.add("org.nuxeo.ecm.");
        prefixes.add("org.nuxeo.");
        label = removePrefix(component, prefixes);
        return new ArtifactLabel(component, label);
    }

    public static ArtifactLabel createLabelFromExtensionPoint(String extensionPoint) {

        String[] parts = extensionPoint.split("--");
        String component = parts[0];
        String ep = parts[1];
        String label = ep + " (" + component + ")";
        return new ArtifactLabel(extensionPoint, label);
    }

    public static ArtifactLabel createLabelFromContribution(String contribution) {
        String[] parts = contribution.split("\\.");
        String label = parts[parts.length-1];
        return new ArtifactLabel(contribution, label);
    }


}
