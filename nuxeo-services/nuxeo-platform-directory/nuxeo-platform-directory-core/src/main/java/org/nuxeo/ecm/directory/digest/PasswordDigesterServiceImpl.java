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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 *
 * @since 7.1
 */
public class PasswordDigesterServiceImpl extends DefaultComponent implements PasswordDigesterService {

    private static final Pattern HASH_PATTERN = Pattern.compile("^\\{(.*)\\}(.*)$");

    private static final String DIGESTER_XP_NAME = "digester";
    Map<String, PasswordDigester> digesters = new ConcurrentHashMap<>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if(DIGESTER_XP_NAME.equals(extensionPoint)) {
            PasswordDigesterDescriptor pdd = (PasswordDigesterDescriptor) contribution;


            if(pdd.enabled) {
                digesters.put(pdd.name, pdd.buildDigester());
            } else if(digesters.containsKey(pdd.name)) {
                digesters.remove(pdd.name);
            }

        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if(DIGESTER_XP_NAME.equals(extensionPoint)) {
            PasswordDigesterDescriptor pdd = (PasswordDigesterDescriptor) contribution;
            if(digesters.containsKey(pdd.name)) {
                digesters.remove(pdd.name);
            }
        }
    }

    @Override
    public PasswordDigester getPasswordDigester(String name)
            throws UnknownAlgorithmException {
       if(digesters.containsKey(name)) {
           return digesters.get(name);
       } else {
           throw new UnknownAlgorithmException();
       }
    }


    /**
     * @param hashedPassword
     * @return
     *
     */
    @Override
    public String getDigesterNameFromHash(String hashedPassword) {
        Matcher m = HASH_PATTERN.matcher(hashedPassword);
        try {
            return m.matches() ? m.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


}
