/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.task.update;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RollbackOptions {

    protected String pkgId;

    protected String key;

    protected String version;

    protected boolean deleteOnExit;

    public RollbackOptions(String pkgId, String key, String version) {
        this.pkgId = pkgId;
        this.key = key;
        this.version = version;
    }

    /**
     * @param key
     * @param opt
     * @since 5.7
     */
    public RollbackOptions(String key, UpdateOptions opt) {
        this.key = key;
        this.pkgId = opt.pkgId;
        this.version = opt.version;
        this.deleteOnExit = opt.deleteOnExit;
    }

    public String getPackageId() {
        return pkgId;
    }

    public String getKey() {
        return key;
    }

    public String getVersion() {
        return version;
    }

    public boolean isDeleteOnExit() {
        return deleteOnExit;
    }

    public void setDeleteOnExit(boolean deleteOnExit) {
        this.deleteOnExit = deleteOnExit;
    }

    /**
     * @since 5.7
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
