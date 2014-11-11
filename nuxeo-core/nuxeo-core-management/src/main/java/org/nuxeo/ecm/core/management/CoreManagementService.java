/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.management;

/**
 * Services that wants to register with the core management for being
 * call-backed should implement that interface.
 * <p>
 * For being call-backed during document store initialization, services should
 * implement the StorageClient interface.
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public interface CoreManagementService {

}
