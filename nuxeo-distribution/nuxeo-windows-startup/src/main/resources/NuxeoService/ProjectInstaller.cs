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
using System.ComponentModel;
using System.Configuration.Install;
using System.ServiceProcess;

namespace NuxeoService
{
	[RunInstaller(true)]
	public class ProjectInstaller : Installer
	{
		private ServiceProcessInstaller serviceProcessInstaller;
		private ServiceInstaller serviceInstaller;
		
		public ProjectInstaller()
		{
			serviceProcessInstaller = new ServiceProcessInstaller();
			serviceInstaller = new ServiceInstaller();
			// Here you can set properties on serviceProcessInstaller or register event handlers
			serviceProcessInstaller.Account = ServiceAccount.LocalService;
			
			this.Installers.AddRange(new Installer[] { serviceProcessInstaller, serviceInstaller });
		}
		
		protected override void OnBeforeUninstall(System.Collections.IDictionary savedState)
		{
			base.OnBeforeUninstall(savedState);
			
			String serviceName = GetContextParameter("servicename");
			
			if (String.IsNullOrEmpty(serviceName)) {
				serviceInstaller.ServiceName = NuxeoService.MyServiceName;
			} else {
				serviceInstaller.ServiceName = serviceName;
			}
		}
		
		protected override void OnBeforeInstall(System.Collections.IDictionary savedState)
		{
			base.OnBeforeInstall(savedState);
			
			String serviceName = GetContextParameter("servicename");
			String startType = GetContextParameter("starttype");
			
			if (String.IsNullOrEmpty(serviceName)) {
				serviceInstaller.ServiceName = NuxeoService.MyServiceName;
			} else {
				serviceInstaller.ServiceName = serviceName;
			}
			
			if (!String.IsNullOrEmpty(startType)) {
				serviceInstaller.StartType = startType == "automatic" ? 
					ServiceStartMode.Automatic : ServiceStartMode.Manual;
			}
		}
		
		private string GetContextParameter(string key)
        {
            string sValue = "";
            try
            {
  				sValue = this.Context.Parameters[key].ToString();
            }
            catch
            {
                sValue = "";
            }
            return sValue.Trim();
		}
	}
}
