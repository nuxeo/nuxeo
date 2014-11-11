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
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Text.RegularExpressions;
using System.Threading;


// Debugging note :
// All the "Console.WriteLine" occurences will only work if
// a console is defined in the calling process (AllocConsole).

namespace NuxeoProcess
{
	// Define log events for non-console logging
	
	public class LogEventArgs : EventArgs
	{
		private String message;
		private String loglevel;
		
		public LogEventArgs(String m) {
			this.message=m;
			this.loglevel="INFO";
		}
		
		public LogEventArgs(String m, String l) {
			this.message=m;
			this.loglevel=l;
		}
		
		public String GetMessage() {
			return this.message;
		}
		
		public String GetLogLevel() {
			return this.loglevel;
		}
	}
	
	public delegate void LogEventHandler(object sender, LogEventArgs e);

	public delegate void ProcessStartedHandler(object sender, EventArgs e);
	
	/// <summary>
	/// This class starts and stop a Nuxeo server in the background.
	/// It can be used both by the service and the Windows application.
	/// </summary>
	public partial class NuxeoController
	{
        public bool Initialized { get; private set; }
        public bool Stoppable { get; set; }

        private String platform = null;
        private String productName ="nuxeo";
        private static Process confProcess = null;
        private static Process nxProcess = null;
        private static Process stopProcess = null;
        public Dictionary<String, String> nxEnv;
        private String confArgs = null;
        private String startArgs = null;
        private String stopArgs = null;
        public bool running = false;
        public bool Configured { get; private set; }
        private bool EnvIsSetUp = false;
        private bool countActive = false;
		private int countStatus = 0;
        private bool memoryAllocated = false;
		
		// Constructor
		
		public NuxeoController() {
			// Detect platform
			int p = (int) Environment.OSVersion.Platform;
            if ((p == 4) || (p == 6) || (p == 128))
            {
                platform = "unix";
            }
            else
            {
                platform = "windows";
            }
            
            productName = ProductName;
            Log("Registered product name : " + productName);
		}
		
		public static String ProductName { 
			get {
				String location = Assembly.GetEntryAssembly().Location;
				
	            // Try to specify which product we're running
	            String PRODUCT_NAME_FILE = Path.Combine(Directory.GetParent(location).FullName, "ProductName.txt");
	            
	            if (File.Exists(PRODUCT_NAME_FILE)) {
	            	using(StreamReader file = new StreamReader(PRODUCT_NAME_FILE)) {
	            		String line=file.ReadLine();
	            		if (line.Length != 0) 
	            			return line;
	            	}
	            }
	            return "nuxeo";
			}
		}

        public bool Initialize() {
            bool init = InitializeController();
            Initialized = init;
            return init;
        }

        private bool InitializeController()
        {
            // Return if The process is already started
            if (nxProcess != null) return false;

            return true;
        }
		
		// Utility : log
		// If delegateLog is true, this will generate a LogEvent
		// instead of writing to the console.
		
		private bool delegateLog=false;
		public void SetDelegateLog(bool sdl) {
			delegateLog=sdl;
		}
		
		public bool IsLogDelegated() {
			return delegateLog;
		}
		
		public event LogEventHandler DelegatedLog;
		
		public event ProcessStartedHandler ProcessStarted;
		
		private void Log(String message) {
			Log(message,"INFO");
		}
		
		private void Log(String message, String level) {
			if (delegateLog==true) {
				LogEventArgs arg=new LogEventArgs(message,level);
				LogEventHandler handler=DelegatedLog;
				handler(this,arg);
			} else {
				Console.WriteLine("["+level+"] "+message);
			}
		}
		
		
		// Callback on process exit
		
		private void Process_Exited(object sender, EventArgs e) {
			Log("Application has exited.","WARN");
			confProcess=null;
			nxProcess=null;
			stopProcess=null;
			nxEnv=new Dictionary<String,String>();
			EnvIsSetUp=false;
			running=false;
		}
		
		// ********** STARTUP **********
		
		public bool SetupEnv() {
            try
            {
                // Parse Nuxeo configuration file
                Dictionary<String, String> nxConfig = new Dictionary<string, string>();
                if ((nxConfig = ParseConfig()) == null)
                {
                    Log("Could not parse nuxeo.conf", "ERROR");
                    return false;
                }

                // Set up environment (nxEnv)
                if (SetupEnv(nxConfig) == false)
                {
                    Log("Could not set up environment", "ERROR");
                    return false;
                }

                // Setup up application server paths
                if (SetupApplicationServer() == false)
                {
                    Log("Could not set up the application server", "ERROR");
                    return false;
                }

                return true;
            }
            catch (Exception e)
            {
                Log(String.Format("{0} : {1}", e.Message, e.StackTrace), "ERROR");
                return false;
            }
		}
		
