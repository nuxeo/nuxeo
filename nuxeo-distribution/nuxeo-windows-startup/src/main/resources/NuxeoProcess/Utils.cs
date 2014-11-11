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
*     Mathieu Guillaume
*/

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text.RegularExpressions;
using System.Reflection;
using Microsoft.Win32;

namespace NuxeoProcess
{
	
	partial class NuxeoController
	{
		//
		// Utility : open registry key
		//
		
		private RegistryKey OpenRegistryKey(String key) {
			String regBase=@"SOFTWARE\";
			String regBase6432=@"SOFTWARE\Wow6432Node\";
			try {
				RegistryKey regKey=Registry.LocalMachine.OpenSubKey(regBase+key);
				if (regKey==null) regKey=Registry.LocalMachine.OpenSubKey(regBase6432+key);
				return regKey;
			} catch {
				return null;
			}
		}
		
		
		//
		// Utility : check java version
		//
		
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
		
		
		//
		// Utility : read nuxeo.conf into the nxConfig dictionary
		//
		
		private Dictionary<String,String> ParseConfig() {
			Dictionary<String,String> nxConfig=new Dictionary<String, String>();
			// Get config file location
			String NUXEO_CONF=null;
			// Check registry (on Windows)
			if (platform == "windows" ) {
				RegistryKey nuxeoKey=OpenRegistryKey(productName);
				if (nuxeoKey!=null) NUXEO_CONF=(String)nuxeoKey.GetValue("ConfigFile");
			}
			// Check environment
			if (!File.Exists(NUXEO_CONF))
				NUXEO_CONF = Environment.GetEnvironmentVariable("NUXEO_CONF");
			// Check work directory
			if (!File.Exists(NUXEO_CONF))
				NUXEO_CONF = "nuxeo.conf";
			// Check desktop
		    if (!File.Exists(NUXEO_CONF)) {
				String desktop = Environment.GetFolderPath(Environment.SpecialFolder.Desktop);
				NUXEO_CONF = Path.Combine(desktop,"nuxeo.conf");
			}
			if (!File.Exists(NUXEO_CONF)) {
				String binDir = Directory.GetParent(Assembly.GetExecutingAssembly().Location).ToString();
				NUXEO_CONF = Path.Combine(binDir, "nuxeo.conf");
			}
			// Give up
			if (!File.Exists(NUXEO_CONF)) {
				Log("Could not find nuxeo configuration","ERROR");
				return null;
			}
			Log("Using configuration at "+NUXEO_CONF,"DEBUG");
			// Read config file
			String line;
			String[] split;
			char[] splitParams={'='};
			try {
				using(StreamReader file = new StreamReader(NUXEO_CONF)) {
				    while ((line=file.ReadLine())!=null) {
                        if (line.Length == 0) continue;
                        if (line[0] == '#') continue;
                        split = line.Split(splitParams, 2);
                        if (split.Length != 2) continue;
                        nxConfig.Add(split[0].Trim(), split[1].Trim());
                    }
                }
			} catch (Exception e) {
				Log("Error reading "+NUXEO_CONF);
				Log(e.Message);
				return null;
			}
			nxConfig.Add("NUXEO_CONF",NUXEO_CONF);
			
			Log("*** Configuration :","DEBUG");
			foreach(KeyValuePair<String,String> pair in nxConfig) {
				Log(pair.Key+" -> "+pair.Value,"DEBUG");
			}
			
			return nxConfig;
		}
		
		
		//
		// Utility : set up environment based on the configuration
		//
		
