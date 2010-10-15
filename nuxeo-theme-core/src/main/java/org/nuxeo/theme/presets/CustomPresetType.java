/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.presets;

public class CustomPresetType extends PresetType {

    public CustomPresetType(String name, String value, String group,
            String category, String label, String description) {
        super(name, value, group, category, label, description);
    }

    @Override
    public String getTypeName() {
        return String.format("%s/%s", group, name);
    }

    @Override
    public String getEffectiveName() {
        return name;
    }

}
