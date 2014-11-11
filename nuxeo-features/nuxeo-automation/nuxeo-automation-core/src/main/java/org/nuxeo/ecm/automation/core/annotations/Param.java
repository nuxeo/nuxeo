/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.automation.OperationParameters;

/**
 * To be used on an operation field to inject operation parameters from the
 * current context. If the parameter to inject cannot be found in the operation
 * parameters map (or it is set to null) then if required is true then an error
 * is thrown otherwise the injection will not be done (and any default value
 * set in the code will be preserved). The default is true - i.e. do not allow
 * missing entries in operation parameter map.
 *
 * @see OperationParameters
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Param {

    /**
     * The parameter key in the operation parameters map.
     */
    String name();

    /**
     * If the parameter to inject cannot be found in the operation parameters
     * map (or it is set to null) then if required is true then an error is
     * thrown otherwise the injection will not be done (and any default value
     * set in the code will be preserved). The default is true - i.e. do not
     * allow missing entries in operation parameter map.
     */
    boolean required() default true;

    /**
     * Optional attribute - useful to generate operation documentation. Provide
     * a widget type to be used by the UI to edit this parameter. If no widget
     * is provided the default mechanism is to choose a widget compatible with
     * the parameter type. For example if the parameter has the type String the
     * default is to use a TextBox but you can override this by specifying a
     * custom widget type like ListBox, TextArea etc.
     */
    String widget() default "";

    /**
     * Optional attribute - useful to generate operation documentation. Provide
     * default values for the parameter widget. If the parameter is rendered
     * using a ListBox or ComboBox then this attribute can be used to hold the
     * choices available in the list. If the widget is not a list then this can
     * be used to specify the default value for the widget.
     */
    String[] values() default {};

    /**
     * Optional attribute to set a parameter order, used for ordering them when
     * presenting the UI form to fill.
     */
    int order() default 0;
}
