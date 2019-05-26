/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.ui.web.component.html;


import javax.el.ValueExpression;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;

/**
 * Overriding the default {@code HtmlInputText} to handle HTML5 attributes.
 *
 * @since 9.1
 */
public class NXHtmlInputText extends HtmlInputText {

    public NXHtmlInputText() {
        super();
    }

    private String placeholder;

    public String getPlaceholder() {
        if (null != this.placeholder) {
            return this.placeholder;
        }
        ValueExpression _ve = getValueExpression("placeholder");
        if (_ve != null) {
            return (String) _ve.getValue(getFacesContext().getELContext());
        } else {
            return null;
        }
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    private Object[] _values;

    @Override
    public Object saveState(FacesContext _context) {
        if (_values == null) {
            _values = new Object[2];
        }
        _values[0] = super.saveState(_context);
        _values[1] = placeholder;
        return _values;
    }

    @Override
    public void restoreState(FacesContext _context, Object _state) {
        _values = (Object[]) _state;
        super.restoreState(_context, _values[0]);
        this.placeholder= (java.lang.String) _values[1];
    }

}
