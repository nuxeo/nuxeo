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
 *     mobrebski
 *     Jackie Aldama <jaldama@nuxeo.com>
 */

const ImageRenderer = (instance, td, row, col, prop, value, cellProperties) => {
	if (value && value.data) {
		var img = document.createElement('img');
		img.src = value.data;
    if (cellProperties.width) { img.setAttribute('width', cellProperties.height); }
    if (cellProperties.height) { img.setAttribute('height', cellProperties.height); }
		Handsontable.Dom.empty(td);
		td.appendChild(img);
	}
	return td;
};

export {ImageRenderer};
