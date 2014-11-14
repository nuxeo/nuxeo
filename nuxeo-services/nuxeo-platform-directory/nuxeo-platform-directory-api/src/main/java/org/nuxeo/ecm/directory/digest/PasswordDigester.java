/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.directory.digest;

import java.util.Map;

/**
 *
 *
 * @since 7.1
 */
public interface PasswordDigester {


    public String getName();

    public void setName(String name);

    public String hashPassword(String password) throws UnknownAlgorithmException;

    void setParams(Map<String, String> params);

    public boolean verifyPassword(String clearPassword, String hashedPassword);
}
