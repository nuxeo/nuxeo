
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text.RegularExpressions;


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
	
	/// <summary>
	/// This class starts and stop a Nuxeo server in the background.
	/// It can be used both by the service and the Windows application.
	/// </summary>
	public class NuxeoController
	{
		
		private static Process nxProcess=null;
		private Dictionary<String,String> nxConfig=new Dictionary<String, String>();
		private String NuxeoConf="";
		public bool running=false;
		
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
		
		// Utility : check java version
		
		private String CheckJavaVersion(String java) {
			Regex jv=new Regex("^java version \"([^\"]*)\"$");
			Process javaProcess=new Process();
			javaProcess.StartInfo.FileName=java;
			javaProcess.StartInfo.Arguments="-version";
			javaProcess.StartInfo.UseShellExecute=false;
			javaProcess.StartInfo.CreateNoWindow=true;
			javaProcess.StartInfo.RedirectStandardError=true;
			javaProcess.Start();
			String line;
			while ((line=javaProcess.StandardError.ReadLine())!=null) {
				MatchCollection matchList=jv.Matches(line);
				if (matchList.Count>0) {
					Match firstMatch=matchList[0];
					if (firstMatch.Groups.Count<2) continue;
					return firstMatch.Groups[1].ToString();
				}
			}
			return "UNKNOWN";
		}
		
		// Utility : read nuxeo.conf into the nxConfig dictionary
		
		private bool ParseConfig() {
			// Get config file location
			NuxeoConf=Environment.GetEnvironmentVariable("NUXEO_CONF");
			if (!File.Exists(NuxeoConf)) {
				NuxeoConf="nuxeo.conf";
				if (!File.Exists(NuxeoConf)) {
					NuxeoConf="C:\\DEV\\nuxeo-dm-jboss\\bin\\nuxeo.conf";
					if (!File.Exists(NuxeoConf)) {
						Log("Could not find nuxeo configuration");
						return false;
					}
				}
			}
			Log("Using configuration at "+NuxeoConf,"INFO");
			// Read config file
			String line;
			String[] split;
			char[] splitParams={'='};
			try {
				System.IO.StreamReader file=new System.IO.StreamReader(NuxeoConf);
				while ((line=file.ReadLine())!=null) {
					if (line.Length==0) continue;
					if (line[0]=='#') continue;
					split=line.Split(splitParams,2);
					if (split.Length!=2) continue;
					nxConfig.Add(split[0],split[1]);
					Log(split[0]+" -> "+split[1],"DEBUG");
				}
			} catch (Exception e) {
				Log("Error reading "+NuxeoConf);
				Log(e.Message);
				return false;
			}
			return true;
		}
		
		// Callback on process exit
		
		private void Process_Exited(object sender, EventArgs e) {
			Log("Application has exited.","WARN");
			nxConfig=new Dictionary<String, String>();
			nxProcess=null;
			running=false;
		}
		
		// ********** STARTUP **********
		
		public bool Start() {
			if (nxProcess!=null) return false;
			if (ParseConfig()==false) return false;
			
			Log("*** Setting up environment ***","DEBUG");
			
			DirectoryInfo di;
			FileInfo[] ls;
			
			// Setup the JVM
			String JAVA="java.exe";
			if (nxConfig.ContainsKey("JAVA_HOME")) {
				JAVA=Path.Combine(Path.Combine(nxConfig["JAVA_HOME"],"bin"),"java.exe");
			}
			Log("JAVA = "+JAVA,"DEBUG");
			
			// Setup NUXEO_HOME
			String NUXEO_HOME=null;
			if (nxConfig.ContainsKey("NUXEO_HOME")) {
				NUXEO_HOME=nxConfig["NUXEO_HOME"];
			} else if (Environment.GetEnvironmentVariable("NUXEO_HOME")!=null) {
				NUXEO_HOME=Environment.GetEnvironmentVariable("NUXEO_HOME");
			} else if (Environment.GetEnvironmentVariable("JBOSS_HOME")!=null) {
				NUXEO_HOME=Environment.GetEnvironmentVariable("JBOSS_HOME");
			} else if (Environment.GetEnvironmentVariable("CATALINA_HOME")!=null) {
				NUXEO_HOME=Environment.GetEnvironmentVariable("CATALINA_HOME");
			} else if (Environment.GetEnvironmentVariable("JETTY_HOME")!=null) {
				NUXEO_HOME=Environment.GetEnvironmentVariable("JETTY_HOME");
			} else {
				NUXEO_HOME=Path.GetDirectoryName(Directory.GetCurrentDirectory());
			}
			Log("NUXEO_HOME = "+NUXEO_HOME,"DEBUG");
			
			// Setup LOG_DIR
			String LOG_DIR=null;
			if (nxConfig.ContainsKey("nuxeo.log.dir")) {
				LOG_DIR=nxConfig["nuxeo.log.dir"];
				if (!Path.IsPathRooted(LOG_DIR)) {
					LOG_DIR=Path.Combine(NUXEO_HOME,LOG_DIR);
				}
			} else {
				LOG_DIR=Path.Combine(NUXEO_HOME,"log");
			}
			try {
				if (!Directory.Exists(LOG_DIR)) Directory.CreateDirectory(LOG_DIR);
			} catch (Exception e) {
				Log("Cannot create "+LOG_DIR,"ERROR");
				Log(e.Message,"ERROR");
				return false;
			}
			String LOG=Path.Combine(LOG_DIR,"console.log");
			Log("LOG = "+LOG,"DEBUG");
			
			// Setup PID_DIR
			String PID_DIR=null;
			if (nxConfig.ContainsKey("nuxeo.pid.dir")) {
				PID_DIR=nxConfig["nuxeo.pid.dir"];
				if (!Path.IsPathRooted(PID_DIR)) {
					PID_DIR=Path.Combine(NUXEO_HOME,PID_DIR);
				}
			} else {
				PID_DIR=LOG_DIR;
			}
			try {
				if (!Directory.Exists(PID_DIR)) Directory.CreateDirectory(PID_DIR);
			} catch (Exception e) {
				Log("Cannot create "+PID_DIR,"ERROR");
				Log(e.Message,"ERROR");
				return false;
			}
			String PID=Path.Combine(PID_DIR,"nuxeo.pid");
			Log("PID = "+PID,"DEBUG");
			
			// Setup DATA_DIR
			// We don't set a default to keep backward compatibility
			String DATA_DIR=null;
			if (nxConfig.ContainsKey("nuxeo.data.dir")) {
				DATA_DIR=nxConfig["nuxeo.data.dir"];
				if (!Path.IsPathRooted(DATA_DIR)) {
					DATA_DIR=Path.Combine(NUXEO_HOME,DATA_DIR);
				}
				try {
					if (!Directory.Exists(DATA_DIR)) Directory.CreateDirectory(DATA_DIR);
				} catch (Exception e) {
					Log("Cannot create "+DATA_DIR,"ERROR");
					Log(e.Message,"ERROR");
					return false;
				}
				Log("DATA_DIR = "+DATA_DIR,"DEBUG");
			}
			
			// Setup NUXEO_BIND_ADDRESS
			String NUXEO_BIND_ADDRESS="0.0.0.0";
			if (nxConfig.ContainsKey("nuxeo.bind.address")) {
				NUXEO_BIND_ADDRESS=nxConfig["nuxeo.bind.address"];
			}
			
			// Application server detection
			bool jboss=false;
			bool tomcat=false;
			bool jetty=false;
			String jbossjar=Path.Combine(Path.Combine(NUXEO_HOME,"bin"),"run.jar");
			String tomcatjar=Path.Combine(Path.Combine(NUXEO_HOME,"bin"),"bootstrap.jar");
			String jettyjar=null;
			String JettyVersion=null;
			String JettyJarName=null;
			di=new DirectoryInfo(NUXEO_HOME);
			ls=di.GetFiles();
			foreach (FileInfo fname in ls) {
				if (fname.ToString().StartsWith("nuxeo-runtime-launcher")) {
					JettyJarName=fname.ToString();
					jettyjar=Path.Combine(NUXEO_HOME,JettyJarName);
					Regex jv=new Regex("^nuxeo-runtime-launcher-(.*).jar");
					MatchCollection matchList=jv.Matches(JettyJarName);
					Match firstMatch=matchList[0];
					if (firstMatch.Groups.Count<2) {
						Log("Can't determine jetty version","ERROR");
						return false;
					}
					JettyVersion=firstMatch.Groups[1].ToString();
					Log("Jetty version = "+JettyVersion,"DEBUG");
				}
			}
			if (File.Exists(jbossjar)) jboss=true;
			else if (File.Exists(tomcatjar)) tomcat=true;
			else if (File.Exists(jettyjar)) jetty=true;
			else {
				Log("Could not find startup jars for either JBoss, Tomcat or Jetty in "+NUXEO_HOME,"ERROR");
				return false;
			}
			
			String javaVersion=CheckJavaVersion(JAVA);
			Log("Java version = "+javaVersion,"DEBUG");
			
			// Application server-specific stuff
			
			String JavaArgs="";
			
			//
			// Jboss
			//
			
			if (jboss==true) {
				
				// Fix JRE 1.5/1.6 issue with script-api.jar
				String NuxeoEAR=Path.Combine(Path.Combine(Path.Combine(Path.Combine(NUXEO_HOME,"server"),"default"),"deploy"),"nuxeo.ear");
			    String NuxeoLib=Path.Combine(NuxeoEAR,"lib");
			    String NuxeoLib15=Path.Combine(NuxeoEAR,"lib-jre1.5");
			    String ScriptAPI=null;
			    String ScriptAPI15=null;
			    if (javaVersion.StartsWith("1.6")) {
					di=new DirectoryInfo(NuxeoLib);
			    	ls=di.GetFiles();
			    	foreach (FileInfo fname in ls) {
			    		if (fname.ToString().StartsWith("script-api")) {
			    			ScriptAPI=Path.Combine(NuxeoLib,fname.ToString());
			    			ScriptAPI15=Path.Combine(NuxeoLib15,fname.ToString());
			    		}
			    	}
			    	if (ScriptAPI!=null) {
			    		Log("Moving scripting API out of the way (included in Java 6).","WARN");
			    		try {
			    			if (!Directory.Exists(NuxeoLib15)) Directory.CreateDirectory(NuxeoLib15);
			    			File.Move(ScriptAPI,ScriptAPI15);
			    		} catch (Exception e) {
			    			Log("Cannot move scripting API","ERROR");
			    			Log(e.Message,"ERROR");
			    			return false;
			    		}
			    	}
			    } else { // Java 5 assumed
			    	di=new DirectoryInfo(NuxeoLib15);
			    	ls=di.GetFiles();
			    	foreach (FileInfo fname in ls) {
			    		if (fname.ToString().StartsWith("script-api")) {
			    			ScriptAPI=Path.Combine(NuxeoLib,fname.ToString());
			    			ScriptAPI15=Path.Combine(NuxeoLib15,fname.ToString());
			    		}
			    	}
			    	if (ScriptAPI!=null) {
			    		Log("Moving scripting API to the CLASSPATH.","WARN");
			    		try {
			    			File.Move(ScriptAPI15,ScriptAPI);
			    		} catch (Exception e) {
			    			Log("Cannot move scripting API","ERROR");
			    			Log(e.Message,"ERROR");
			    			return false;
			    		}
			    	}
			    }
			    
			    String NUXEO_CLASSPATH="";
			    if (nxConfig.ContainsKey("CLASSPATH")) {
			    	NUXEO_CLASSPATH=nxConfig["CLASSPATH"]+":";
			    }
			    NUXEO_CLASSPATH=NUXEO_CLASSPATH+jbossjar;
			    
			    String NUXEO_ENDORSED=Path.Combine(Path.Combine(NUXEO_HOME,"lib"),"endorsed");
			    String NUXEO_DATA="";
			    if (DATA_DIR!=null) NUXEO_DATA=" -Djboss.server.data.dir="+DATA_DIR;
			    
			    String JAVA_OPTS="";
			    if (nxConfig.ContainsKey("JAVA_OPTS")) {
			    	JAVA_OPTS=nxConfig["JAVA_OPTS"];
			    }
			    
			    JavaArgs=JAVA_OPTS+" -classpath "+NUXEO_CLASSPATH+
			    	" -Dprogram.name=nuxeoctl -Djava.endorsed.dirs="+NUXEO_ENDORSED+
			    	" -Djboss.server.log.dir="+LOG_DIR+NUXEO_DATA+
			    	" -Dnuxeo.home="+NUXEO_HOME+" -Dnuxeo.conf="+NuxeoConf+
			    	" org.jboss.Main -b "+NUXEO_BIND_ADDRESS;
			    
			    Log("Jboss startup options : "+JavaArgs,"DEBUG");
			}
			
			//
			// TOMCAT
			//
			
			else if (tomcat==true) {
				
				String NUXEO_CLASSPATH="";
			    if (nxConfig.ContainsKey("CLASSPATH")) {
			    	NUXEO_CLASSPATH=nxConfig["CLASSPATH"]+":";
			    }
			    NUXEO_CLASSPATH=NUXEO_CLASSPATH+tomcatjar;
			    
			    String JAVA_OPTS="";
			    if (nxConfig.ContainsKey("JAVA_OPTS")) {
			    	JAVA_OPTS=nxConfig["JAVA_OPTS"];
			    }
			    
			    String LOGGING_PROPERTIES=Path.Combine(Path.Combine(NUXEO_HOME,"conf"),"logging.properties");
			    String CATALINA_TEMP=Path.Combine(NUXEO_HOME,"temp");
			    
				JavaArgs=JAVA_OPTS+" -classpath "+NUXEO_CLASSPATH+" -Dnuxeo.home="+
					NUXEO_HOME+" -Dnuxeo.conf="+NuxeoConf+
					" -Djava.util.logging.config.file="+LOGGING_PROPERTIES+
					" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"+
					" -Dcatalina.base="+NUXEO_HOME+" -Dcatalina.home="+NUXEO_HOME+
					" -Djava.io.tmpdir="+CATALINA_TEMP+
					" org.apache.catalina.startup.Bootstrap start";
				
				Log("Tomcat startup options : "+JavaArgs,"DEBUG");
				
			}
			
			//
			// JETTY
			//
			
			else if (jetty==true) {
				
				Directory.SetCurrentDirectory(NUXEO_HOME);
				
				String NUXEO_CLASSPATH="";
			    if (nxConfig.ContainsKey("CLASSPATH")) {
			    	NUXEO_CLASSPATH=nxConfig["CLASSPATH"]+":";
			    }
			    NUXEO_CLASSPATH=NUXEO_CLASSPATH+jettyjar;
			    
			    String JAVA_OPTS="";
			    if (nxConfig.ContainsKey("JAVA_OPTS")) {
			    	JAVA_OPTS=nxConfig["JAVA_OPTS"];
			    }
			    
			    String NUXEO_BUNDLES="bundles/.:lib/.:config";
			    
			    JAVA_OPTS=JAVA_OPTS+
			    	" -Djava.rmi.server.RMIClassLoaderSpi=org.nuxeo.runtime.launcher.NuxeoRMIClassLoader"+
			    	" -Dsun.lang.ClassLoader.allowArraySyntax=true"+
			    	" -Dderby.system.home="+Path.Combine(DATA_DIR,"derby")+
			    	" -Dorg.nuxeo.launcher.libdirs=lib";
			    
			    JavaArgs=JAVA_OPTS+" -classpath "+NUXEO_CLASSPATH+
			    	" -Dnuxeo.home="+NUXEO_HOME+" -Dnuxeo.conf="+NuxeoConf+
			    	" -jar "+JettyJarName+
			    	" bundles/nuxeo-runtime-osgi-"+JettyVersion+".jar/org.nuxeo.osgi.application.Main "+
			    	NUXEO_BUNDLES+" -home "+NUXEO_HOME;
				
			    Log("Jetty startup options : "+JavaArgs,"DEBUG");
			    
			}
			
			
			
			// Run
			nxProcess=new Process();
			nxProcess.StartInfo.FileName=JAVA;
			nxProcess.StartInfo.Arguments=JavaArgs;
			nxProcess.StartInfo.UseShellExecute=false;
			nxProcess.StartInfo.CreateNoWindow=true;
			nxProcess.StartInfo.RedirectStandardError=true;
			nxProcess.StartInfo.RedirectStandardOutput=true;
			nxProcess.EnableRaisingEvents=true;
			nxProcess.Exited+=new EventHandler(Process_Exited);
			nxProcess.Start();
			running=true;
			return true;
		}
		
		public bool Stop() {
			// Unclean shutdown. Will change to use the shutdown jars soon.
			if (nxProcess!=null) nxProcess.Kill();
			return true;
		}
		
		public Process getProcess() {
			return nxProcess;
		}
		
		
	}
}