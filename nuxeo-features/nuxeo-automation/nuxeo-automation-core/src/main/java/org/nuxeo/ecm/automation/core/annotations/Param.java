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
package org.nuxeo.ecm.automation.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.automation.OperationParameters;

/**
 * To be used on an operation field to inject operation parameters from the current context. If the parameter to inject
 * cannot be found in the operation parameters map (or it is set to null) then if required is true then an error is
 * thrown otherwise the injection will not be done (and any default value set in the code will be preserved). The
 * default is true - i.e. do not allow missing entries in operation parameter map.
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
     * @since 5.7.3 The parameter description to explicit its purpose.
     */
    String description() default "";

    /**
     * If the parameter to inject cannot be found in the operation parameters map (or it is set to null) then if
     * required is true then an error is thrown otherwise the injection will not be done (and any default value set in
     * the code will be preserved). The default is true - i.e. do not allow missing entries in operation parameter map.
     */
    boolean required() default true;

    /**
     * Optional attribute - useful to generate operation documentation. Provide a widget type to be used by the UI to
     * edit this parameter. If no widget is provided the default mechanism is to choose a widget compatible with the
     * parameter type. For example if the parameter has the type String the default is to use a TextBox but you can
     * override this by specifying a custom widget type like ListBox, TextArea etc.
     */
    String widget() default "";

    /**
     * Optional attribute - useful to generate operation documentation. Provide default values for the parameter widget.
     * If the parameter is rendered using a ListBox or ComboBox then this attribute can be used to hold the choices
     * available in the list. If the widget is not a list then this can be used to specify the default value for the
     * widget.
     */
    String[] values() default {};

    /**
     * Optional attribute to set a parameter order, used for ordering them when presenting the UI form to fill.
     */
    int order() default 0;

    /**
     * Optional alias for the parameter key. If the operation parameters map does not contain the name, then alias will
     * be used if any.
     *
     * @since 5.9.2
     */
    String[] alias() default {};
}
