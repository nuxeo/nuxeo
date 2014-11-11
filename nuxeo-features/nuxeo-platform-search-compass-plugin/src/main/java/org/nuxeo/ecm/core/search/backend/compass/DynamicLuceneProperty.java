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

package org.nuxeo.ecm.core.search.backend.compass;

import org.apache.lucene.document.Field;
import org.compass.core.lucene.LuceneProperty;
import org.compass.core.mapping.ResourcePropertyMapping;

/**
 * An override to allow a more dynamic behavior than LuceneProperty.
 *
 * @author gracinet
 *
 */
public class DynamicLuceneProperty extends LuceneProperty {

    private static final long serialVersionUID = 1L;

    public DynamicLuceneProperty(Field field) {
        super(field);
    }

    /**
     * In LuceneResource.add, the property mapping gets overridden by the
     * one from the global registry, we disable this in case the latter is null.
     */
    @Override
    public void setPropertyMapping(ResourcePropertyMapping propertyMapping) {
        if (propertyMapping != null) {
            super.setPropertyMapping(propertyMapping);
        }
    }

}
