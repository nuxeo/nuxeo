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

package org.nuxeo.opensocial.container.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * ValuePair
 *
 * @author Guillaume Cusnieux
 */
public class ValuePair implements IsSerializable {

  private String value;
  private String displayValue;

  public ValuePair() {
  }

  public ValuePair(String value, String displayValue) {
    this.value = value;
    this.displayValue = displayValue;
  }

  public String getValue() {
    return value;
  }

  public String getDisplayValue() {
    return displayValue;
  }
}
