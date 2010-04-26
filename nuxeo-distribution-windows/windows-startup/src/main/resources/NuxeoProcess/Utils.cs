
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text.RegularExpressions;

namespace NuxeoProcess
{
	
	partial class NuxeoController
	{
		// Utility : check java version
		
		public String CheckJavaVersion(String java) {
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
		
		public Dictionary<String,String> ParseConfig() {
			Dictionary<String,String> nxConfig=new Dictionary<String, String>();
			// Get config file location
			String NuxeoConf=Environment.GetEnvironmentVariable("NUXEO_CONF");
			if (!File.Exists(NuxeoConf)) {
				NuxeoConf="nuxeo.conf";
				if (!File.Exists(NuxeoConf)) {
					NuxeoConf="C:\\DEV\\nuxeo-dm-jboss\\bin\\nuxeo.conf";
					if (!File.Exists(NuxeoConf)) {
						Log("Could not find nuxeo configuration");
						return null;
					}
				}
			}
			//Log("Using configuration at "+NuxeoConf,"INFO");
			// Read config file
			String line;
			String[] split;
			char[] splitParams={'='};
			try {
				StreamReader file=new StreamReader(NuxeoConf);
				while ((line=file.ReadLine())!=null) {
					if (line.Length==0) continue;
					if (line[0]=='#') continue;
					split=line.Split(splitParams,2);
					if (split.Length!=2) continue;
					nxConfig.Add(split[0].Trim(),split[1].Trim());
					Log(split[0].Trim()+" -> "+split[1].Trim(),"DEBUG");
				}
			} catch (Exception e) {
				Log("Error reading "+NuxeoConf);
				Log(e.Message);
				return null;
			}
			nxConfig.Add("NuxeoConf",NuxeoConf);
			return nxConfig;
		}
		
		
	}
}