		public bool Configure() {
			if (!SetupEnv()) {
				return false;
			}
			
            // Run
			confProcess=new Process();
			confProcess.StartInfo.FileName=nxEnv["JAVA"];
			confProcess.StartInfo.Arguments=confArgs;
			confProcess.StartInfo.UseShellExecute=false;
			confProcess.StartInfo.CreateNoWindow=true;
			confProcess.StartInfo.RedirectStandardError=true;
			confProcess.StartInfo.RedirectStandardOutput=true;
			confProcess.EnableRaisingEvents=true;
			confProcess.Start();
			return true;
		}

		public bool Start() {
            if (!SetupEnv())
            {
                Log("Setup env isn't completed", "ERROR");
                return false;
            }

            if (!testMemoryAllocation())
            {
                Log("Could not reserve suffisant memory, use a different PermSize value", "ERROR");
                return false;
            }

            countActive = false;
            
            // Run
			nxProcess=new Process();
			nxProcess.StartInfo.FileName=nxEnv["JAVA"];
			nxProcess.StartInfo.Arguments=startArgs;
			nxProcess.StartInfo.UseShellExecute=false;
			nxProcess.StartInfo.WorkingDirectory=Path.Combine(nxEnv["NUXEO_HOME"],"bin");
			nxProcess.StartInfo.EnvironmentVariables["PATH"]=nxEnv["PATH"];
			nxProcess.StartInfo.CreateNoWindow=true;
			nxProcess.StartInfo.RedirectStandardError=true;
			nxProcess.StartInfo.RedirectStandardOutput=true;
			nxProcess.EnableRaisingEvents=true;
			nxProcess.Exited+=new EventHandler(Process_Exited);
			nxProcess.Start();
			nxProcess.OutputDataReceived += new DataReceivedEventHandler(nxProcess_OutputDataReceived);
			
			running=true;
            Stoppable = false;
			return true;
		}

        public bool testMemoryAllocation()
        {
            Process javaProcess = new Process();
            javaProcess.StartInfo.FileName = nxEnv["JAVA"];
            javaProcess.StartInfo.Arguments = nxEnv["JAVA_OPTS"] + " -version";
            javaProcess.StartInfo.UseShellExecute = false;
            javaProcess.StartInfo.CreateNoWindow = true;
            javaProcess.StartInfo.RedirectStandardError = true;
            javaProcess.Start();
            String line;
            while ((line = javaProcess.StandardError.ReadLine()) != null)
            {
                if (line.StartsWith("java version"))
                {
                    return true;
                }
            }
            return false;
        }

        void versionProcess_OutputDataReceived(object sender, DataReceivedEventArgs e)
        {
            if (e == null || e.Data == null)
            {
                Log("l'event ou les date sont nulls Oo", "INFO");
                return;
            }
            foreach (String line in e.Data.Split("\n".ToCharArray()))
            {
                Log(line, "INFO");
                if (line.StartsWith("java version"))
                {
                    memoryAllocated = true;
                }
            }
        }

		void nxProcess_OutputDataReceived(object sender, DataReceivedEventArgs e)
		{
			foreach(String line in e.Data.Split("\n".ToCharArray())) {
				if (!countActive && e.Data.Contains("[OSGiRuntimeService] Nuxeo EP Started")) {
					countActive = true;
					countStatus = 0;
				}
				if (!countActive) {  
					return;
				}
				
				if (e.Data.Contains("====================================================")) {
					countStatus += 1;
				}
				
				if (countStatus >= 3) {
                    Stoppable = true;
					ProcessStarted(sender, new EventArgs());
					nxProcess.OutputDataReceived -= new DataReceivedEventHandler(nxProcess_OutputDataReceived);
				}
			}
		}
		
		public bool Stop() {
			if (nxEnv == null && !SetupEnv()) {
				return false;
			}
			
			stopProcess=new Process();
			stopProcess.StartInfo.FileName=nxEnv["JAVA"];
			stopProcess.StartInfo.Arguments=stopArgs;
			stopProcess.StartInfo.UseShellExecute=false;
			stopProcess.StartInfo.CreateNoWindow=true;
			stopProcess.StartInfo.RedirectStandardError=true;
			stopProcess.StartInfo.RedirectStandardOutput=true;
			stopProcess.EnableRaisingEvents=true;
			stopProcess.Start();

            Thread.Sleep(7000);
			return true;
		}
		
		public bool Terminate() {
			if (nxProcess!=null) nxProcess.Kill();
			return true;
		}
		public Process getProcess() {
			return nxProcess;
		}
		
		public Process getConfProcess() {
			return confProcess;
		}
		
		public Process getStopProcess() {
			return stopProcess;
		}
		
	}
}
