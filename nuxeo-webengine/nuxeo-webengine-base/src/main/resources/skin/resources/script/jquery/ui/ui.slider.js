/*
 * jQuery UI Slider
 *
 * Copyright (c) 2008 Paul Bakaus
 * Dual licensed under the MIT (MIT-LICENSE.txt)
 * and GPL (GPL-LICENSE.txt) licenses.
 *
 * http://docs.jquery.com/UI/Slider
 *
 * Depends:
 *   ui.base.js
 *
 * Revision: $Id: ui.slider.js 5219 2008-04-09 20:16:05Z braeker $
 */
;(function($) {

	$.fn.extend({
		slider: function(options) {
			var args = Array.prototype.slice.call(arguments, 1);

			if ( options == "value" )
				return $.data(this[0], "slider").value(arguments[1]);

			return this.each(function() {
				if (typeof options == "string") {
					var slider = $.data(this, "slider");
					if (slider) slider[options].apply(slider, args);

				} else if(!$.data(this, "slider"))
					new $.ui.slider(this, options);
			});
		}
	});

	$.ui.slider = function(element, options) {

		//Initialize needed constants
		var self = this;
		this.element = $(element);
		$.data(element, "slider", this);
		this.element.addClass("ui-slider");

		//Prepare the passed options
		this.options = $.extend({}, $.ui.slider.defaults, options);
		var o = this.options;
		$.extend(o, {
			axis: o.axis || (element.offsetWidth < element.offsetHeight ? 'vertical' : 'horizontal'),
			max: !isNaN(parseInt(o.max,10)) ? { x: parseInt(o.max, 10), y: parseInt(o.max, 10)  } : ({ x: o.max && o.max.x || 100, y:  o.max && o.max.y || 100 }),
			min: !isNaN(parseInt(o.min,10)) ? { x: parseInt(o.min, 10), y: parseInt(o.min, 10)  } : ({ x: o.min && o.min.x || 0, y:  o.min && o.min.y || 0 })
		});

		//Prepare the real maxValue
		o.realMax = {
			x: o.max.x - o.min.x,
			y: o.max.y - o.min.y
		};

		//Calculate stepping based on steps
		o.stepping = {
			x: o.stepping && o.stepping.x || parseInt(o.stepping, 10) || (o.steps && o.steps.x ? o.realMax.x/o.steps.x : 0),
			y: o.stepping && o.stepping.y || parseInt(o.stepping, 10) || (o.steps && o.steps.y ? o.realMax.y/o.steps.y : 0)
		};

		$(element).bind("setData.slider", function(event, key, value){
			self.options[key] = value;
		}).bind("getData.slider", function(event, key){
			return self.options[key];
		});

		//Initialize mouse and key events for interaction
		this.handle = $(o.handle, element);
		if (!this.handle.length) {
			self.handle = self.generated = $(o.handles || [0]).map(function() {
				var handle = $("<div/>").addClass("ui-slider-handle").appendTo(element);
				if (this.id)
					handle.attr("id", this.id);
				return handle[0];
			});
		}
		$(this.handle)
			.mouseInteraction({
				executor: this,
				delay: o.delay,
				distance: o.distance != undefined ? o.distance : 1,
				dragPrevention: o.prevention ? o.prevention.toLowerCase().split(',') : ['input','textarea','button','select','option'],
				start: this.start,
				stop: this.stop,
				drag: this.drag,
				condition: function(e, handle) {
					if(!this.disabled) {
						if(this.currentHandle) this.blur(this.currentHandle);
						this.focus(handle,1);
						return !this.disabled;
					}
				}
			})
			.wrap('<a href="javascript:void(0)" style="cursor:default;"></a>')
			.parent()
				.bind('focus', function(e) { self.focus(this.firstChild); })
				.bind('blur', function(e) { self.blur(this.firstChild); })
				.bind('keydown', function(e) {
					if(/(37|38|39|40)/.test(e.keyCode)) {
						self.moveTo({
							x: /(37|39)/.test(e.keyCode) ? (e.keyCode == 37 ? '-' : '+') + '=' + self.oneStep(1) : null,
							y: /(38|40)/.test(e.keyCode) ? (e.keyCode == 38 ? '-' : '+') + '=' + self.oneStep(2) : null
						}, this.firstChild);
					}
				})
		;

		//Prepare dynamic properties for later use
		this.actualSize = { width: this.element.outerWidth() , height: this.element.outerHeight() };

		//Bind the click to the slider itself
		this.element.bind('mousedown.slider', function(e) {
			self.click.apply(self, [e]);
			self.currentHandle.data("ui-mouse").trigger(e);
			self.firstValue = self.firstValue + 1; //This is for always triggering the change event
		});

		//Move the first handle to the startValue
		$.each(o.handles || [], function(index, handle) {
			self.moveTo(handle.start, index, true);
		});
		if (!isNaN(o.startValue))
			this.moveTo(o.startValue, 0, true);

		//If we only have one handle, set the previous handle to this one to allow clicking before selecting the handle
		if(this.handle.length == 1) this.previousHandle = this.handle;
		if(this.handle.length == 2 && o.range) this.createRange();

	};

	$.extend($.ui.slider.prototype, {
		plugins: {},
		createRange: function() {
			this.rangeElement = $('<div></div>')
				.addClass('ui-slider-range')
				.css({ position: 'absolute' })
				.appendTo(this.element);
			this.updateRange();
		},
		updateRange: function() {
				var prop = this.options.axis == "vertical" ? "top" : "left";
				var size = this.options.axis == "vertical" ? "height" : "width";
				this.rangeElement.css(prop, parseInt($(this.handle[0]).css(prop),10) + this.handleSize(0, this.options.axis == "vertical" ? 2 : 1)/2);
				this.rangeElement.css(size, parseInt($(this.handle[1]).css(prop),10) - parseInt($(this.handle[0]).css(prop),10));
		},
		getRange: function() {
			return this.rangeElement ? this.convertValue(parseInt(this.rangeElement.css(this.options.axis == "vertical" ? "height" : "width"),10)) : null;
		},
		ui: function(e) {
			return {
				instance: this,
				options: this.options,
				handle: this.currentHandle,
				value: this.options.axis != "both" || !this.options.axis ? Math.round(this.value(null,this.options.axis == "vertical" ? 2 : 1)) : {
					x: Math.round(this.value(null,1)),
					y: Math.round(this.value(null,2))
				},
				range: this.getRange()
			};
		},
		propagate: function(n,e) {
			$.ui.plugin.call(this, n, [e, this.ui()]);
			this.element.triggerHandler(n == "slide" ? n : "slide"+n, [e, this.ui()], this.options[n]);
		},
		destroy: function() {
			this.element
				.removeClass("ui-slider ui-slider-disabled")
				.removeData("slider")
				.unbind(".slider");
			this.handle.removeMouseInteraction();
			this.generated && this.generated.remove();
		},
		enable: function() {
			this.element.removeClass("ui-slider-disabled");
			this.disabled = false;
		},
		disable: function() {
			this.element.addClass("ui-slider-disabled");
			this.disabled = true;
		},
		focus: function(handle,hard) {
			this.currentHandle = $(handle).addClass('ui-slider-handle-active');
			if(hard) this.currentHandle.parent()[0].focus();
		},
		blur: function(handle) {
			$(handle).removeClass('ui-slider-handle-active');
			if(this.currentHandle && this.currentHandle[0] == handle) { this.previousHandle = this.currentHandle; this.currentHandle = null; };
		},
		value: function(handle, axis) {
			if(this.handle.length == 1) this.currentHandle = this.handle;
			if(!axis) axis = this.options.axis == "vertical" ? 2 : 1;

			var value = ((parseInt($(handle != undefined && handle !== null ? this.handle[handle] || handle : this.currentHandle).css(axis == 1 ? "left" : "top"),10) / (this.actualSize[axis == 1 ? "width" : "height"] - this.handleSize(null,axis))) * this.options.realMax[axis == 1 ? "x" : "y"]) + this.options.min[axis == 1 ? "x" : "y"];

			var o = this.options;
			if (o.stepping[axis == 1 ? "x" : "y"]) {
			    value = Math.round(value / o.stepping[axis == 1 ? "x" : "y"]) * o.stepping[axis == 1 ? "x" : "y"];
			}
			return value;
		},
		convertValue: function(value,axis) {
			if(!axis) axis = this.options.axis == "vertical" ? 2 : 1;
			return this.options.min[axis == 1 ? "x" : "y"] + (value / (this.actualSize[axis == 1 ? "width" : "height"] - this.handleSize(null,axis))) * this.options.realMax[axis == 1 ? "x" : "y"];
		},
		translateValue: function(value,axis) {
			if(!axis) axis = this.options.axis == "vertical" ? 2 : 1;
			return ((value - this.options.min[axis == 1 ? "x" : "y"]) / this.options.realMax[axis == 1 ? "x" : "y"]) * (this.actualSize[axis == 1 ? "width" : "height"] - this.handleSize(null,axis));
		},
		handleSize: function(handle,axis) {
			if(!axis) axis = this.options.axis == "vertical" ? 2 : 1;
			return $(handle != undefined && handle !== null ? this.handle[handle] : this.currentHandle)[axis == 1 ? "outerWidth" : "outerHeight"]();
		},
		click: function(e) {

			// This method is only used if:
			// - The user didn't click a handle
			// - The Slider is not disabled
			// - There is a current, or previous selected handle (otherwise we wouldn't know which one to move)
			var pointer = [e.pageX,e.pageY];
			var clickedHandle = false; this.handle.each(function() { if(this == e.target) clickedHandle = true;  });
			if(clickedHandle || this.disabled || !(this.currentHandle || this.previousHandle)) return;

			//If a previous handle was focussed, focus it again
			if(this.previousHandle) this.focus(this.previousHandle, 1);

			//Move focussed handle to the clicked position
			this.offset = this.element.offset();
			this.moveTo({
				y: this.convertValue(e.pageY - this.offset.top - this.currentHandle.outerHeight()/2),
				x: this.convertValue(e.pageX - this.offset.left - this.currentHandle.outerWidth()/2)
			}, null, true);
		},
		start: function(e, handle) {

			var o = this.options;
			if(!this.currentHandle) this.focus(this.previousHandle, true); //This is a especially ugly fix for strange blur events happening on mousemove events

			this.offset = this.element.offset();
			this.handleOffset = this.currentHandle.offset();
			this.clickOffset = { top: e.pageY - this.handleOffset.top, left: e.pageX - this.handleOffset.left };
			this.firstValue = this.value();

			this.propagate('start', e);
			return false;

		},
		stop: function(e) {
			this.propagate('stop', e);
			if (this.firstValue != this.value())
				this.propagate('change', e);
			this.focus(this.currentHandle, true); //This is a especially ugly fix for strange blur events happening on mousemove events
			return false;
		},

		oneStep: function(axis) {
			if(!axis) axis = this.options.axis == "vertical" ? 2 : 1;
			return this.options.stepping[axis == 1 ? "x" : "y"] ? this.options.stepping[axis == 1 ? "x" : "y"] : (this.options.realMax[axis == 1 ? "x" : "y"] / this.actualSize[axis == 1 ? "width" : "height"]) * 5;
		},

		translateRange: function(value,axis) {
			if (this.rangeElement) {
				if (this.currentHandle[0] == this.handle[0] && value >= this.translateValue(this.value(1),axis))
					value = this.translateValue(this.value(1,axis) - this.oneStep(axis), axis);
				if (this.currentHandle[0] == this.handle[1] && value <= this.translateValue(this.value(0),axis))
					value = this.translateValue(this.value(0,axis) + this.oneStep(axis));
			}
			if (this.options.handles) {
				var handle = this.options.handles[this.handleIndex()];
				if (value < this.translateValue(handle.min,axis)) {
					value = this.translateValue(handle.min,axis);
				} else if (value > this.translateValue(handle.max,axis)) {
					value = this.translateValue(handle.max,axis);
				}
			}
			return value;
		},

		handleIndex: function() {
			return this.handle.index(this.currentHandle[0])
		},

		translateLimits: function(value,axis) {
			if(!axis) axis = this.options.axis == "vertical" ? 2 : 1;
			if (value >= this.actualSize[axis == 1 ? "width" : "height"] - this.handleSize(null,axis))
				value = this.actualSize[axis == 1 ? "width" : "height"] - this.handleSize(null,axis);
			if (value <= 0)
				value = 0;
			return value;
		},

		drag: function(e, handle) {

			var o = this.options;
			var position = { top: e.pageY - this.offset.top - this.clickOffset.top, left: e.pageX - this.offset.left - this.clickOffset.left};
			if(!this.currentHandle) this.focus(this.previousHandle, true); //This is a especially ugly fix for strange blur events happening on mousemove events

			position.left = this.translateLimits(position.left,1);
			position.top = this.translateLimits(position.top,2);

			if (o.stepping.x) {
				var value = this.convertValue(position.left,1);
				value = Math.round(value / o.stepping.x) * o.stepping.x;
				position.left = this.translateValue(value, 1);
			}
			if (o.stepping.y) {
				var value = this.convertValue(position.top,2);
				value = Math.round(value / o.stepping.y) * o.stepping.y;
				position.top = this.translateValue(value, 2);
			}

			position.left = this.translateRange(position.left, 1);
			position.top = this.translateRange(position.top, 2);

			if(o.axis != "vertical") this.currentHandle.css({ left: position.left });
			if(o.axis != "horizontal") this.currentHandle.css({ top: position.top });

			if (this.rangeElement)
				this.updateRange();
			this.propagate('slide', e);
			return false;
		},

		moveTo: function(value, handle, noPropagation) {
			var o = this.options;
			if (handle == undefined && !this.currentHandle && this.handle.length != 1)
				return false; //If no handle has been passed, no current handle is available and we have multiple handles, return false
			if (handle == undefined && !this.currentHandle)
				handle = 0; //If only one handle is available, use it
			if (handle != undefined)
				this.currentHandle = this.previousHandle = $(this.handle[handle] || handle);



			if(value.x !== undefined && value.y !== undefined) {
				var x = value.x;
				var y = value.y;
			} else {
				var x = value, y = value;
			}

			if(x && x.constructor != Number) {
				var me = /^\-\=/.test(x), pe = /^\+\=/.test(x);
				if (me) {
					x = this.value(null,1) - parseInt(x.replace('-=', ''), 10);
				} else if (pe) {
					x = this.value(null,1) + parseInt(x.replace('+=', ''), 10);
				}
			}

			if(y && y.constructor != Number) {
				var me = /^\-\=/.test(y), pe = /^\+\=/.test(y);
				if (me) {
					y = this.value(null,2) - parseInt(y.replace('-=', ''), 10);
				} else if (pe) {
					y = this.value(null,2) + parseInt(y.replace('+=', ''), 10);
				}
			}

			if(o.axis != "vertical" && x) {
				if(o.stepping.x) x = Math.round(x / o.stepping.x) * o.stepping.x;
				x = this.translateValue(x, 1);
				x = this.translateLimits(x, 1);
				x = this.translateRange(x, 1);
				this.currentHandle.css({ left: x });
			}

			if(o.axis != "horizontal" && y) {
				if(o.stepping.y) y = Math.round(y / o.stepping.y) * o.stepping.y;
				y = this.translateValue(y, 2);
				y = this.translateLimits(y, 2);
				y = this.translateRange(y, 2);
				this.currentHandle.css({ top: y });
			}

			if (this.rangeElement)
				this.updateRange();

			if (!noPropagation) {
				this.propagate('start', null);
				this.propagate('stop', null);
				this.propagate('change', null);
				this.propagate("slide", null);
			}
		}
	});

	$.ui.slider.defaults = {
		handle: ".ui-slider-handle"
	};

})(jQuery);
