/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.jca;

import java.io.Serializable;
import java.util.Map;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * This class encapsulates the credentials for creating a
 * session from the repository.
 * <p>
 * These sources are based on the JackRabbit JCA implementation (http://jackrabbit.apache.org/)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class JCAConnectionRequestInfo
        implements ConnectionRequestInfo {

    /**
     * Credentials.
     */
    private final Map<String, Serializable> sessionContext;


    /**
     * Construct a copy of the request info from a given request info instance.
     * @param cri the connection request info to copy
     */
    public JCAConnectionRequestInfo(JCAConnectionRequestInfo cri) {
        this(cri.sessionContext);
    }

    /**
     * Construct the request info.
     * @param context the session context
     */
    public JCAConnectionRequestInfo(Map<String, Serializable> context) {
        sessionContext = context;
    }


    /**
     * Return the credentials.
     */
    public Map<String, Serializable> getSessionContext() {
        return sessionContext;
    }

    /**
     * Return true if equals.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof JCAConnectionRequestInfo)) {
            return false;
        }
        JCAConnectionRequestInfo other = (JCAConnectionRequestInfo) o;
        if (sessionContext == null) {
            return other.sessionContext == null;
        } else {
            return sessionContext.equals(other.sessionContext);
        }
    }

    @Override
    public int hashCode() {
        return sessionContext == null ? 0 : sessionContext.hashCode();
    }

}
