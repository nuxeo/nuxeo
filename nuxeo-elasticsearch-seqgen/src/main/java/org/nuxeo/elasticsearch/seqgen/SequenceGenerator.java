/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Tiry
 * 
 */

package org.nuxeo.elasticsearch.seqgen;

/**
 * Interface for managing abstracted sequences 
 * 
 * @author tiry
 *
 */
public interface SequenceGenerator {

    /**
     * For the given sequence name, increment it and return the value
     *  
     * @param sequenceName the name of the sequence
     * @return the new value of the sequence after increment
     */
    long getNextId(String sequenceName);
    
}
