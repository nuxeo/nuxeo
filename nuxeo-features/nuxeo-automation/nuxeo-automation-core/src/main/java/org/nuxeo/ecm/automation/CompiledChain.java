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

/**
 * A compiled operation chain. The chain is immutable (cannot be modified after
 * it was compiled). This is a self contained object - once built it can be
 * used at any time to invoke the operations in the chain. Not that the chain
 * must be executed on a context compatible with the one used for compiling
 * (which provides the same input type)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface CompiledChain {

    Object invoke(OperationContext ctx) throws OperationException;

}
