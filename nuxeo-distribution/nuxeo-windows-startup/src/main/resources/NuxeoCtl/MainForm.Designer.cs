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
			this.components = new System.ComponentModel.Container();
			System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(MainForm));
			this.logBox = new System.Windows.Forms.RichTextBox();
			this.terminateButton = new System.Windows.Forms.Button();
			this.label1 = new System.Windows.Forms.Label();
			this.pictureBox1 = new System.Windows.Forms.PictureBox();
			this.stopButton = new System.Windows.Forms.Button();
			this.startButton = new System.Windows.Forms.Button();
			this.showLogsButton = new System.Windows.Forms.Button();
			this.hideLogsButton = new System.Windows.Forms.Button();
			this.notifyIcon1 = new System.Windows.Forms.NotifyIcon(this.components);
			this.pictureBox2 = new System.Windows.Forms.PictureBox();
			this.nxCtlState = new System.Windows.Forms.Label();
			((System.ComponentModel.ISupportInitialize)(this.pictureBox1)).BeginInit();
			((System.ComponentModel.ISupportInitialize)(this.pictureBox2)).BeginInit();
			this.SuspendLayout();
			// 
			// logBox
			// 
			this.logBox.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
									| System.Windows.Forms.AnchorStyles.Left) 
									| System.Windows.Forms.AnchorStyles.Right)));
			this.logBox.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(64)))), ((int)(((byte)(64)))), ((int)(((byte)(64)))));
			this.logBox.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
			this.logBox.DetectUrls = false;
			this.logBox.Font = new System.Drawing.Font("Tahoma", 11F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Pixel);
			this.logBox.Location = new System.Drawing.Point(27, 267);
			this.logBox.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
			this.logBox.Name = "logBox";
			this.logBox.ReadOnly = true;
			this.logBox.Size = new System.Drawing.Size(593, 10);
			this.logBox.TabIndex = 4;
			this.logBox.TabStop = false;
			this.logBox.Text = "";
			this.logBox.Visible = false;
			// 
			// terminateButton
			// 
			this.terminateButton.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
			this.terminateButton.BackColor = System.Drawing.Color.Transparent;
			this.terminateButton.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("terminateButton.BackgroundImage")));
			this.terminateButton.BackgroundImageLayout = System.Windows.Forms.ImageLayout.None;
			this.terminateButton.Cursor = System.Windows.Forms.Cursors.Hand;
			this.terminateButton.FlatAppearance.BorderSize = 0;
			this.terminateButton.FlatAppearance.MouseDownBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(64)))), ((int)(((byte)(64)))), ((int)(((byte)(64)))));
			this.terminateButton.FlatAppearance.MouseOverBackColor = System.Drawing.Color.Transparent;
			this.terminateButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
			this.terminateButton.Font = new System.Drawing.Font("Tahoma", 11.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
			this.terminateButton.ForeColor = System.Drawing.SystemColors.ActiveCaptionText;
			this.terminateButton.Location = new System.Drawing.Point(530, 225);
			this.terminateButton.Name = "terminateButton";
			this.terminateButton.Size = new System.Drawing.Size(110, 35);
			this.terminateButton.TabIndex = 5;
			this.terminateButton.Text = "Terminate";
			this.terminateButton.UseVisualStyleBackColor = false;
			this.terminateButton.Visible = false;
			this.terminateButton.Click += new System.EventHandler(this.TerminateClick);
			// 
			// label1
			// 
			this.label1.Anchor = System.Windows.Forms.AnchorStyles.Bottom;
			this.label1.ForeColor = System.Drawing.SystemColors.ActiveCaptionText;
			this.label1.Location = new System.Drawing.Point(37, 294);
			this.label1.Name = "label1";
			this.label1.Size = new System.Drawing.Size(573, 23);
			this.label1.TabIndex = 6;
			this.label1.Text = "Copyright © 2001-2010 Nuxeo and respective authors. ";
			this.label1.TextAlign = System.Drawing.ContentAlignment.MiddleCenter;
			// 
			// pictureBox1
			// 
			this.pictureBox1.Anchor = System.Windows.Forms.AnchorStyles.Top;
			this.pictureBox1.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("pictureBox1.BackgroundImage")));
			this.pictureBox1.ErrorImage = null;
			this.pictureBox1.InitialImage = null;
			this.pictureBox1.Location = new System.Drawing.Point(163, 12);
			this.pictureBox1.Name = "pictureBox1";
			this.pictureBox1.Size = new System.Drawing.Size(326, 43);
			this.pictureBox1.TabIndex = 7;
			this.pictureBox1.TabStop = false;
			// 
			// stopButton
			// 
			this.stopButton.Anchor = System.Windows.Forms.AnchorStyles.Top;
			this.stopButton.BackColor = System.Drawing.Color.White;
			this.stopButton.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("stopButton.BackgroundImage")));
			this.stopButton.BackgroundImageLayout = System.Windows.Forms.ImageLayout.None;
			this.stopButton.Cursor = System.Windows.Forms.Cursors.Hand;
			this.stopButton.Enabled = false;
			this.stopButton.FlatAppearance.BorderColor = System.Drawing.Color.Silver;
			this.stopButton.FlatAppearance.MouseOverBackColor = System.Drawing.Color.White;
			this.stopButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
			this.stopButton.Font = new System.Drawing.Font("Tahoma", 11.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
			this.stopButton.ForeColor = System.Drawing.SystemColors.ControlDarkDark;
			this.stopButton.ImageAlign = System.Drawing.ContentAlignment.MiddleLeft;
			this.stopButton.Location = new System.Drawing.Point(260, 127);
			this.stopButton.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
			this.stopButton.Name = "stopButton";
			this.stopButton.Padding = new System.Windows.Forms.Padding(10, 0, 0, 0);
			this.stopButton.Size = new System.Drawing.Size(126, 37);
			this.stopButton.TabIndex = 1;
			this.stopButton.Text = "Stop";
			this.stopButton.UseVisualStyleBackColor = false;
			this.stopButton.Click += new System.EventHandler(this.StopClick);
			// 
			// startButton
			// 
			this.startButton.Anchor = System.Windows.Forms.AnchorStyles.Top;
			this.startButton.BackColor = System.Drawing.Color.White;
			this.startButton.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("startButton.BackgroundImage")));
			this.startButton.BackgroundImageLayout = System.Windows.Forms.ImageLayout.None;
			this.startButton.Cursor = System.Windows.Forms.Cursors.Hand;
			this.startButton.FlatAppearance.BorderColor = System.Drawing.Color.DarkGray;
			this.startButton.FlatAppearance.MouseOverBackColor = System.Drawing.Color.White;
			this.startButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
			this.startButton.Font = new System.Drawing.Font("Tahoma", 11.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
			this.startButton.ForeColor = System.Drawing.SystemColors.ControlDarkDark;
			this.startButton.ImageAlign = System.Drawing.ContentAlignment.MiddleLeft;
			this.startButton.Location = new System.Drawing.Point(260, 127);
			this.startButton.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
			this.startButton.Name = "startButton";
			this.startButton.Padding = new System.Windows.Forms.Padding(10, 0, 0, 0);
			this.startButton.RightToLeft = System.Windows.Forms.RightToLeft.No;
			this.startButton.Size = new System.Drawing.Size(126, 37);
			this.startButton.TabIndex = 0;
			this.startButton.Text = "Start";
			this.startButton.UseVisualStyleBackColor = false;
			this.startButton.Click += new System.EventHandler(this.StartClick);
			// 
			// showLogsButton
			// 
			this.showLogsButton.BackColor = System.Drawing.Color.Transparent;
			this.showLogsButton.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("showLogsButton.BackgroundImage")));
			this.showLogsButton.BackgroundImageLayout = System.Windows.Forms.ImageLayout.None;
			this.showLogsButton.Cursor = System.Windows.Forms.Cursors.Hand;
			this.showLogsButton.FlatAppearance.BorderSize = 0;
			this.showLogsButton.FlatAppearance.MouseDownBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(64)))), ((int)(((byte)(64)))), ((int)(((byte)(64)))));
			this.showLogsButton.FlatAppearance.MouseOverBackColor = System.Drawing.Color.Transparent;
			this.showLogsButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
			this.showLogsButton.Font = new System.Drawing.Font("Tahoma", 11.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
			this.showLogsButton.ForeColor = System.Drawing.SystemColors.ActiveCaptionText;
			this.showLogsButton.Location = new System.Drawing.Point(27, 225);
			this.showLogsButton.Name = "showLogsButton";
			this.showLogsButton.Size = new System.Drawing.Size(108, 35);
			this.showLogsButton.TabIndex = 8;
			this.showLogsButton.Text = "Show logs";
			this.showLogsButton.UseVisualStyleBackColor = false;
			this.showLogsButton.Click += new System.EventHandler(this.ShowLogsButtonClick);
			// 
			// hideLogsButton
			// 
			this.hideLogsButton.BackColor = System.Drawing.Color.Transparent;
			this.hideLogsButton.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("hideLogsButton.BackgroundImage")));
			this.hideLogsButton.BackgroundImageLayout = System.Windows.Forms.ImageLayout.None;
			this.hideLogsButton.Cursor = System.Windows.Forms.Cursors.Hand;
			this.hideLogsButton.Enabled = false;
			this.hideLogsButton.FlatAppearance.BorderSize = 0;
			this.hideLogsButton.FlatAppearance.MouseDownBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(64)))), ((int)(((byte)(64)))), ((int)(((byte)(64)))));
			this.hideLogsButton.FlatAppearance.MouseOverBackColor = System.Drawing.Color.Transparent;
			this.hideLogsButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
			this.hideLogsButton.Font = new System.Drawing.Font("Tahoma", 11.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
			this.hideLogsButton.ForeColor = System.Drawing.SystemColors.ActiveCaptionText;
			this.hideLogsButton.Location = new System.Drawing.Point(27, 225);
			this.hideLogsButton.Name = "hideLogsButton";
			this.hideLogsButton.Size = new System.Drawing.Size(108, 35);
			this.hideLogsButton.TabIndex = 9;
			this.hideLogsButton.Text = "Hide logs";
			this.hideLogsButton.UseVisualStyleBackColor = false;
			this.hideLogsButton.Visible = false;
			this.hideLogsButton.Click += new System.EventHandler(this.HideLogsButtonClick);
			// 
			// notifyIcon1
			// 
			this.notifyIcon1.Text = "notifyIcon1";
			this.notifyIcon1.Visible = true;
			// 
			// pictureBox2
			// 
			this.pictureBox2.Anchor = System.Windows.Forms.AnchorStyles.Bottom;
			this.pictureBox2.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("pictureBox2.BackgroundImage")));
			this.pictureBox2.Location = new System.Drawing.Point(37, 281);
			this.pictureBox2.Name = "pictureBox2";
			this.pictureBox2.Size = new System.Drawing.Size(573, 1);
			this.pictureBox2.TabIndex = 10;
			this.pictureBox2.TabStop = false;
			// 
			// nxCtlState
			// 
			this.nxCtlState.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
									| System.Windows.Forms.AnchorStyles.Right)));
			this.nxCtlState.ForeColor = System.Drawing.SystemColors.ActiveCaptionText;
			this.nxCtlState.Location = new System.Drawing.Point(37, 194);
			this.nxCtlState.Name = "nxCtlState";
			this.nxCtlState.Size = new System.Drawing.Size(573, 23);
			this.nxCtlState.TabIndex = 11;
			this.nxCtlState.TextAlign = System.Drawing.ContentAlignment.MiddleCenter;
			// 
			// MainForm
			// 
			this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
			this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
			this.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(55)))), ((int)(((byte)(55)))), ((int)(((byte)(55)))));
			this.BackgroundImage = ((System.Drawing.Image)(resources.GetObject("$this.BackgroundImage")));
			this.BackgroundImageLayout = System.Windows.Forms.ImageLayout.None;
			this.ClientSize = new System.Drawing.Size(652, 335);
			this.Controls.Add(this.nxCtlState);
			this.Controls.Add(this.pictureBox2);
			this.Controls.Add(this.pictureBox1);
			this.Controls.Add(this.label1);
			this.Controls.Add(this.terminateButton);
			this.Controls.Add(this.logBox);
			this.Controls.Add(this.showLogsButton);
			this.Controls.Add(this.hideLogsButton);
			this.Controls.Add(this.startButton);
			this.Controls.Add(this.stopButton);
			this.Font = new System.Drawing.Font("Tahoma", 11F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Pixel);
			this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
			this.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
			this.Name = "MainForm";
			this.Text = "NuxeoCtl";
			this.Load += new System.EventHandler(this.MainFormLoad);
			this.Shown += new System.EventHandler(this.MainForm_Shown);
			this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.MainFormFormClosing);
			((System.ComponentModel.ISupportInitialize)(this.pictureBox1)).EndInit();
			((System.ComponentModel.ISupportInitialize)(this.pictureBox2)).EndInit();
			this.ResumeLayout(false);
		}
		private System.Windows.Forms.Label nxCtlState;
		private System.Windows.Forms.Button showLogsButton;
		private System.Windows.Forms.Button hideLogsButton;
		private System.Windows.Forms.PictureBox pictureBox2;
		private System.Windows.Forms.NotifyIcon notifyIcon1;
		private System.Windows.Forms.PictureBox pictureBox1;
		private System.Windows.Forms.Label label1;
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

		private void DisplayCtlStateText(String text) {
			nxCtlState.Text = text;
		}
		
		bool StartApplication() {
			if (nxControl == null)
			{
				nxControl = new NuxeoProcess.NuxeoController();
				DisplayCtlStateText(null);
				nxControl.ProcessStarted += new ProcessStartedHandler(nxControl_ProcessStarted);
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

			DisplayCtlStateText("Nuxeo is starting ...");
			Process nxProcess=nxControl.getProcess();
			nxProcess.OutputDataReceived+=new DataReceivedEventHandler(OutputLog);
			nxProcess.BeginOutputReadLine();
			nxProcess.ErrorDataReceived+=new DataReceivedEventHandler(ErrorLog);
			nxProcess.BeginErrorReadLine();
			Log("Starting "+nxSvcName+" application...","WARN");
			return true;
		}

		void nxControl_ProcessStarted(object sender, EventArgs e)
		{
			if (CliUse)
            {
                Environment.Exit(0);
                this.Close();
            }
			
			String address = nxControl.nxEnv["NUXEO_BIND_ADDRESS"];
			if (address == "0.0.0.0") {
				address = "127.0.0.1";
			}
			
			DisplayCtlStateText(String.Format("Nuxeo is now accessible at http://{0}:8080", address));
		}

		bool StopService() {
			Log("Stopping "+nxSvcName+" service...","WARN");
			DisplayCtlStateText("");
			
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

				DisplayCtlStateText("");
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
				DisplayCtlStateText("");
				
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
				stopButton.Show();
				startButton.Enabled=false;
				startButton.Hide();
			}
		}

		void StopClick(object sender, System.EventArgs e)
		{
			bool statusChanged;
			if (nxService!=null) {
				statusChanged=StopService();
			} else {
				statusChanged=StopApplication();
				terminateButton.Enabled=true;
				terminateButton.Show();
			}
			if (statusChanged) {
				stopButton.Enabled=false;
			}
		}

		void TerminateClick(object sender, EventArgs e)
		{
			if (nxService==null) {
				TerminateApplication();
				terminateButton.Hide();
			}
		}

		void MainFormLoad(object sender, EventArgs e)
		{
			this.showLogsButton.Show();
			this.showLogsButton.Enabled=true;
			this.hideLogsButton.Hide();
			this.hideLogsButton.Enabled=false;
		}

		void HideLogsButtonClick(object sender, EventArgs e)
		{
			this.showLogsButton.Show();
			this.showLogsButton.Enabled=true;
			this.hideLogsButton.Hide();
			this.hideLogsButton.Enabled=false;
			this.Height-=400;
			this.logBox.Hide();
		}

		void ShowLogsButtonClick(object sender, EventArgs e)
		{
			this.showLogsButton.Hide();
			this.showLogsButton.Enabled=false;
			this.hideLogsButton.Show();
			this.hideLogsButton.Enabled=true;
			this.Height+=400;
			this.logBox.Show();
		}

	}
}
