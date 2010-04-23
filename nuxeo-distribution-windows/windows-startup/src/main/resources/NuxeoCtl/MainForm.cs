
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
		private ServiceController nxService;
		private static String nxSvcName="NuxeoDM";
		private String nxSvcStatus;
		private System.Windows.Forms.Timer nxSvcTimer;
		private System.Windows.Forms.Timer nxAppTimer;
		
		// Logging to the logBox
		
		private delegate void LogHandler(String outLine);
		
		private void Log(String message) {
			Log(message,"INFO");
		}
		
		private void Log(String message, String loglevel) {
			if (logBox.InvokeRequired) {
				logBox.Invoke(new LogHandler(Log), new object[] {message});
			} else {
				Color color=Color.Black;
				if (loglevel=="INFO") color=Color.Black;
				else if (loglevel=="DEBUG") color=Color.Green;
				else if (loglevel=="WARN") color=Color.DarkBlue;
				else if (loglevel=="ERROR") color=Color.Red;
				else Log("NO SUCH LOGLEVEL :"+loglevel,"ERROR");
				logBox.SelectionStart=logBox.TextLength;
				logBox.SelectionColor=color;
				logBox.AppendText("["+loglevel+"] "+message+Environment.NewLine);
				logBox.SelectionStart=logBox.TextLength;
				logBox.ScrollToCaret();
			}
		}
		
		// Logging callbacks
		
		private void OutputLog(object sender, DataReceivedEventArgs outLine) {
			if (!String.IsNullOrEmpty(outLine.Data)) {
				Log(outLine.Data,"INFO");
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
		
		private void AppLog(object sender, LogEventArgs arg) {
			Log(arg.GetMessage(),arg.GetLogLevel());
		}
		
		// This function is called every 10s when Nuxeo is defined as a service.
		// It updates the start/stop buttons depending on service status.
		
		private void nxSvcDisplay() {
			nxService.Refresh();
			if (nxService.Status == ServiceControllerStatus.Stopped) {
				startButton.Enabled=true;
				stopButton.Enabled=false;
			}
			if (nxService.Status == ServiceControllerStatus.Running) {
				startButton.Enabled=false;
				stopButton.Enabled=true;
			}
		}
		
		private void nxSvcTimer_Elapsed(object sender, EventArgs e) {
			nxSvcDisplay();
		}
		
		// Same thing with the application version
		
		private void nxAppDisplay() {
			if (nxControl.running==false) {
				startButton.Enabled=true;
				stopButton.Enabled=false;
			} else {
				startButton.Enabled=false;
				stopButton.Enabled=true;
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
				EventLog log=new EventLog("System");
				log.EnableRaisingEvents=true;
				log.EntryWritten+=new EntryWrittenEventHandler(ServiceLog);
			} catch {
				nxService=null;
				Log(nxSvcName+" is not defined as a service","WARN");
				nxAppTimer=new System.Windows.Forms.Timer();
				nxAppTimer.Interval=5000;
				nxAppTimer.Tick+=new EventHandler(nxAppTimer_Elapsed);
				nxAppTimer.Start();
			}
						
			
		}
		
		
	}
	
	
}
