/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: DirectoryEntryOutputComponent.java 29914 2008-02-06 14:46:40Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component to display a directory entry.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class DirectoryEntryOutputComponent extends DirectoryAwareComponent {

    private static final Log log = LogFactory.getLog(DirectoryEntryOutputComponent.class);

    public static final String COMPONENT_TYPE = "nxdirectory.DirectoryEntryOutput";

    public static final String COMPONENT_FAMILY = "nxdirectory.DirectoryEntryOutput";

    /**
     * @deprecated standard value attribute should be used instead
     */
    @Deprecated
    protected String entryId;

    protected String keySeparator;

    /**
     * @deprecated never used
     */
    @Deprecated
    protected String display;

    public DirectoryEntryOutputComponent() {
        setRendererType(COMPONENT_TYPE);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
     * @deprecated use standard {@link #getValue()} instead
     */
    @Deprecated
    public String getEntryId() {
        ValueBinding vb = getValueBinding("entryId");
        if (vb != null) {
            log.warn("\"entryId\" attribute is deprecated on "
                    + "DirectoryEntryOutputComponent, use \"value\" instead");
            return (String) vb.getValue(getFacesContext());
        } else {
            return entryId;
        }
    }

    /**
     * @deprecated use standard {@link #setValue(Object)} instead
     */
    @Deprecated
    public void setEntryId(String entryId) {
        log.warn("\"entryId\" attribute is deprecated on "
                + "DirectoryEntryOutputComponent, use \"value\" instead");
        setValue(entryId);
    }

    /**
     * @deprecated use {@link #getLocalize()} instead
     */
    @Deprecated
    public Boolean getTranslate() {
        return localize;
    }

    /**
     * @deprecated use {@link #setLocalize(Boolean)} instead
     */
    @Deprecated
    public void setTranslate(Boolean translate) {
        log.warn("\"translate\" attribute is deprecated on "
                + "DirectoryEntryOutputComponent, use \"localize\" instead");
        localize = translate;
    }

    public String getKeySeparator() {
        if (keySeparator != null) {
            return keySeparator;
        }
        return getStringValue("keySeparator", null);
    }

    public void setKeySeparator(String keySeparator) {
        this.keySeparator = keySeparator;
    }

    /**
     * @deprecated never used
     */
    @Override
    @Deprecated
    public String getDisplay() {
        return display;
    }

    /**
     * @deprecated never used
     */
    @Override
    @Deprecated
    public void setDisplay(String display) {
        this.display = display;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = entryId;
        values[2] = keySeparator;
        values[3] = display;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        entryId = (String) values[1];
        keySeparator = (String) values[2];
        display = (String) values[3];
    }
}
