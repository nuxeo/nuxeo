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
*     Mathieu Guillaume, Arnaud Kervern, Julien Carsique
*/

using System;
using System.Collections.Generic;
using System.ServiceProcess;
using System.Text;

namespace NuxeoService
{
	static class Program
	{
		/// <summary>
		/// This method starts the service.
		/// </summary>
		static void Main()
		{
			// To run more than one service you have to add them here
			ServiceBase.Run(new ServiceBase[] { new NuxeoService() });
		}
	}
}
