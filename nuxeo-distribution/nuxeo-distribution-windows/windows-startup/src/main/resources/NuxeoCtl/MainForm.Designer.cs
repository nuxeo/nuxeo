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
using System.Diagnostics;
using NuxeoProcess;

namespace NuxeoCtl
{
	partial class MainForm
	{
		/// <summary>
		/// Designer variable used to keep track of non-visual components.
		/// </summary>
		private System.ComponentModel.IContainer components = null;
		
		/// <summary>
		/// Disposes resources used by the form.
		/// </summary>
		/// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
		protected override void Dispose(bool disposing)
		{
			if (disposing) {
				if (components != null) {
					components.Dispose();
				}
			}
			base.Dispose(disposing);
		}
		
		/// <summary>
		/// This method is required for Windows Forms designer support.
		/// Do not change the method contents inside the source code editor. The Forms designer might
		/// not be able to load this method if it was changed manually.
		/// </summary>
		private void InitializeComponent()
		{
            this.startButton = new System.Windows.Forms.Button();
            this.stopButton = new System.Windows.Forms.Button();
            this.logBox = new System.Windows.Forms.RichTextBox();
            this.terminateButton = new System.Windows.Forms.Button();
            this.SuspendLayout();
            // 
            // startButton
            // 
            this.startButton.Location = new System.Drawing.Point(12, 33);
            this.startButton.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
            this.startButton.Name = "startButton";
            this.startButton.Size = new System.Drawing.Size(116, 22);
            this.startButton.TabIndex = 0;
            this.startButton.Text = "Start";
            this.startButton.UseVisualStyleBackColor = true;
            this.startButton.Click += new System.EventHandler(this.StartClick);
            // 
            // stopButton
            // 
            this.stopButton.Enabled = false;
            this.stopButton.Location = new System.Drawing.Point(134, 33);
            this.stopButton.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
            this.stopButton.Name = "stopButton";
            this.stopButton.Size = new System.Drawing.Size(116, 22);
            this.stopButton.TabIndex = 1;
            this.stopButton.Text = "Stop";
            this.stopButton.UseVisualStyleBackColor = true;
            this.stopButton.Click += new System.EventHandler(this.StopClick);
            // 
            // logBox
            // 
            this.logBox.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom)
                        | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.logBox.DetectUrls = false;
            this.logBox.Font = new System.Drawing.Font("Verdana", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.logBox.Location = new System.Drawing.Point(12, 79);
            this.logBox.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
            this.logBox.Name = "logBox";
            this.logBox.ReadOnly = true;
            this.logBox.Size = new System.Drawing.Size(917, 462);
            this.logBox.TabIndex = 4;
            this.logBox.TabStop = false;
            this.logBox.Text = "";
            // 
            // terminateButton
            // 
            this.terminateButton.BackColor = System.Drawing.Color.Red;
            this.terminateButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.terminateButton.Location = new System.Drawing.Point(256, 32);
            this.terminateButton.Name = "terminateButton";
            this.terminateButton.Size = new System.Drawing.Size(113, 23);
            this.terminateButton.TabIndex = 5;
            this.terminateButton.Text = "Terminate";
            this.terminateButton.UseVisualStyleBackColor = false;
            this.terminateButton.Visible = false;
            this.terminateButton.Click += new System.EventHandler(this.TerminateClick);
            // 
            // MainForm
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(941, 553);
            this.Controls.Add(this.terminateButton);
            this.Controls.Add(this.logBox);
            this.Controls.Add(this.stopButton);
            this.Controls.Add(this.startButton);
            this.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
            this.Name = "MainForm";
            this.Text = "NuxeoCtl";
            this.Shown += new System.EventHandler(this.MainForm_Shown);
            this.ResumeLayout(false);

		}
		private System.Windows.Forms.Button terminateButton;
		private System.Windows.Forms.RichTextBox logBox;
		private System.Windows.Forms.Button startButton;
		private System.Windows.Forms.Button stopButton;
		
		private NuxeoProcess.NuxeoController nxControl=null;
		
		// Button handlers
		
		bool StartService() {
			Log("Starting "+nxSvcName+" service...","WARN");
			try {
				nxService.Start();
				return true;
			} catch (Exception e) {
				Log(e.Message,"ERROR");
				return false;
			}
			
		}
		
		bool StartApplication() {
            if (nxControl == null)
            {
                nxControl = new NuxeoProcess.NuxeoController();
            }

			if (!nxControl.IsLogDelegated()) {
				nxControl.DelegatedLog+=new LogEventHandler(nxControllerLog);
				nxControl.SetDelegateLog(true);
			}

            if(!nxControl.Initialized) {
                nxControl.Initialize();
            }

            if (!nxControl.Initialized || nxControl.Start() == false)
            {
				Log("Could not start the application","ERROR");
				return false;
			}

			Process nxProcess=nxControl.getProcess();
			nxProcess.OutputDataReceived+=new DataReceivedEventHandler(OutputLog);
			nxProcess.BeginOutputReadLine();
			nxProcess.ErrorDataReceived+=new DataReceivedEventHandler(ErrorLog);
			nxProcess.BeginErrorReadLine();
			Log("Starting "+nxSvcName+" application...","WARN");
			return true;
		}
		
		bool StopService() {
			Log("Stopping "+nxSvcName+" service...","WARN");
			try {
				nxService.Stop();
				return true;
			} catch (Exception e) {
				Log(e.Message,"ERROR");
				return false;
			}
		}
		
		bool StopApplication() {
            if (nxControl == null)
            {
                nxControl = new NuxeoProcess.NuxeoController();
            }

            if (!nxControl.Initialized) {
                nxControl.Initialize();
            }

            if (nxControl != null && nxControl.Initialized)
            {
                nxControl.Stop();
                Log("Stopping " + nxSvcName + " application...", "WARN");
                Process stopProcess = nxControl.getStopProcess();
                stopProcess.OutputDataReceived += new DataReceivedEventHandler(OutputLog);
                stopProcess.BeginOutputReadLine();
                stopProcess.ErrorDataReceived += new DataReceivedEventHandler(ErrorLog);
                stopProcess.BeginErrorReadLine();

                return true;
            }
            else
            {
                Log("Application is not started", "WARN");
                return false;
            }
		}
		
		bool TerminateApplication() {
			if (nxControl!=null) {
				nxControl.Terminate();
				Log("Terminating "+nxSvcName+" application...","WARN");
				return true;
			} else {
				Log("Application is not started","WARN");
				return false;
			}
		}
		
		void StartClick(object sender, System.EventArgs e)
		{
			bool statusChanged;
			if (nxService!=null) {
				statusChanged=StartService();
			} else {
				statusChanged=StartApplication();
			}
			if (statusChanged) {
				stopButton.Enabled=true;
				startButton.Enabled=false;
			}
		}
		
		void StopClick(object sender, System.EventArgs e)
		{
			bool statusChanged;
			if (nxService!=null) {
				statusChanged=StopService();
			} else {
				statusChanged=StopApplication();
				terminateButton.Visible=true;
			}
			if (statusChanged) {
				startButton.Enabled=true;
				stopButton.Enabled=false;
			}
		}
		
		void TerminateClick(object sender, EventArgs e)
		{
			if (nxService==null) {
				TerminateApplication();
				terminateButton.Visible=false;
			}
		}
	}
}
