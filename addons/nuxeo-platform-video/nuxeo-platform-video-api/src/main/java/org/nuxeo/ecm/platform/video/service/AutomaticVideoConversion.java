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

package org.nuxeo.ecm.platform.video.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Object representing a registered automatic video conversion on the
 * {@link VideoService}.
 * <p>
 * An {@code AutomaticVideoConversion} references the {@code VideoConversion}
 * through its name.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("automaticVideoConversion")
public class AutomaticVideoConversion implements Cloneable,
        Comparable<AutomaticVideoConversion> {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("@order")
    private int order = 0;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public AutomaticVideoConversion clone() throws CloneNotSupportedException {
        return (AutomaticVideoConversion) super.clone();
    }

    @Override
    public int compareTo(AutomaticVideoConversion o) {
        int cmp = order - o.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(o.name);
        }
        return cmp;
    }

}
