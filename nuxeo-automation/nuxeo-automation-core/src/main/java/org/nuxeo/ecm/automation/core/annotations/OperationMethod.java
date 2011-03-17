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

import org.nuxeo.ecm.automation.OutputCollector;

/**
 * To be used to mark methods provided by an operation. A method must have at
 * most one argument which is the operation input and a return type which is
 * the operation output. Methods with zero parameters (void input) will match
 * any input. An operation may have multiple methods if it supports multiple
 * input and output types.
 * <p>
 * For each INPUT/OUTPUT type association you must create a new method in an
 * operation and annotate it using this annotation. The set of input types
 * available in an operation are the operation accepted types and the set of
 * output types are the operation produced types. A produced type will become
 * the input type for the next operation in the chain, that will be dispatched
 * to the right method that know how to consume the type.
 * <p>
 * When an operation provides 2 methods accepting the same input type the chain
 * will need to take a decision to determine the best way to continue. A common
 * algorithm to find the right path until the end of the chain is backtracking:
 * from a set of potential nodes one is selected to be visited (randomly or
 * not). If the node is a dead end then another node from the set is taken
 * until the path to the last node is created.
 * <p>
 * A chain may have no paths until the last operation. In this case the chain
 * is invalid and the chain processor will trigger an error. Also, a chain can
 * provide multiple paths to the last operation. To help the engine to find the
 * best path until the last operation you can use the {@link #priority()}
 * attribute to specify which method is preferred. The default priority is 0
 * (e.g. no priority). Higher priorities have more chance to be selected when a
 * conflict occurs. If no user priority is specified (i.e. priority is 0) then
 * the default priority is used. Here is how the default priority is computed
 * (the top most case has the greater priority):
 * <ul>
 * <li> The input type is an exact match of the method declared argument
 * <li> The method argument type is assignable from the input type (i.e. a
 * super type but not an exact match)
 * <li> The input can be adapted to the method argument using registered type
 * adapters
 * <li> the method has no arguments (void input)
 * </ul>
 * If no one of these rules applies then the method will not match the input.
 * <p>
 * The class owning the annotated method must be annotated using
 * {@link Operation}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OperationMethod {

    /**
     * If defined the method is iterable.
     * <p>
     * It means that when such a method is called with an input type of <code>Iterable<INPUT></code>
     * (where INPUT is the declared method input type) the method will be iteratively called
     * to generate all the outputs and collect them using the given OutputCollector.
     * @return
     */
    @SuppressWarnings("rawtypes")
    Class<? extends OutputCollector> collector() default OutputCollector.class;

    int priority() default 0;

}
