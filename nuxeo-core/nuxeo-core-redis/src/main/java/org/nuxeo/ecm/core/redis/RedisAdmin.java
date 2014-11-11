/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import java.io.IOException;

public interface RedisAdmin {

    String namespace(String... names);

    /**
     * Load script in redis
     *
     * @throws IOException
     *
     * @since 6.0
     */
    String load(String bundle, String name) throws IOException;


    /**
     * Clear keys in redis
     *
     * @throws IOException
     *
     * @since 6.0
     */
    public Long clear(String prefix) throws IOException;

}
