/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

/**
 * The versioning options that can be requested when saving a document, or when
 * doing a check in.
 */
public enum VersioningOption {
    /**
     * No versioning requested.
     */
    NONE,
    /**
     * Minor versioning requested.
     */
    MINOR,
    /**
     * Major versioning requested.
     */
    MAJOR;
}
