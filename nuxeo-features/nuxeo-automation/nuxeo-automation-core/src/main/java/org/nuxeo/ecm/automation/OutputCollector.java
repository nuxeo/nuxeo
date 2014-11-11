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
package org.nuxeo.ecm.automation;

import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

/**
 * This interface is used to implement result collectors when an operation
 * method is invoked over an iterable input.
 * <p>
 * The operation method usually declare a scalar type (e.g. data object) as
 * argument. When the operation method is invoked on an iterator over elements
 * of that type the execution of the chain may help you deal with this by
 * automatically invoking your method over every element in the input iterator.
 * For this to work you should set the collector that should be used by the
 * automatic iteration {@link OperationMethod#collector()} in order to construct
 * the method output.
 * <p>
 * So a collector is in fact collecting the result of each individual invocation
 * over a collections of inputs. The collector will be asked by the chain
 * execution to add an individual result by calling
 * {@link #add(OperationContext, Object)}. This method is taking as argument the
 * return value of the invocation - so it must accept the same type of object -
 * see T generic type. When all partial results are collected the collector will
 * be asked to return the operation result through the {@link #getOutput()}
 * method.
 * <p>
 * So when writing a collector you <b>must</b> ensure that the collected type is
 * compatible with the one returned by the operation method where the collector
 * is used.
 * <p>
 * <b>IMPORTANT<> An implementation of this class must explicitly implements
 * this interface (and not through its super classes). This is to ease generic
 * type detections. If not doing so your collector class will be rejected and
 * the operation using it invalid.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface OutputCollector<T, R> {

    /**
     * Collects a new partial result (the result of the last iteration step).
     */
    void collect(OperationContext ctx, T obj) throws OperationException;

    /**
     * Gets the final output. This is usually a list or set of collected objects.
     */
    R getOutput();

}
