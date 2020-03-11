/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.impl;

import java.io.File;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Base implementation of the {@link Rendition} interface that mainly wrapps the {@link RenditionDefinition}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class AbstractRendition implements Rendition {

    protected final RenditionDefinition definition;

    protected static final Log log = LogFactory.getLog(AbstractRendition.class);

    public AbstractRendition(RenditionDefinition definition) {
        this.definition = definition;
    }

    @Override
    public String getIcon() {
        return definition.getIcon();
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public String getCmisName() {
        return definition.getCmisName();
    }

    @Override
    public String getLabel() {
        return definition.getLabel();
    }

    @Override
    public String getKind() {
        return definition.getKind();
    }

    protected RenditionDefinition getDefinition() {
        return definition;
    }

    @Override
    public Calendar getModificationDate() {
        if (isStored()) {
            DocumentModel hdoc = getHostDocument();
            if (hdoc != null) {
                try {
                    return (Calendar) hdoc.getPropertyValue("dc:modified");
                } catch (PropertyException e) {
                    log.error(e);
                }
            }
        } else if (isCompleted()) {
            Calendar cal = Calendar.getInstance();
            Blob blob = getBlob();
            if (blob != null) {
                File file = blob.getFile();
                if (file != null) {
                    cal.setTimeInMillis(file.lastModified());
                }
            }
            return cal;
        }
        return null;
    }

    @Override
    public String getProviderType() {
        return definition.getProviderType();
    }
}
