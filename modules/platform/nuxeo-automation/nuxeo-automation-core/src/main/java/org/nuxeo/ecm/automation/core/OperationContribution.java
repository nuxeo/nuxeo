/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
@XObject("operation")
public class OperationContribution {

    /**
     * The operation class that must be annotated using {@link Operation} annotation.
     */
    @XNode("@class")
    public String type;

    /**
     * Put it to true to override an existing contribution having the same ID. By default overriding is not permitted
     * and an exception is thrown when this flag is on false.
     */
    @XNode("@replace")
    public boolean replace;

    /**
     * The widget descriptor for the operation parameters.
     *
     * @since 5.9.5
     */
    @XNodeList(componentType = WidgetDescriptor.class, type = ArrayList.class, value = "widgets/widget")
    public List<WidgetDescriptor> widgets;

}