		private bool SetupEnv(Dictionary<String,String> nxConfig) {
            nxEnv = new Dictionary<String, String>();
			nxEnv.Add("NUXEO_CONF",nxConfig["NUXEO_CONF"]);
			
			// Setup the JVM
			String JAVA="javaw.exe";
			if (platform=="unix") JAVA="java";
			if (nxConfig.ContainsKey("JAVA_HOME")) {
				JAVA=Path.Combine(Path.Combine(nxConfig["JAVA_HOME"],"bin"),JAVA);
			} else if (Environment.GetEnvironmentVariable("JAVA_HOME")!=null) {
				JAVA=Path.Combine(Path.Combine(Environment.GetEnvironmentVariable("JAVA_HOME"),"bin"),JAVA);
			} else if (platform=="windows") {
				String regJDK=@"JavaSoft\Java Development Kit";
				RegistryKey regJDKKey=null;
				RegistryKey regCurrentJDKKey=null;
				try {
					regJDKKey=OpenRegistryKey(regJDK);
					String regCurrentJDK=(String)regJDKKey.GetValue("CurrentVersion");
					regCurrentJDKKey=regJDKKey.OpenSubKey(regCurrentJDK);
					String regJavaHome=(String)regCurrentJDKKey.GetValue("JavaHome");
					JAVA=Path.Combine(Path.Combine(regJavaHome,"bin"),JAVA);
					Log("Using JavaHome from registry : "+regJavaHome,"DEBUG");
				} catch {
					Log("Can not find JDK in the registry","ERROR");
				}
				if (regCurrentJDKKey!=null) regCurrentJDKKey.Close();
				if (regJDKKey!=null) regCurrentJDKKey.Close();
			}

            if (!File.Exists(JAVA)) {
                Log("Can not find "+JAVA, "ERROR");
                return false;
            }
			nxEnv.Add("JAVA",JAVA);
			
			if (nxConfig.ContainsKey("CLASSPATH")) {
				nxEnv.Add("CLASSPATH",nxConfig["CLASSPATH"]);
			} else if (Environment.GetEnvironmentVariable("CLASSPATH")!=null) {
				nxEnv.Add("CLASSPATH",Environment.GetEnvironmentVariable("CLASSPATH"));
			} else {
				nxEnv.Add("CLASSPATH","");
			}
			
			if (nxConfig.ContainsKey("JAVA_OPTS")) {
				nxEnv.Add("JAVA_OPTS",nxConfig["JAVA_OPTS"]);
			} else if (Environment.GetEnvironmentVariable("JAVA_OPTS")!=null) {
				nxEnv.Add("JAVA_OPTS",Environment.GetEnvironmentVariable("JAVA_OPTS"));
			} else {
				nxEnv.Add("JAVA_OPTS","");
			}
			
			// Setup NUXEO_HOME
			String NUXEO_HOME=null;
			if (nxConfig.ContainsKey("NUXEO_HOME")) {
                NUXEO_HOME = nxConfig["NUXEO_HOME"];
            }
            else if (Environment.GetEnvironmentVariable("NUXEO_HOME") != null)
            {
                NUXEO_HOME = Environment.GetEnvironmentVariable("NUXEO_HOME");
            }
            else if (Environment.GetEnvironmentVariable("JBOSS_HOME") != null)
            {
                NUXEO_HOME = Environment.GetEnvironmentVariable("JBOSS_HOME");
            }
            else if (Environment.GetEnvironmentVariable("CATALINA_HOME") != null)
            {
                NUXEO_HOME = Environment.GetEnvironmentVariable("CATALINA_HOME");
            }
            else
            {
                NUXEO_HOME = Directory.GetParent(Assembly.GetEntryAssembly().Location).Parent.FullName;
			}

			nxEnv.Add("NUXEO_HOME",NUXEO_HOME);
			
			// Add 3rdparty dir to PATH
			
			String PathValue = System.Environment.GetEnvironmentVariable("Path");
			PathValue = PathValue + ";" + Path.Combine(NUXEO_HOME,"3rdparty");
			System.Environment.SetEnvironmentVariable("Path",PathValue);
			
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
			nxEnv.Add("LOG_DIR",LOG_DIR);
			nxEnv.Add("LOG",LOG);
			
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
			nxEnv.Add("PID_DIR",PID_DIR);
			nxEnv.Add("PID",PID);
			
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
			}
			nxEnv.Add("DATA_DIR",DATA_DIR);
			
			// Setup NUXEO_BIND_ADDRESS
			String NUXEO_BIND_ADDRESS="0.0.0.0";
			if (nxConfig.ContainsKey("nuxeo.bind.address")) {
				NUXEO_BIND_ADDRESS=nxConfig["nuxeo.bind.address"];
			}
			nxEnv.Add("NUXEO_BIND_ADDRESS",NUXEO_BIND_ADDRESS);
			
			Log("*** Environment :","DEBUG");
			foreach(KeyValuePair<String,String> pair in nxEnv) {
				Log(pair.Key+" -> "+pair.Value,"DEBUG");
			}
			
			return true;
		}
		
		
		//
		// Utility : detect application server
		//
		
