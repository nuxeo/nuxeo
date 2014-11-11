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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

// TODO find a better name

/**
 * @deprecated use the nuxeo-platform-layouts-core module descriptors instead
 */
@Deprecated
@XObject("widget")
public class FieldWidget implements Serializable {

    private static final long serialVersionUID = 8230128696886651327L;

    private static final int PRIME_FOR_HASH = 79;

    @XNode("@jsfcomponent")
    String jsfComponent;

    @XNode("@schemaname")
    String schemaName;

    @XNode("@fieldname")
    String fieldName;

    @XNode("@required")
    String required;

    // directory attribute is only needed for nxdir:* JSF components
    @XNode("@directory")
    String directory;

    /**
     * Used by ChainSelect* components. Describe whether the parent
     * columns contains full paths to parent entries or simply the key
     * of parent entry.
     */
    @XNode("@qualifiedParentKeys")
    boolean qualifiedParentKeys = false;

    /** Used by ChainSelect* components **/
    @XNode("@keySeparator")
    String keySeparator;

    // needed for search
    @XNode("@type")
    String type;

    /**
     * This field specifies the property name for a label that is associated with
     * JSF component.
     */
    @XNode("@label")
    String label;

    /** an optional display option. Used by nxdir components. **/
    @XNode("@display")
    String display = "";

    @XNode("@displayIdAndLabel")
    boolean displayIdAndLabel = false;

    @XNode("@maxlength")
    int maxlength = Integer.MAX_VALUE;

    private String prefixedName;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getJsfComponent() {
        return jsfComponent;
    }

    public void setJsfComponent(String jsfComponent) {
        this.jsfComponent = jsfComponent;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getFullName() {
        return schemaName + ':' + fieldName;
    }

    public String getPrefixedName() {
        return prefixedName;
    }

    public void setPrefixedName(String prefixedName) {
        this.prefixedName = prefixedName;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }

    public void setRequired(boolean required) {
        this.required = Boolean.toString(required).toLowerCase();
    }

    public String getLabel() {
        return label;
    }

    /**
     * To be used in the generics JSF - treats the case where
     * there is no label declared
     * @return
     */
    public String getDisplayLabel() {
        if (label == null || label.trim().length()==0){
            return fieldName;
        }
        return label;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public String getKeySeparator() {
        return keySeparator;
    }

    public boolean getQualifiedParentKeys() {
        return qualifiedParentKeys;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getDisplayIdAndLabel() {
        return displayIdAndLabel;
    }

    public void setDisplayIdAndLabel(boolean displayIdLabel) {
        this.displayIdAndLabel = displayIdLabel;
    }

    /**
     * Needed because it is used in comparison.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FieldWidget)) {
            return false;
        }
        final FieldWidget other = (FieldWidget) obj;
        boolean eq = strEq(this.schemaName, other.schemaName);
        eq &= strEq(this.fieldName, other.fieldName);
        eq &= strEq(this.jsfComponent, other.jsfComponent);
        eq &= strEq(this.required, other.required);
        eq &= strEq(this.directory, other.directory);
        eq &= strEq(this.type, other.type);
        return eq;
    }

    private static boolean strEq(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        } else {
            return s1.equals(s2);
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (this.jsfComponent != null) {
            hash = PRIME_FOR_HASH * hash + jsfComponent.hashCode();
        }
        if (this.schemaName != null) {
            hash = PRIME_FOR_HASH * hash + schemaName.hashCode();
        }
        if (this.fieldName != null) {
            hash = PRIME_FOR_HASH * hash + fieldName.hashCode();
        }
        if (this.required != null) {
            hash = PRIME_FOR_HASH * hash + required.hashCode();
        }
        if (this.directory != null) {
            hash = PRIME_FOR_HASH * hash + directory.hashCode();
        }
        if (this.type != null) {
            hash = PRIME_FOR_HASH * hash + type.hashCode();
        }
        return hash;
    }

    public int getMaxlength() {
        return maxlength;
    }

    public void setMaxlength(int maxlength) {
        this.maxlength = maxlength;
    }

}
