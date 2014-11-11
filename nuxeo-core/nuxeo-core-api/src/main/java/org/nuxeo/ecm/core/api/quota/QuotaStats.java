/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.quota;

/**
 * Adapter giving statistics about a given
 * {@link org.nuxeo.ecm.core.api.DocumentModel}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface QuotaStats {

    /**
     * Returns the intrinsic cardinal value of the underlying document.
     */
    long getIntrinsic();

    /**
     * Returns the cardinal value of all the children of the underlying
     * document.
     */
    long getChildren();

    /**
     * Returns the cardinal value of all the descendants of the underlying
     * document. plus the value of {@link #getIntrinsic()}.
     */
    long getTotal();

}