		private bool SetupApplicationServer() {
			
			String srvType=null;
			String srvStartJar=null;
			String srvVersion=null;
			
			// Find out which server type we're running
			
			String jbossJar=Path.Combine(Path.Combine(nxEnv["NUXEO_HOME"],"bin"),"run.jar");
			if (File.Exists(jbossJar)) {
				srvType="jboss";
				srvStartJar=jbossJar;
				if (SetupJboss(srvStartJar,srvVersion)==false) {
					Log("Could not set up JBoss options","ERROR");
					return false;
				}
			}
			
			if (srvType==null) {
				String tomcatJar=Path.Combine(Path.Combine(nxEnv["NUXEO_HOME"],"bin"),"bootstrap.jar");
				if (File.Exists(tomcatJar)) {
					srvType="tomcat";
					srvStartJar=tomcatJar;
					if (SetupTomcat(srvStartJar,srvVersion)==false) {
						Log("Could not set up Tomcat options","ERROR");
						return false;
					}
				}
			}
			
			if (srvType==null) {
				Log("Could not find startup jars for either JBoss or Tomcat "+nxEnv["NUXEO_HOME"],"ERROR");
				return false;
			}
			
			return true;
			
		}
		
		
		private bool SetupJboss(String srvStartJar, String srvVersion) {
			
			// Fix JRE 1.5/1.6 issue with script-api.jar
			String NuxeoEAR=Path.Combine(Path.Combine(Path.Combine(Path.Combine(nxEnv["NUXEO_HOME"],"server"),"default"),"deploy"),"nuxeo.ear");
			String NuxeoLib=Path.Combine(NuxeoEAR,"lib");
			String NuxeoLib15=Path.Combine(NuxeoEAR,"lib-jre1.5");
			String ScriptAPI=null;
			String ScriptAPI15=null;
			DirectoryInfo di;
			FileInfo[] ls;
			if (CheckJavaVersion(nxEnv["JAVA"]).StartsWith("1.6")) {
				di=new DirectoryInfo(NuxeoLib);
				ls=di.GetFiles();
				foreach (FileInfo fname in ls) {
					String shortname=Path.GetFileName(fname.ToString());
					if (shortname.StartsWith("script-api")) {
				    	ScriptAPI=Path.Combine(NuxeoLib,shortname);
			    		ScriptAPI15=Path.Combine(NuxeoLib15,shortname);
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
				if (!di.Exists) {
					di.Create();
				}
				ls=di.GetFiles();
			    foreach (FileInfo fname in ls) {
					String shortname=Path.GetFileName(fname.ToString());
					if (shortname.StartsWith("script-api")) {
			    		ScriptAPI=Path.Combine(NuxeoLib,shortname);
			    		ScriptAPI15=Path.Combine(NuxeoLib15,shortname);
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
			
			// Start-Stop args
			
			String NUXEO_CLASSPATH;
			if (nxEnv["CLASSPATH"].Length==0) NUXEO_CLASSPATH=srvStartJar;
			else NUXEO_CLASSPATH=nxEnv["CLASSPATH"]+Path.PathSeparator+srvStartJar;
			
			String NUXEO_ENDORSED=Path.Combine(Path.Combine(nxEnv["NUXEO_HOME"],"lib"),"endorsed");
			
			String NUXEO_DATA="";
			if (nxEnv["DATA_DIR"]!=null) {
			    	NUXEO_DATA=" -Djboss.server.data.dir=\""+nxEnv["DATA_DIR"]+"\"";
			}
			
			startArgs=nxEnv["JAVA_OPTS"]+" -classpath \""+NUXEO_CLASSPATH+"\""+
				" -Dprogram.name=nuxeoctl -Djava.endorsed.dirs=\""+NUXEO_ENDORSED+"\""+
				" -Djboss.server.log.dir=\""+nxEnv["LOG_DIR"]+"\""+NUXEO_DATA+
				" -Dnuxeo.home=\""+nxEnv["NUXEO_HOME"]+"\""+
				" -Dnuxeo.conf=\""+nxEnv["NUXEO_CONF"]+"\""+
				" org.jboss.Main -b "+nxEnv["NUXEO_BIND_ADDRESS"];
			
			stopArgs=nxEnv["JAVA_OPTS"]+" -classpath \""+NUXEO_CLASSPATH+"\" -jar \""+
				Path.Combine(Path.Combine(nxEnv["NUXEO_HOME"],"bin"),"shutdown.jar")+
				"\" -S";
			
			Log("JBoss startup options : "+startArgs,"DEBUG");
			Log("JBoss shutdown options : "+stopArgs,"DEBUG");
			
			return true;
			
		} // End SetupJboss
		
		
		private bool SetupTomcat(String srvStartJar, String srvVersion) {
			
			String NUXEO_CLASSPATH;
			if (nxEnv["CLASSPATH"].Length==0) NUXEO_CLASSPATH=srvStartJar;
			else NUXEO_CLASSPATH=nxEnv["CLASSPATH"]+Path.PathSeparator+srvStartJar;
			    
			String CATALINA_TEMP=Path.Combine(nxEnv["NUXEO_HOME"],"temp");
			    
			startArgs=nxEnv["JAVA_OPTS"]+" -classpath \""+NUXEO_CLASSPATH+"\""+
				" -Dnuxeo.home=\""+nxEnv["NUXEO_HOME"]+"\""+
				" -Dnuxeo.conf=\""+nxEnv["NUXEO_CONF"]+"\""+
				" -Dnuxeo.log.dir=\""+nxEnv["LOG_DIR"]+"\""+
				" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"+
				" -Dcatalina.base=\""+nxEnv["NUXEO_HOME"]+"\""+
				" -Dcatalina.home=\""+nxEnv["NUXEO_HOME"]+"\""+
				" -Djava.io.tmpdir=\""+CATALINA_TEMP+"\""+
				" -Dnuxeo.data.dir=\""+nxEnv["DATA_DIR"]+"\""+
				" org.apache.catalina.startup.Bootstrap start";
			
			stopArgs=nxEnv["JAVA_OPTS"]+" -classpath \""+NUXEO_CLASSPATH+"\""+
				" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"+
				" -Dcatalina.base=\""+nxEnv["NUXEO_HOME"]+"\""+
				" -Dcatalina.home=\""+nxEnv["NUXEO_HOME"]+"\""+
				" -Djava.io.tmpdir=\""+CATALINA_TEMP+"\""+
				" org.apache.catalina.startup.Bootstrap stop";
				
			Log("Tomcat startup options : "+startArgs,"DEBUG");
			Log("Tomcat shutdown options : "+stopArgs,"DEBUG");
			
			return true;
			
		} // End SetupTomcat
		
		
	}
}
