/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ws@nuxeo.com
 */


package key;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:ws@nuxeo.com">WS</a>
 *
 */
public interface KeyService {

    /**
     * retrieve existing keys
     *
     * @param userId
     * @return
     */
    public KeyPair getKeys(CertInfo certInfo) throws Exception;

    public KeyPair createKeys(CertInfo certInfo)
            throws NoSuchAlgorithmException;

    void storeKeys();

}
