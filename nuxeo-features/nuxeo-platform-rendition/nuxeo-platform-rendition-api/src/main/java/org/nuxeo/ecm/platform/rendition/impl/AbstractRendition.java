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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.impl;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
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
                } catch (Exception e) {
                    log.error(e);
                }
            }
        } else if (isCompleted()) {
            Calendar cal = GregorianCalendar.getInstance();
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
