/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * If set to false, the binding will not be called to get the value (and use it to preserve it in the redirect URL
     * after a POST)
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
