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
 *     Mike Obrebski <mobrebski@nuxeo.com>
 */

function ImageRenderer(instance, td, row, col, prop, value, cellProperties) {

	var escaped = Handsontable.helper.stringify(value), img;

	if (escaped.indexOf('http') === 0) {
		img = document.createElement('IMG');
		img.src = value;
		img.setAttribute('width', '280px');	

		Handsontable.Dom.empty(td);
		td.appendChild(img);
	}	else {
		// render as text
		Handsontable.renderers.TextRenderer.apply(this, arguments);
	}
	return td;
}

export
{
	ImageRenderer
};