/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.impl.jsongraph;

import java.util.Locale;

/**
 * @since 7.2
 */
public class TransitionView {

    public String nodeSourceId;

    public String nodeTargetId;

    public String label;

    public TransitionView(String nodeSourceId, String nodeTargetId, String label, Locale locale) {
        this.nodeSourceId = nodeSourceId;
        this.nodeTargetId = nodeTargetId;
        this.label = JsonGraphRoute.getI18nLabel(label, locale);
    }

    public String getLabel() {
        return label;
    }

    public String getNodeSourceId() {
        return nodeSourceId;
    }

    public String getNodeTargetId() {
        return nodeTargetId;
    }

}