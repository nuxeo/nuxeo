/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *     Vilogia - Mail address formatting
 *
 */

package org.nuxeo.ecm.platform.mail.web.converter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import static java.util.regex.Pattern.*;

/**
 * Simple mail address converter: most of the addresses imported from the POP3
 * or IMAP mailbox simply return the string "null <mail.address@domain.org>". To
 * avoid a list of nulls this converter removes the "null" aliases and only keep
 * the mail address. Also return a mailto: link to the sender.
 *
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 */
public class MailAddressConverter implements Converter {

    private static final String EMAIL_REGEXP = "(.*)<([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4})>";

    private static final Pattern pattern = compile(EMAIL_REGEXP, CASE_INSENSITIVE);

    public Object getAsObject(FacesContext ctx, UIComponent uiComp, String inStr) {
        return inStr;
    }

    public String getAsString(FacesContext ctx, UIComponent uiComp, Object inObj) {
        if (null == inObj) {
            return null;
        }

        if (inObj instanceof String) {
            String inStr = (String) inObj;
            Matcher m = pattern.matcher(inStr);

            if (m.matches()) {
                String alias = m.group(1);
                String email = m.group(2);

                if (alias.trim().toLowerCase().equals("null")) {
                    alias = email;
                }

                return String.format("<a href=\"mailto:%s\">%s</a>",
                        email, alias);

            } else {
                return inStr;
            }
        } else {
            return inObj.toString();
        }
    }

}
