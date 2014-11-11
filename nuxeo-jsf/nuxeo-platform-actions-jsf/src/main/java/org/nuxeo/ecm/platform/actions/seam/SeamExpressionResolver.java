/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions.seam;

import org.jboss.seam.el.SeamELResolver;
import org.nuxeo.ecm.platform.el.ExpressionResolver;

/**
 * Seam expression resolver, adding the Seam EL resolver to the list of
 * standard resolvers.
 *
 * @since 5.7.3
 */
public class SeamExpressionResolver extends ExpressionResolver {

    public SeamExpressionResolver() {
        super();
        add(new SeamELResolver());
    }

}
