;(function($){
/*
 * jquery.event.extendedclick.js
 * Version: 1.0
 *
 * Copyright (c) 2008, Minus Creative (http://minuscreative.com)
 * Dual licensed under the MIT (MIT-LICENSE.txt)
 * and GPL (GPL-LICENSE.txt) licenses.
 *
 * Created: 2008-10-01
 */

// jquery methods
$.fn.ctrlclick         = function(fn) { return this[fn ? "bind" : "trigger"]("ctrlclick", fn); };
$.fn.shiftclick        = function(fn) { return this[fn ? "bind" : "trigger"]("shiftclick", fn); };
$.fn.altclick          = function(fn) { return this[fn ? "bind" : "trigger"]("altclick", fn); };
$.fn.ctrlaltclick      = function(fn) { return this[fn ? "bind" : "trigger"]("ctrlaltclick", fn); };
$.fn.ctrlshiftclick    = function(fn) { return this[fn ? "bind" : "trigger"]("ctrlshiftclick", fn); };
$.fn.altshiftclick     = function(fn) { return this[fn ? "bind" : "trigger"]("altshiftclick", fn); };
$.fn.ctrlaltshiftclick = function(fn) { return this[fn ? "bind" : "trigger"]("ctrlaltshiftclick", fn); };

// all event clicks share the same config
$.event.special.ctrlclick         =
$.event.special.altclick          =
$.event.special.shiftclick        =
$.event.special.ctrlaltclick      =
$.event.special.ctrlshiftclick    =
$.event.special.altshiftclick     =
$.event.special.ctrlaltshiftclick = {
	setup: function() {
		$.event.add(this, extendedClickEvents, extendedClickHandler, {});
	},
	teardown: function() {
		$.event.remove(this, extendedClickEvents, extendedClickHandler);
	}
};

var extendedClickEvents = "click";


// Big shared event handler
function extendedClickHandler(event){
	if (event.ctrlKey)
	{
		if (event.shiftKey)
		{
			if (event.altKey || event.originalEvent.altKey)
			{
				event.type = "ctrlaltshiftclick"; // set to trigger
			}
			else
				event.type = "ctrlshiftclick"; // set to trigger
		}
		else if (event.altKey || event.originalEvent.altKey)
		{
			event.type = "ctrlaltclick"; // set to trigger
		}
		else
			event.type = "ctrlclick"; // set to trigger
	}
	else if (event.altKey || event.originalEvent.altKey)
	{
		if (event.shiftKey)
		{
			event.type = "altshiftclick"; // set to trigger
		}
		else
			event.type = "altclick"; // set to trigger
	}
	else if (event.shiftKey)
	{
		event.type = "shiftclick"; // set to trigger
	}
	return $.event.handle.call(this, event);
}
})(jQuery);