/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     "Stephane Lacoin at Nuxeo (aka matic)"
 */
package org.nuxeo.ecm.core.management.guards;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;

/**
 * Indicates that a method or class cannot be invoked if the specified
 * status is in passive state.
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Guarded {

    String id() default AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY;

}
