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
*     Mathieu Guillaume, Arnaud Kervern, Lise Kemen, Julien Carsique
*/

using System;
using System.Collections.Generic;
using System.ServiceProcess;
using System.Drawing;
using System.Windows.Forms;
using System.Diagnostics;
using NuxeoProcess;

namespace NuxeoCtl
{
	/// <summary>
	/// Description of MainForm.
	/// </summary>
	public partial class MainForm : Form
	{
        private bool CliUse { get; set; }
        private String arg;
        public String Arg
        {
            get
            {
                return arg;
            }
            set
            {
                arg = value;
                CliUse = true;
            }
        }

		private ServiceController nxService;
		private static String nxSvcName="Nuxeo";
		private String nxSvcStatus;
		private System.Windows.Forms.Timer nxSvcTimer;
		private System.Windows.Forms.Timer nxAppTimer;
		
		// Logging to the logBox
		
		private delegate void LogHandler(String outLine, String loglevel);
		
		private void Log(String message) {
			Log(message,"INFO");
		}
		
		private static char[] splitParams={' '};
		
		private void Log(String message, String loglevel) {
			if (logBox.InvokeRequired) {
				logBox.Invoke(new LogHandler(Log), new object[] {message,loglevel});
			} else {
				Color color=Color.FromArgb(234,234,234);
				if (loglevel=="INFO") color=Color.FromArgb(234,234,234);
				else if (loglevel=="DEBUG") color=Color.FromArgb(108,183,242);
				else if (loglevel=="WARN") color=Color.FromArgb(234,138,2);
				else if (loglevel=="ERROR") color=Color.FromArgb(245,0,63);
				else if (loglevel=="LOG") {
					String[] split=message.Split(splitParams,3);
					if (split.Length==3) {
						if (split[1]=="INFO") color=Color.FromArgb(234,234,234);
						else if (split[1]=="DEBUG") color=Color.FromArgb(108,183,242);
						else if (split[1]=="WARN") color=Color.FromArgb(234,138,2);
						else if (split[1]=="ERROR") color=Color.FromArgb(245,0,63);
						else color=Color.FromArgb(234,234,234);
					} else {
						color=Color.FromArgb(234,234,234);
					}
				}
				else Log("NO SUCH LOGLEVEL :"+loglevel,"ERROR");
				logBox.SelectionStart=logBox.TextLength;
				logBox.SelectionColor=color;
				if (loglevel != "LOG") {
					logBox.AppendText("["+loglevel+"] "+message+Environment.NewLine);
				}
				else {
					logBox.AppendText(message+Environment.NewLine);
				}
				logBox.SelectionStart=logBox.TextLength;
				logBox.ScrollToCaret();
			}
		}
		
		// Logging callbacks
		
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
		
		private void ServiceLog(object sender, EntryWrittenEventArgs entryArg) {
			if (entryArg.Entry.Source.ToString()!=nxSvcName) return;
			if (entryArg.Entry.EntryType==EventLogEntryType.Error) {
				Log(entryArg.Entry.Message,"ERROR");
			} else {
				Log(entryArg.Entry.Message,"INFO");
			}
		}
		
		private void nxControllerLog(object sender, LogEventArgs arg) {
			Log(arg.GetMessage(),arg.GetLogLevel());
		}
		
		// This function is called every 10s when Nuxeo is defined as a service.
		// It updates the start/stop buttons depending on service status.
		
		private void nxSvcDisplay() {
			nxService.Refresh();
			if (nxService.Status == ServiceControllerStatus.Stopped) {
				startButton.Enabled=true;
				startButton.Show();
				stopButton.Enabled=false;
				stopButton.Hide();
				terminateButton.Enabled=false;
				terminateButton.Hide();
			}
			if (nxService.Status == ServiceControllerStatus.Running) {
				startButton.Enabled=false;
				startButton.Hide();
				stopButton.Enabled=true;
				stopButton.Show();
				terminateButton.Enabled=true;
				terminateButton.Show();
			}
		}
		
		private void nxSvcTimer_Elapsed(object sender, EventArgs e) {
			nxSvcDisplay();
		}
		
		// Same thing with the application version
		
		private void nxAppDisplay() {
			if (nxControl==null) return;
			if (nxControl.running==false) {
				startButton.Enabled=true;
				startButton.Show();
				stopButton.Enabled=false;
				stopButton.Hide();
				terminateButton.Enabled=false;
				terminateButton.Hide();
			} else {
				startButton.Enabled=false;
				startButton.Hide();
				stopButton.Enabled=true;
				stopButton.Show();
			}
		}
		private void nxAppTimer_Elapsed(object sender, EventArgs e) {
			nxAppDisplay();
		}
		
		// Main
		
		public MainForm()
		{
			//
			// The InitializeComponent() call is required for Windows Forms designer support.
			//
			InitializeComponent();
			
			nxSvcName = NuxeoController.ProductName;
			
			// Check whether Nuxeo is installed as a service or standalone.
			nxService = new ServiceController(nxSvcName);
			try {
				nxService.Refresh();
				nxSvcStatus=nxService.Status.ToString();
				Log("Service status: "+nxSvcStatus,"WARN");
				// Status updates : timer + callback
				nxSvcTimer=new System.Windows.Forms.Timer();
				nxSvcTimer.Interval=5000;
				nxSvcTimer.Tick+=new EventHandler(nxSvcTimer_Elapsed);
				nxSvcTimer.Start();
				nxSvcDisplay();
				// Logging
				EventLog log=new EventLog("Application");
				log.EnableRaisingEvents=true;
				log.EntryWritten+=new EntryWrittenEventHandler(ServiceLog);
			} catch {
				nxService=null;
				Log(nxSvcName + " is not defined as a service", "WARN");
				nxAppTimer=new System.Windows.Forms.Timer();
				nxAppTimer.Interval=5000;
				nxAppTimer.Tick+=new EventHandler(nxAppTimer_Elapsed);
				nxAppTimer.Start();
			}
		}

        private void MainForm_Shown(object sender, EventArgs e)
        {
            if (CliUse)
            {
                switch (Arg)
                {
                    case "start":
                        StartClick(this, new EventArgs());
                        break;
                    case "stop":
                        StopClick(this, new EventArgs());
                        Environment.Exit(0);
                        this.Close();
                        break;
                    default:
                        Log(String.Format("Invalid argument \"{0}\"", Arg), "ERROR");
                        break;
                }
            }
        }
		
		
		void MainFormFormClosing(object sender, FormClosingEventArgs e)
		{
			if (!(CliUse || nxService != null)) {
				if (!startButton.Visible) {
					DialogResult res = MessageBox.Show("Are you sure to exit ? Nuxeo will be shut down.", 
						"Warning", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
					e.Cancel = res == DialogResult.No;
				}
				
				try {
					if (!e.Cancel) {
						StopClick(this, new EventArgs());
					}
				} catch {}
			}
		}
	}
	
	
}
