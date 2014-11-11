/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management.probes;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.management.api.Probe;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
@XObject("probe")
public class ProbeDescriptor {

    @XNode("@name")
    private String shortcutName;

    @XNode("@qualifiedName")
    private String qualifiedName;

    @XNode("@class")
    private Class<? extends Probe> probeClass;

    @XNode("label")
    private String label;

    @XNode("description")
    private String description;

    public String getLabel() {
        if (label == null) {
            return "label." + shortcutName;
        }
        return label;
    }

    public String getDescription() {
        if (description == null) {
            return "description." + shortcutName;
        }
        return description;
    }

    public String getShortcut() {
        return shortcutName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public Class<? extends Probe> getProbeClass() {
        return probeClass;
    }

}
