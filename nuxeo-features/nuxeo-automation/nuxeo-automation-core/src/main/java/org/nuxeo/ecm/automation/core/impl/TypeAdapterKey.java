/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class TypeAdapterKey {

    public final Class<?> input;
    public final Class<?> output;
    private int hashCode;
    
    public TypeAdapterKey(Class<?> input, Class<?> output) {
        this.input = input;
        this.output = output;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // this class is final - do not need instanceof check.
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == TypeAdapterKey.class) { 
            TypeAdapterKey key = (TypeAdapterKey)obj;
            return key.input == input && key.output == output;
        }
        return false;
    }
    
    
    @Override
    public String toString() {
        return input+":"+output;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = createHashCode();
        }
        return hashCode;
    }

    protected int createHashCode() {
        int result = input.hashCode() | output.hashCode();
        return result == 0 ? 0xbabe : result;
    }

}
