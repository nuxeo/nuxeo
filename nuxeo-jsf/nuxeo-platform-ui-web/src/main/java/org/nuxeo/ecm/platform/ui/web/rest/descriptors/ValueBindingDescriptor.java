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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: ValueBindingDescriptor.java 21462 2007-06-26 21:16:36Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for a value binding.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("binding")
public class ValueBindingDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("")
    protected String expression;

    /**
     * If set to false, the binding will not be called to get the value (and
     * use it to preserve it in the redirect URL after a POST)
     *
     * @since 5.4.2
     */
    @XNode("@callGetter")
    protected boolean callGetter = true;

    /**
     * If set to false, the binding will not be called to set the value
     *
     * @since 5.4.2
     */
    @XNode("@callSetter")
    protected boolean callSetter = true;

    public String getExpression() {
        return expression;
    }

    public String getName() {
        return name;
    }

    public boolean getCallGetter() {
        return callGetter;
    }

    public boolean getCallSetter() {
        return callSetter;
    }

}
