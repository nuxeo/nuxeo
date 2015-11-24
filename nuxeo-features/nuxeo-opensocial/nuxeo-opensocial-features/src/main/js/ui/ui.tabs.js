/*
 * jQuery UI Tabs
 *
 * Copyright (c) 2007, 2008 Klaus Hartl (stilbuero.de)
 * Dual licensed under the MIT (MIT-LICENSE.txt)
 * and GPL (GPL-LICENSE.txt) licenses.
 *
 * http://docs.jquery.com/UI/Tabs
 *
 * Depends:
 *	ui.core.js
 */
(function(jQuery) {

jQuery.widget("ui.tabs", {
	init: function() {
		this.options.event += '.tabs'; // namespace event
		
		// create tabs
		this.tabify(true);
	},
	setData: function(key, value) {
		if ((/^selected/).test(key))
			this.select(value);
		else {
			this.options[key] = value;
			this.tabify();
		}
	},
	length: function() {
		return this.jQuerytabs.length;
	},
	tabId: function(a) {
		return a.title && a.title.replace(/\s/g, '_').replace(/[^A-Za-z0-9\-_:\.]/g, '')
			|| this.options.idPrefix + jQuery.data(a);
	},
	ui: function(tab, panel) {
		return {
			options: this.options,
			tab: tab,
			panel: panel,
			index: this.jQuerytabs.index(tab)
		};
	},
	tabify: function(init) {

		this.jQuerylis = jQuery('li:has(a[href])', this.element);
		this.jQuerytabs = this.jQuerylis.map(function() { return jQuery('a', this)[0]; });
		this.jQuerypanels = jQuery([]);

		var self = this, o = this.options;

		this.jQuerytabs.each(function(i, a) {
			// inline tab
			if (a.hash && a.hash.replace('#', '')) // Safari 2 reports '#' for an empty hash
				self.jQuerypanels = self.jQuerypanels.add(a.hash);
			// remote tab
			else if (jQuery(a).attr('href') != '#') { // prevent loading the page itself if href is just "#"
				jQuery.data(a, 'href.tabs', a.href); // required for restore on destroy
				jQuery.data(a, 'load.tabs', a.href); // mutable
				var id = self.tabId(a);
				a.href = '#' + id;
				var jQuerypanel = jQuery('#' + id);
				if (!jQuerypanel.length) {
					jQuerypanel = jQuery(o.panelTemplate).attr('id', id).addClass(o.panelClass)
						.insertAfter( self.jQuerypanels[i - 1] || self.element );
					jQuerypanel.data('destroy.tabs', true);
				}
				self.jQuerypanels = self.jQuerypanels.add( jQuerypanel );
			}
			// invalid tab href
			else
				o.disabled.push(i + 1);
		});

		if (init) {

			// attach necessary classes for styling if not present
			this.element.addClass(o.navClass);
			this.jQuerypanels.each(function() {
				var jQuerythis = jQuery(this);
				jQuerythis.addClass(o.panelClass);
			});

			// Selected tab
			// use "selected" option or try to retrieve:
			// 1. from fragment identifier in url
			// 2. from cookie
			// 3. from selected class attribute on <li>
			if (o.selected === undefined) {
				if (location.hash) {
					this.jQuerytabs.each(function(i, a) {
						if (a.hash == location.hash) {
							o.selected = i;
							// prevent page scroll to fragment
							if (jQuery.browser.msie || jQuery.browser.opera) { // && !o.remote
								var jQuerytoShow = jQuery(location.hash), toShowId = jQuerytoShow.attr('id');
								jQuerytoShow.attr('id', '');
								setTimeout(function() {
									jQuerytoShow.attr('id', toShowId); // restore id
								}, 500);
							}
							scrollTo(0, 0);
							return false; // break
						}
					});
				}
				else if (o.cookie) {
					var index = parseInt(jQuery.cookie('ui-tabs' + jQuery.data(self.element)),10);
					if (index && self.jQuerytabs[index])
						o.selected = index;
				}
				else if (self.jQuerylis.filter('.' + o.selectedClass).length)
					o.selected = self.jQuerylis.index( self.jQuerylis.filter('.' + o.selectedClass)[0] );
			}
			o.selected = o.selected === null || o.selected !== undefined ? o.selected : 0; // first tab selected by default

			// Take disabling tabs via class attribute from HTML
			// into account and update option properly.
			// A selected tab cannot become disabled.
			o.disabled = jQuery.unique(o.disabled.concat(
				jQuery.map(this.jQuerylis.filter('.' + o.disabledClass),
					function(n, i) { return self.jQuerylis.index(n); } )
			)).sort();
			if (jQuery.inArray(o.selected, o.disabled) != -1)
				o.disabled.splice(jQuery.inArray(o.selected, o.disabled), 1);
			
			// highlight selected tab
			this.jQuerypanels.addClass(o.hideClass);
			this.jQuerylis.removeClass(o.selectedClass);
			if (o.selected !== null) {
				this.jQuerypanels.eq(o.selected).show().removeClass(o.hideClass); // use show and remove class to show in any case no matter how it has been hidden before
				this.jQuerylis.eq(o.selected).addClass(o.selectedClass);
				
				// seems to be expected behavior that the show callback is fired
				var onShow = function() {
					jQuery(self.element).triggerHandler('tabsshow',
						[self.fakeEvent('tabsshow'), self.ui(self.jQuerytabs[o.selected], self.jQuerypanels[o.selected])], o.show);
				}; 

				// load if remote tab
				if (jQuery.data(this.jQuerytabs[o.selected], 'load.tabs'))
					this.load(o.selected, onShow);
				// just trigger show event
				else
					onShow();
				
			}
			
			// clean up to avoid memory leaks in certain versions of IE 6
			jQuery(window).bind('unload', function() {
				self.jQuerytabs.unbind('.tabs');
				self.jQuerylis = self.jQuerytabs = self.jQuerypanels = null;
			});

		}

		// disable tabs
		for (var i = 0, li; li = this.jQuerylis[i]; i++)
			jQuery(li)[jQuery.inArray(i, o.disabled) != -1 && !jQuery(li).hasClass(o.selectedClass) ? 'addClass' : 'removeClass'](o.disabledClass);

		// reset cache if switching from cached to not cached
		if (o.cache === false)
			this.jQuerytabs.removeData('cache.tabs');
		
		// set up animations
		var hideFx, showFx, baseFx = { 'min-width': 0, duration: 1 }, baseDuration = 'normal';
		if (o.fx && o.fx.constructor == Array)
			hideFx = o.fx[0] || baseFx, showFx = o.fx[1] || baseFx;
		else
			hideFx = showFx = o.fx || baseFx;

		// reset some styles to maintain print style sheets etc.
		var resetCSS = { display: '', overflow: '', height: '' };
		if (!jQuery.browser.msie) // not in IE to prevent ClearType font issue
			resetCSS.opacity = '';

		// Hide a tab, animation prevents browser scrolling to fragment,
		// jQueryshow is optional.
		function hideTab(clicked, jQueryhide, jQueryshow) {
			jQueryhide.animate(hideFx, hideFx.duration || baseDuration, function() { //
				jQueryhide.addClass(o.hideClass).css(resetCSS); // maintain flexible height and accessibility in print etc.
				if (jQuery.browser.msie && hideFx.opacity)
					jQueryhide[0].style.filter = '';
				if (jQueryshow)
					showTab(clicked, jQueryshow, jQueryhide);
			});
		}

		// Show a tab, animation prevents browser scrolling to fragment,
		// jQueryhide is optional.
		function showTab(clicked, jQueryshow, jQueryhide) {
			if (showFx === baseFx)
				jQueryshow.css('display', 'block'); // prevent occasionally occuring flicker in Firefox cause by gap between showing and hiding the tab panels
			jQueryshow.animate(showFx, showFx.duration || baseDuration, function() {
				jQueryshow.removeClass(o.hideClass).css(resetCSS); // maintain flexible height and accessibility in print etc.
				if (jQuery.browser.msie && showFx.opacity)
					jQueryshow[0].style.filter = '';

				// callback
				jQuery(self.element).triggerHandler('tabsshow',
					[self.fakeEvent('tabsshow'), self.ui(clicked, jQueryshow[0])], o.show);

			});
		}

		// switch a tab
		function switchTab(clicked, jQueryli, jQueryhide, jQueryshow) {
			/*if (o.bookmarkable && trueClick) { // add to history only if true click occured, not a triggered click
				jQuery.ajaxHistory.update(clicked.hash);
			}*/
			jQueryli.addClass(o.selectedClass)
				.siblings().removeClass(o.selectedClass);
			hideTab(clicked, jQueryhide, jQueryshow);
		}

		// attach tab event handler, unbind to avoid duplicates from former tabifying...
		this.jQuerytabs.unbind('.tabs').bind(o.event, function() {

			//var trueClick = e.clientX; // add to history only if true click occured, not a triggered click
			var jQueryli = jQuery(this).parents('li:eq(0)'),
				jQueryhide = self.jQuerypanels.filter(':visible'),
				jQueryshow = jQuery(this.hash);

			// If tab is already selected and not unselectable or tab disabled or 
			// or is already loading or click callback returns false stop here.
			// Check if click handler returns false last so that it is not executed
			// for a disabled or loading tab!
			if ((jQueryli.hasClass(o.selectedClass) && !o.unselect)
				|| jQueryli.hasClass(o.disabledClass) 
				|| jQuery(this).hasClass(o.loadingClass)
				|| jQuery(self.element).triggerHandler('tabsselect', [self.fakeEvent('tabsselect'), self.ui(this, jQueryshow[0])], o.select) === false
				) {
				this.blur();
				return false;
			}

			self.options.selected = self.jQuerytabs.index(this);

			// if tab may be closed
			if (o.unselect) {
				if (jQueryli.hasClass(o.selectedClass)) {
					self.options.selected = null;
					jQueryli.removeClass(o.selectedClass);
					self.jQuerypanels.stop();
					hideTab(this, jQueryhide);
					this.blur();
					return false;
				} else if (!jQueryhide.length) {
					self.jQuerypanels.stop();
					var a = this;
					self.load(self.jQuerytabs.index(this), function() {
						jQueryli.addClass(o.selectedClass).addClass(o.unselectClass);
						showTab(a, jQueryshow);
					});
					this.blur();
					return false;
				}
			}

			if (o.cookie)
				jQuery.cookie('ui-tabs' + jQuery.data(self.element), self.options.selected, o.cookie);

			// stop possibly running animations
			self.jQuerypanels.stop();

			// show new tab
			if (jQueryshow.length) {

				// prevent scrollbar scrolling to 0 and than back in IE7, happens only if bookmarking/history is enabled
				/*if (jQuery.browser.msie && o.bookmarkable) {
					var showId = this.hash.replace('#', '');
					jQueryshow.attr('id', '');
					setTimeout(function() {
						jQueryshow.attr('id', showId); // restore id
					}, 0);
				}*/

				var a = this;
				self.load(self.jQuerytabs.index(this), jQueryhide.length ? 
					function() {
						switchTab(a, jQueryli, jQueryhide, jQueryshow);
					} :
					function() {
						jQueryli.addClass(o.selectedClass);
						showTab(a, jQueryshow);
					}
				);

				// Set scrollbar to saved position - need to use timeout with 0 to prevent browser scroll to target of hash
				/*var scrollX = window.pageXOffset || document.documentElement && document.documentElement.scrollLeft || document.body.scrollLeft || 0;
				var scrollY = window.pageYOffset || document.documentElement && document.documentElement.scrollTop || document.body.scrollTop || 0;
				setTimeout(function() {
					scrollTo(scrollX, scrollY);
				}, 0);*/

			} else
				throw 'jQuery UI Tabs: Mismatching fragment identifier.';

			// Prevent IE from keeping other link focussed when using the back button
			// and remove dotted border from clicked link. This is controlled in modern
			// browsers via CSS, also blur removes focus from address bar in Firefox
			// which can become a usability and annoying problem with tabsRotate.
			if (jQuery.browser.msie)
				this.blur();

			//return o.bookmarkable && !!trueClick; // convert trueClick == undefined to Boolean required in IE
			return false;

		});

		// disable click if event is configured to something else
		if (!(/^click/).test(o.event))
			this.jQuerytabs.bind('click.tabs', function() { return false; });

	},
	add: function(url, label, index) {
		if (index == undefined) 
			index = this.jQuerytabs.length; // append by default

		var o = this.options;
		var jQueryli = jQuery(o.tabTemplate.replace(/#\{href\}/g, url).replace(/#\{label\}/g, label));
		jQueryli.data('destroy.tabs', true);

		var id = url.indexOf('#') == 0 ? url.replace('#', '') : this.tabId( jQuery('a:first-child', jQueryli)[0] );

		// try to find an existing element before creating a new one
		var jQuerypanel = jQuery('#' + id);
		if (!jQuerypanel.length) {
			jQuerypanel = jQuery(o.panelTemplate).attr('id', id)
				.addClass(o.hideClass)
				.data('destroy.tabs', true);
		}
		jQuerypanel.addClass(o.panelClass);
		if (index >= this.jQuerylis.length) {
			jQueryli.appendTo(this.element);
			jQuerypanel.appendTo(this.element[0].parentNode);
		} else {
			jQueryli.insertBefore(this.jQuerylis[index]);
			jQuerypanel.insertBefore(this.jQuerypanels[index]);
		}
		
		o.disabled = jQuery.map(o.disabled,
			function(n, i) { return n >= index ? ++n : n });
			
		this.tabify();

		if (this.jQuerytabs.length == 1) {
			jQueryli.addClass(o.selectedClass);
			jQuerypanel.removeClass(o.hideClass);
			var href = jQuery.data(this.jQuerytabs[0], 'load.tabs');
			if (href)
				this.load(index, href);
		}

		// callback
		this.element.triggerHandler('tabsadd',
			[this.fakeEvent('tabsadd'), this.ui(this.jQuerytabs[index], this.jQuerypanels[index])], o.add
		);
	},
	remove: function(index) {
		var o = this.options, jQueryli = this.jQuerylis.eq(index).remove(),
			jQuerypanel = this.jQuerypanels.eq(index).remove();

		// If selected tab was removed focus tab to the right or
		// in case the last tab was removed the tab to the left.
		if (jQueryli.hasClass(o.selectedClass) && this.jQuerytabs.length > 1)
			this.select(index + (index + 1 < this.jQuerytabs.length ? 1 : -1));

		o.disabled = jQuery.map(jQuery.grep(o.disabled, function(n, i) { return n != index; }),
			function(n, i) { return n >= index ? --n : n });

		this.tabify();

		// callback
		this.element.triggerHandler('tabsremove',
			[this.fakeEvent('tabsremove'), this.ui(jQueryli.find('a')[0], jQuerypanel[0])], o.remove
		);
	},
	enable: function(index) {
		var o = this.options;
		if (jQuery.inArray(index, o.disabled) == -1)
			return;
			
		var jQueryli = this.jQuerylis.eq(index).removeClass(o.disabledClass);
		if (jQuery.browser.safari) { // fix disappearing tab (that used opacity indicating disabling) after enabling in Safari 2...
			jQueryli.css('display', 'inline-block');
			setTimeout(function() {
				jQueryli.css('display', 'block');
			}, 0);
		}

		o.disabled = jQuery.grep(o.disabled, function(n, i) { return n != index; });

		// callback
		this.element.triggerHandler('tabsenable',
			[this.fakeEvent('tabsenable'), this.ui(this.jQuerytabs[index], this.jQuerypanels[index])], o.enable
		);

	},
	disable: function(index) {
		var self = this, o = this.options;
		if (index != o.selected) { // cannot disable already selected tab
			this.jQuerylis.eq(index).addClass(o.disabledClass);

			o.disabled.push(index);
			o.disabled.sort();

			// callback
			this.element.triggerHandler('tabsdisable',
				[this.fakeEvent('tabsdisable'), this.ui(this.jQuerytabs[index], this.jQuerypanels[index])], o.disable
			);
		}
	},
	select: function(index) {
		if (typeof index == 'string')
			index = this.jQuerytabs.index( this.jQuerytabs.filter('[hrefjQuery=' + index + ']')[0] );
		this.jQuerytabs.eq(index).trigger(this.options.event);
	},
	load: function(index, callback) { // callback is for internal usage only
		
		var self = this, o = this.options, jQuerya = this.jQuerytabs.eq(index), a = jQuerya[0],
				bypassCache = callback == undefined || callback === false, url = jQuerya.data('load.tabs');

		callback = callback || function() {};
		
		// no remote or from cache - just finish with callback
		if (!url || !bypassCache && jQuery.data(a, 'cache.tabs')) {
			callback();
			return;
		}

		// load remote from here on
		
		var inner = function(parent) {
			var jQueryparent = jQuery(parent), jQueryinner = jQueryparent.find('*:last');
			return jQueryinner.length && jQueryinner.is(':not(img)') && jQueryinner || jQueryparent;
		};
		var cleanup = function() {
			self.jQuerytabs.filter('.' + o.loadingClass).removeClass(o.loadingClass)
						.each(function() {
							if (o.spinner)
								inner(this).parent().html(inner(this).data('label.tabs'));
						});
			self.xhr = null;
		};
		
		if (o.spinner) {
			var label = inner(a).html();
			inner(a).wrapInner('<em></em>')
				.find('em').data('label.tabs', label).html(o.spinner);
		}

		var ajaxOptions = jQuery.extend({}, o.ajaxOptions, {
			url: url,
			success: function(r, s) {
				jQuery(a.hash).html(r);
				cleanup();
				
				if (o.cache)
					jQuery.data(a, 'cache.tabs', true); // if loaded once do not load them again

				// callbacks
				jQuery(self.element).triggerHandler('tabsload',
					[self.fakeEvent('tabsload'), self.ui(self.jQuerytabs[index], self.jQuerypanels[index])], o.load
				);
				o.ajaxOptions.success && o.ajaxOptions.success(r, s);
				
				// This callback is required because the switch has to take
				// place after loading has completed. Call last in order to 
				// fire load before show callback...
				callback();
			}
		});
		if (this.xhr) {
			// terminate pending requests from other tabs and restore tab label
			this.xhr.abort();
			cleanup();
		}
		jQuerya.addClass(o.loadingClass);
		setTimeout(function() { // timeout is again required in IE, "wait" for id being restored
			self.xhr = jQuery.ajax(ajaxOptions);
		}, 0);

	},
	url: function(index, url) {
		this.jQuerytabs.eq(index).removeData('cache.tabs').data('load.tabs', url);
	},
	destroy: function() {
		var o = this.options;
		this.element.unbind('.tabs')
			.removeClass(o.navClass).removeData('tabs');
		this.jQuerytabs.each(function() {
			var href = jQuery.data(this, 'href.tabs');
			if (href)
				this.href = href;
			var jQuerythis = jQuery(this).unbind('.tabs');
			jQuery.each(['href', 'load', 'cache'], function(i, prefix) {
				jQuerythis.removeData(prefix + '.tabs');
			});
		});
		this.jQuerylis.add(this.jQuerypanels).each(function() {
			if (jQuery.data(this, 'destroy.tabs'))
				jQuery(this).remove();
			else
				jQuery(this).removeClass([o.selectedClass, o.unselectClass,
					o.disabledClass, o.panelClass, o.hideClass].join(' '));
		});
	},
	fakeEvent: function(type) {
		return jQuery.event.fix({
			type: type,
			target: this.element[0]
		});
	}
});

jQuery.ui.tabs.defaults = {
	// basic setup
	unselect: false,
	event: 'click',
	disabled: [],
	cookie: null, // e.g. { expires: 7, path: '/', domain: 'jquery.com', secure: true }
	// TODO history: false,

	// Ajax
	spinner: 'Loading&#8230;',
	cache: false,
	idPrefix: 'ui-tabs-',
	ajaxOptions: {},

	// animations
	fx: null, // e.g. { height: 'toggle', opacity: 'toggle', duration: 200 }

	// templates
	tabTemplate: '<li><a href="#{href}"><span>#{label}</span></a></li>',
	panelTemplate: '<div></div>',

	// CSS classes
	navClass: 'ui-tabs-nav',
	selectedClass: 'ui-tabs-selected',
	unselectClass: 'ui-tabs-unselect',
	disabledClass: 'ui-tabs-disabled',
	panelClass: 'ui-tabs-panel',
	hideClass: 'ui-tabs-hide',
	loadingClass: 'ui-tabs-loading'
};

jQuery.ui.tabs.getter = "length";

/*
 * Tabs Extensions
 */

/*
 * Rotate
 */
jQuery.extend(jQuery.ui.tabs.prototype, {
	rotation: null,
	rotate: function(ms, continuing) {
		
		continuing = continuing || false;
		
		var self = this, t = this.options.selected;
		
		function start() {
			self.rotation = setInterval(function() {
				t = ++t < self.jQuerytabs.length ? t : 0;
				self.select(t);
			}, ms); 
		}
		
		function stop(e) {
			if (!e || e.clientX) { // only in case of a true click
				clearInterval(self.rotation);
			}
		}
		
		// start interval
		if (ms) {
			start();
			if (!continuing)
				this.jQuerytabs.bind(this.options.event, stop);
			else
				this.jQuerytabs.bind(this.options.event, function() {
					stop();
					t = self.options.selected;
					start();
				});
		}
		// stop interval
		else {
			stop();
			this.jQuerytabs.unbind(this.options.event, stop);
		}
	}
});

})(jQuery);
