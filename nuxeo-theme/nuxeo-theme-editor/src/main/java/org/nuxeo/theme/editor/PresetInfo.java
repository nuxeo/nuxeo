/*
 * (C) Copyright 2006-2008 Nuxeo SAS <http://nuxeo.com> and others
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

import org.nuxeo.theme.editor.previews.Preview;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.presets.PresetType;

public class PresetInfo {
    
    private static final String PREVIEW_PROPERTIES_RESOURCE = "/nxthemes/editor/styles/previews.properties";

    private final Properties previewProperties = new Properties();

    final PresetType preset;

    public PresetInfo(PresetType preset) {
        this.preset = preset;
    }

    public String getName() {
        return preset.getTypeName();
    }

    public String getPreview() {
        final String category = preset.getCategory();
        final String previewClassName = getPreviewProperties().getProperty(
                category);
        if (previewClassName == null) {
            return "";
        }
        Preview preview = null;
        try {
            preview = (Preview) Class.forName(previewClassName).newInstance();
        } catch (Exception e) {
        }
        if (preview == null) {
            return "";
        }
        String content = preset.getName();
        return preview.render(preset.getValue(), content);
    }

    /* Load properties */
    private Properties getPreviewProperties() {
        Utils.loadProperties(previewProperties, PREVIEW_PROPERTIES_RESOURCE);
        return previewProperties;
    }

}