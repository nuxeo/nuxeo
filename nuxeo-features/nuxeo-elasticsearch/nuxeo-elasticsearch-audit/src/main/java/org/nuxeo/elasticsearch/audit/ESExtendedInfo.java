/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.elasticsearch.audit;

import java.io.Serializable;

import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;

/**
 * Extended info for the {@link ESAuditBackend}.
 * 
 * @since 7.10
 */
public class ESExtendedInfo implements ExtendedInfo {

    private static final long serialVersionUID = -8946082235160679850L;

    protected Serializable value;

    public ESExtendedInfo(Serializable value) {
        this.value = value;
    }

    @Override
    public Long getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setId(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getSerializableValue() {
        return value;
    }

    @Override
    public <T> T getValue(Class<T> clazz) {
        return clazz.cast(getSerializableValue());
    }

}
