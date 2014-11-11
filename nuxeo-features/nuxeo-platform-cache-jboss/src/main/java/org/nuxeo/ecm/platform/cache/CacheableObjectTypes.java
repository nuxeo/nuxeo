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

package org.nuxeo.ecm.platform.cache;

public enum CacheableObjectTypes {

    CORE_OBJ_DOCUMENT_MODEL("DocModel"), CORE_OBJ_DOCUMENT_REF_CHILDREN(
            "DocRefChildren"), CORE_OBJ_DATA_MODEL("DataModel");

    /**
     * Read-only field.
     */
    public final String prefix;

    private CacheableObjectTypes(String prefix) {
        this.prefix = prefix;
    }

    /*
     * public String getPrefix() { switch (this) { case CORE_OBJ_DATA_MODEL:
     * return "DataModel/"; }
     *
     * throw new EnumConstantNotPresentException(this.getClass(), "unknown"); }
     */

    @Override
    public String toString() {
        return prefix + '/';
    }
}
