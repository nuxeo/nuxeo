/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.shindig.crypto;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("portalConfig")
public class PortalConfig {

    /**
     * This field ends up being the "name" of the container. This defaults to
     * "default" and should not be changed by most users. The only situation
     * where this would need to be changed is a situation in which there are
     * multiple containers inside nuxeo and this is not advised.
     */
    @XNode("containerName")
    private String containerName;

    /**
     * This field ends up being the "domain" of the container. This defaults to
     * "localhost" and should not be changed by most users.
     */
    @XNode("domain")
    private String domain;

    /**
     * This field is the key that is used by shindig to communicate with itself.
     * For example, sometimes the interpretation of a gadget results in a call
     * to the "make request" servlet for access to external resources. This
     * symmetric key is used to sign the message going from shindig to shindig
     * to verify that the message receivied by the make request servlet is not
     * "forged".
     *
     * This value can and, in most cases should, be left empty. When it is left
     * empty, the system will use a random set of bytes for this key.
     */
    @XNode("key")
    private String key;

    public String getDomain() {
        return domain;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getKey() {
        return key;
    }

}
