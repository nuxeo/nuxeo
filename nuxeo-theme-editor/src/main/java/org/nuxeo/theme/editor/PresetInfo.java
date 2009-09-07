/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.editor;

import java.util.Properties;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Preview;
import org.nuxeo.theme.PreviewType;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class PresetInfo {

    private static final String PREVIEW_PROPERTIES_RESOURCE = "/nxthemes/editor/styles/previews.properties";

    private final Properties previewProperties = new Properties();

    final PresetType preset;

    public PresetInfo(PresetType preset) {
        this.preset = preset;
    }

    public String getName() {
        return preset.getName();
    }

    public String getEffectiveName() {
        return preset.getEffectiveName();
    }

    public String getId() {
        return preset.getTypeName();
    }

    public String getPreview() {
        final String category = preset.getCategory();
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        PreviewType previewType = (PreviewType) typeRegistry.lookup(
                TypeFamily.PREVIEW, category);
        final String previewClassName = previewType.getClassName();
        if (previewClassName == null) {
            return "";
        }
        Preview preview = null;
        try {
            preview = (Preview) Class.forName(previewClassName).newInstance();
        } catch (Exception e) {
            e.getStackTrace();
        }
        if (preview == null) {
            return "";
        }
        return preview.render(preset.getValue());
    }

    public String getValue() {
        return preset.getValue();
    }

    public String getCategory() {
        return preset.getCategory();
    }

}
