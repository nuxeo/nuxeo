/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: LogEntryFactoryDescriptor.java 19481 2007-05-27 10:50:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.service.extension;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Extended info descriptor
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("extendedInfo")
public class ExtendedInfoDescriptor implements Serializable {

    private static final long serialVersionUID = 5381693968565370680L;

    @XNode("@key")
    private String key;

    @XNode("@expression")
    private String expression;

    @XNode("@enabled")
    private boolean enabled = true;


    public String getKey() {
        return key;
    }

    public void setKey(String value) {
        key = value;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String value) {
        expression = value;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int hashCode() {
        return key == null ? 0 : key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExtendedInfoDescriptor other = (ExtendedInfoDescriptor) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
