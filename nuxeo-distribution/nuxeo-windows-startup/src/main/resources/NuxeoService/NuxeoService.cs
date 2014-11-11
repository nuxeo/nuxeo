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
using System.Data;
using System.Diagnostics;
using System.ServiceProcess;
using System.Text;

using NuxeoProcess;

namespace NuxeoService
{
	public class NuxeoService : ServiceBase
	{
		public const string MyServiceName = "Nuxeo";
		private NuxeoController nxControl=null;
		private EventLog log=new EventLog("Application");
		
		public NuxeoService()
		{
			InitializeComponent();
		}
		
		private void InitializeComponent()
		{
			this.ServiceName = MyServiceName;
		}
		
		// Logging callbacks
		
		private void Log(String message, String loglevel) {
			log.WriteEntry(message,EventLogEntryType.Information);
		}
		
		private void OutputLog(object sender, DataReceivedEventArgs outLine) {
			if (!String.IsNullOrEmpty(outLine.Data)) {
				Log(outLine.Data,"LOG");
			}
		}
		
		private void ErrorLog(object sender, DataReceivedEventArgs errLine) {
			if (!String.IsNullOrEmpty(errLine.Data)) {
				Log(errLine.Data,"ERROR");
			}
		}
		
		private void nxControllerLog(object sender, LogEventArgs arg) {
			Log(arg.GetMessage(),arg.GetLogLevel());
		}
		
		/// <summary>
		/// Clean up any resources being used.
		/// </summary>
		protected override void Dispose(bool disposing)
		{
			base.Dispose(disposing);
		}
		
		/// <summary>
		/// Start this service.
		/// </summary>
		protected override void OnStart(string[] args)
		{
			log.Source=MyServiceName;
			if (nxControl==null) nxControl=new NuxeoProcess.NuxeoController();
			if (!nxControl.IsLogDelegated()) {
				nxControl.DelegatedLog+=new LogEventHandler(nxControllerLog);
				nxControl.SetDelegateLog(true);
			}
			if (nxControl.Start()==false) {
				Log("Could not start the application","ERROR");
				return;
			}
			Process nxProcess=nxControl.getProcess();
			nxProcess.OutputDataReceived+=new DataReceivedEventHandler(OutputLog);
			nxProcess.BeginOutputReadLine();
			nxProcess.ErrorDataReceived+=new DataReceivedEventHandler(ErrorLog);
			nxProcess.BeginErrorReadLine();
		}
		
		/// <summary>
		/// Stop this service.
		/// </summary>
		protected override void OnStop()
		{
			if (nxControl!=null) nxControl.Stop();
		}
	}
}
