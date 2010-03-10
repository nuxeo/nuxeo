/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

function unhidePlayerAndPlay(videoPreviewId, videoPlayerId) {
	preview = document.getElementById(videoPreviewId);
	preview.style = "display: none";
	video = document.getElementById(videoPlayerId);
	video.style = "display: block";
	VideoController(videoPlayerId).play();
}

function VideoController(id) {
	this._movie = eval("document." + id);
	this._listenersRegistered = false;
	
	this._startTime = 0;
	this._endTime = this._movie.GetDuration();

	this.play = function() {
		this._movie.Play();
	}

	this.stop = function() {
		this._movie.Stop();
	}

	this.rewind = function() {
		this._movie.Rewind();
	}

	this._timeUnitToMillisecond = function(time) {
		return Math.round(time * 1000 / this._movie.GetTimeScale());
	}

	this._millisecondToTimeUnit = function(time) {
		return Math.round(time * this._movie.GetTimeScale() / 1000);
	}

	this.setSelection = function(from, to, enableEndListener) {
		if (!this._listenersRegistered) {
			this._registerListeners();
			this._listenersRegistered = true;
		}
		
		this._startTime = this._millisecondToTimeUnit(from);
		this._endTime = this._millisecondToTimeUnit(to);
		this._movie.SetTime(this._startTime);
		this._movie.SetStartTime(this._startTime);
		this._movie.SetEndTime(this._endTime);
		
		enableEndListener = enableEndListener || false;
		var controller = this;
		if (enableEndListener) {
			this._timer = setInterval(function() {
				controller._isSelectionPlayEnded();
			}, 10);
		}
	}
	
	this._registerListeners = function() {
		var controller = this;
        this._addListener("qt_ended", function() { controller._onEnded() });
        this._addListener("qt_timechanged", function() { controller._onTimeChanged() });
        this._addListener("qt_durationchange", function() { controller._onDurationChange() });
        this._addListener("qt_load", function() { controller._onLoad() });
		this._addListener("qt_error", function() { controller._onError() });
		this._addListener("qt_progress", function() { controller._onProgress() });
		this._addListener("qt_waiting", function() { controller._onWaiting() });
		this._addListener("qt_stalled", function() { controller._onStalled() });
		this._addListener("qt_begin", function() { controller._onBegin() });
		this._addListener("qt_loadedmetadata", function() { controller._onLoadedMetadata() });
		this._addListener("qt_loadedfirstframe", function() { controller._onLoadedFirstFrame() });
		this._addListener("qt_canplay", function() { controller._onCanPlay() });
		this._addListener("qt_canplaythrough", function() { controller._onCanPlayThrough() });
		this._addListener("qt_pause", function() { controller._onPause() });
		this._addListener("qt_play", function() { controller._onPlay() });
	}
	
	this._isSelectionPlayEnded = function() {
		time = this._movie.GetTime();
		if (time >= this._endTime) {
			this.clearSelection();
			this.stop();
		}
	}
	
	this._addListener = function(event, handler) {
	    if (document.addEventListener) {
       	    this._movie.addEventListener(event, handler, false);
        } else {
            this._movie.attachEvent('on' + event, handler);
       }
	}
	
	this._onEnded = function() {
		this.clearSelection();
	}
	
	this._onDurationChange = function() {
		this._movie.SetStartTime(this._startTime);
		this._movie.SetEndTime(this._endTime);
	}
	
	this._onLoad = function() {
	}
	
	this._onError = function() {
	}
	
	this._onProgress = function() {
	}
	
	this._onWaiting = function() {
	}
	
	this._onStalled = function() {
	}
	
	this._onBegin = function() {
	}
	
	this._onLoadedMetadata = function() {
	}

	this._onLoadedFirstFrame = function() {
	}
	
	this._onCanPlay = function() {
	}
	
	this._onCanPlayThrough = function() {
	}
	
	this._onPause = function() {
	}
	
	this._onPlay = function() {
	}
	
	this._onTimeChanged = function() {
		time = this._movie.GetTime();
		if (time < this._startTime || time > this._endTime) {
			this.clearSelection();
		}
	}

	this.clearSelection = function() {
        //console.log("clearSelection");
		clearInterval(this._timer);
		this._startTime = 0;
		this._endTime = this._movie.GetDuration();
		this._movie.SetStartTime(this._startTime);
		this._movie.SetEndTime(this._endTime);
	}

	this.getTime = function() {
		return this._timeUnitToMillisecond(this._movie.GetTime());
	}

	this.setTime = function(time) {
		this._movie.SetTime(this._millisecondToTimeUnit(time));
	}

	this.getDuration = function() {
		return this._timeUnitToMillisecond(this._movie.GetDuration());
	}

}
