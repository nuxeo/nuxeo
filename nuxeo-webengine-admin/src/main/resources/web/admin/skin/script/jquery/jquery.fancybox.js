/*
 * FancyBox - simple jQuery plugin for fancy image zooming
 * Examples and documentation at: http://fancy.klade.lv/
 * Version: 1.0.0 (29/04/2008)
 * Copyright (c) 2008 Janis Skarnelis
 * Licensed under the MIT License: http://www.opensource.org/licenses/mit-license.php
 * Requires: jQuery v1.2.1 or later
*/
(function($) {
	var opts = {}, 
		imgPreloader = new Image, imgTypes = ['png', 'jpg', 'jpeg', 'gif'], 
		loadingTimer, loadingFrame = 1;

   $.fn.fancybox = function(settings) {
		opts.settings = $.extend({}, $.fn.fancybox.defaults, settings);

		$.fn.fancybox.init();

		return this.each(function() {
			var $this = $(this);
			var o = $.metadata ? $.extend({}, opts.settings, $this.metadata()) : opts.settings;

			$this.unbind('click').click(function() {
				$.fn.fancybox.start(this, o); return false;
			});
		});
	};

	$.fn.fancybox.start = function(el, o) {
		if (opts.animating) return false;

		if (o.overlayShow) {
			$("#fancy_wrap").prepend('<div id="fancy_overlay"></div>');
			$("#fancy_overlay").css({'width': $(window).width(), 'height': $(document).height(), 'opacity': o.overlayOpacity});

			if ($.browser.msie) {
				$("#fancy_wrap").prepend('<iframe id="fancy_bigIframe" scrolling="no" frameborder="0"></iframe>');
				$("#fancy_bigIframe").css({'width': $(window).width(), 'height': $(document).height(), 'opacity': 0});
			}

			$("#fancy_overlay").click($.fn.fancybox.close);
		}

		opts.itemArray	= [];
		opts.itemNum	= 0;

		if (jQuery.isFunction(o.itemLoadCallback)) {
		   o.itemLoadCallback.apply(this, [opts]);

			var c	= $(el).children("img:first").length ? $(el).children("img:first") : $(el);
			var tmp	= {'width': c.width(), 'height': c.height(), 'pos': $.fn.fancybox.getPosition(c)}

		   for (var i = 0; i < opts.itemArray.length; i++) {
				opts.itemArray[i].o = $.extend({}, o, opts.itemArray[i].o);
				
				if (o.zoomSpeedIn > 0 || o.zoomSpeedOut > 0) {
					opts.itemArray[i].orig = tmp;
				}
		   }

		} else {
			if (!el.rel || el.rel == '') {
				var item = {url: el.href, title: el.title, o: o};

				if (o.zoomSpeedIn > 0 || o.zoomSpeedOut > 0) {
					var c = $(el).children("img:first").length ? $(el).children("img:first") : $(el);
					item.orig = {'width': c.width(), 'height': c.height(), 'pos': $.fn.fancybox.getPosition(c)}
				}

				opts.itemArray.push(item);

			} else {
				var arr	= $("a[@rel=" + el.rel + "]").get();

				for (var i = 0; i < arr.length; i++) {
					var tmp		= $.metadata ? $.extend({}, o, $(arr[i]).metadata()) : o;
   					var item	= {url: arr[i].href, title: arr[i].title, o: tmp};

   					if (o.zoomSpeedIn > 0 || o.zoomSpeedOut > 0) {
						var c = $(arr[i]).children("img:first").length ? $(arr[i]).children("img:first") : $(el);

						item.orig = {'width': c.width(), 'height': c.height(), 'pos': $.fn.fancybox.getPosition(c)}
					}

					if (arr[i].href == el.href) opts.itemNum = i;

					opts.itemArray.push(item);
				}
			}
		}

		$.fn.fancybox.changeItem(opts.itemNum);
	};

	$.fn.fancybox.changeItem = function(n) {
		$.fn.fancybox.showLoading();

		opts.itemNum = n;

		$("#fancy_nav").empty();
		$("#fancy_outer").stop();
		$("#fancy_title").hide();
		$(document).unbind("keydown");

		imgRegExp = imgTypes.join('|');
    	imgRegExp = new RegExp('\.' + imgRegExp + '$', 'i');

		var url = opts.itemArray[n].url;

		if (url.match(/#/)) {
			var target = window.location.href.split('#')[0]; target = url.replace(target,'');

	        $.fn.fancybox.showItem('<div id="fancy_div">' + $(target).html() + '</div>');

	        $("#fancy_loading").hide();

		} else if (url.match(imgRegExp)) {
			$(imgPreloader).unbind('load').bind('load', function() {
				$("#fancy_loading").hide();

				opts.itemArray[n].o.frameWidth	= imgPreloader.width;
				opts.itemArray[n].o.frameHeight	= imgPreloader.height;

				$.fn.fancybox.showItem('<img id="fancy_img" src="' + imgPreloader.src + '" />');

			}).attr('src', url + '?rand=' + Math.floor(Math.random() * 999999999) );

		} else {
			$.fn.fancybox.showItem('<iframe id="fancy_frame" onload="$.fn.fancybox.showIframe()" name="fancy_iframe' + Math.round(Math.random()*1000) + '" frameborder="0" hspace="0" src="' + url + '"></iframe>');
		}
	};

	$.fn.fancybox.showIframe = function() {
		$("#fancy_loading").hide();
		$("#fancy_frame").show();
	};

	$.fn.fancybox.showItem = function(val) {
		$.fn.fancybox.preloadNeighborImages();

		var viewportPos	= $.fn.fancybox.getViewport();
		var itemSize	= $.fn.fancybox.getMaxSize(viewportPos[0] - 50, viewportPos[1] - 100, opts.itemArray[opts.itemNum].o.frameWidth, opts.itemArray[opts.itemNum].o.frameHeight);

		var itemLeft	= viewportPos[2] + Math.round((viewportPos[0] - itemSize[0]) / 2) - 20;
		var itemTop		= viewportPos[3] + Math.round((viewportPos[1] - itemSize[1]) / 2) - 40;

		var itemOpts = {
			'left':		itemLeft, 
			'top':		itemTop, 
			'width':	itemSize[0] + 'px', 
			'height':	itemSize[1] + 'px'	
		}

		if (opts.active) {
			$('#fancy_content').fadeOut("normal", function() {
				$("#fancy_content").empty();
				
				$("#fancy_outer").animate(itemOpts, "normal", function() {
					$("#fancy_content").append($(val)).fadeIn("normal");
					$.fn.fancybox.updateDetails();
				});
			});

		} else {
			opts.active = true;

			$("#fancy_content").empty();

			if ($("#fancy_content").is(":animated")) {
				console.info('animated!');
			}

			if (opts.itemArray[opts.itemNum].o.zoomSpeedIn > 0) {
				opts.animating		= true;
				itemOpts.opacity	= "show";

				$("#fancy_outer").css({
					'top':		opts.itemArray[opts.itemNum].orig.pos.top - 18,
					'left':		opts.itemArray[opts.itemNum].orig.pos.left - 18,
					'height':	opts.itemArray[opts.itemNum].orig.height,
					'width':	opts.itemArray[opts.itemNum].orig.width
				});

				$("#fancy_content").append($(val)).show();

				$("#fancy_outer").animate(itemOpts, opts.itemArray[opts.itemNum].o.zoomSpeedIn, function() {
					opts.animating = false;
					$.fn.fancybox.updateDetails();
				});

			} else {
				$("#fancy_content").append($(val)).show();
				$("#fancy_outer").css(itemOpts).show();
				$.fn.fancybox.updateDetails();
			}
		 }
	};

	$.fn.fancybox.updateDetails = function() {
		$("#fancy_bg,#fancy_close").show();

		if (opts.itemArray[opts.itemNum].title !== undefined && opts.itemArray[opts.itemNum].title !== '') {
			$('#fancy_title div').html(opts.itemArray[opts.itemNum].title);
			$('#fancy_title').show();
		}

		if (opts.itemArray[opts.itemNum].o.hideOnContentClick) {
			$("#fancy_content").click($.fn.fancybox.close);
		} else {
			$("#fancy_content").unbind('click');
		}

		if (opts.itemNum != 0) {
			$("#fancy_nav").append('<a id="fancy_left" href="javascript:;"></a>');

			$('#fancy_left').click(function() {
				$.fn.fancybox.changeItem(opts.itemNum - 1); return false;
			});
		}

		if (opts.itemNum != (opts.itemArray.length - 1)) {
			$("#fancy_nav").append('<a id="fancy_right" href="javascript:;"></a>');
			
			$('#fancy_right').click(function(){
				$.fn.fancybox.changeItem(opts.itemNum + 1); return false;
			});
		}

		$(document).keydown(function(event) {
			if (event.keyCode == 27) {
            	$.fn.fancybox.close();

			} else if(event.keyCode == 37 && opts.itemNum != 0) {
            	$.fn.fancybox.changeItem(opts.itemNum - 1);

			} else if(event.keyCode == 39 && opts.itemNum != (opts.itemArray.length - 1)) {
            	$.fn.fancybox.changeItem(opts.itemNum + 1);
			}
		});
	};

	$.fn.fancybox.preloadNeighborImages = function() {
		if ((opts.itemArray.length - 1) > opts.itemNum) {
			preloadNextImage = new Image();
			preloadNextImage.src = opts.itemArray[opts.itemNum + 1].url;
		}

		if (opts.itemNum > 0) {
			preloadPrevImage = new Image();
			preloadPrevImage.src = opts.itemArray[opts.itemNum - 1].url;
		}
	};

	$.fn.fancybox.close = function() {
		if (opts.animating) return false;

		$(imgPreloader).unbind('load');
		$(document).unbind("keydown");

		$("#fancy_loading,#fancy_title,#fancy_close,#fancy_bg").hide();

		$("#fancy_nav").empty();

		opts.active	= false;

		if (opts.itemArray[opts.itemNum].o.zoomSpeedOut > 0) {
			var itemOpts = {
				'top':		opts.itemArray[opts.itemNum].orig.pos.top - 18,
				'left':		opts.itemArray[opts.itemNum].orig.pos.left - 18,
				'height':	opts.itemArray[opts.itemNum].orig.height,
				'width':	opts.itemArray[opts.itemNum].orig.width,
				'opacity':	'hide'
			};

			opts.animating = true;

			$("#fancy_outer").animate(itemOpts, opts.itemArray[opts.itemNum].o.zoomSpeedOut, function() {
				$("#fancy_content").hide().empty();
				$("#fancy_overlay,#fancy_bigIframe").remove();
				opts.animating = false;
			});

		} else {
			$("#fancy_outer").hide();
			$("#fancy_content").hide().empty();
			$("#fancy_overlay,#fancy_bigIframe").fadeOut("fast").remove();
		}
	};

	$.fn.fancybox.showLoading = function() {
		clearInterval(loadingTimer);

		var pos = $.fn.fancybox.getViewport();

		$("#fancy_loading").css({'left': ((pos[0] - 40) / 2 + pos[2]), 'top': ((pos[1] - 40) / 2 + pos[3])}).show();
		$("#fancy_loading").bind('click', $.fn.fancybox.close);
		
		loadingTimer = setInterval($.fn.fancybox.animateLoading, 66);
	};

	$.fn.fancybox.animateLoading = function(el, o) {
		if (!$("#fancy_loading").is(':visible')){
			clearInterval(loadingTimer);
			return;
		}

		$("#fancy_loading > div").css('top', (loadingFrame * -40) + 'px');

		loadingFrame = (loadingFrame + 1) % 12;
	};

	$.fn.fancybox.init = function() {
		if (!$('#fancy_wrap').length) {
			$('<div id="fancy_wrap"><div id="fancy_loading"><div></div></div><div id="fancy_outer"><div id="fancy_inner"><div id="fancy_nav"></div><div id="fancy_close"></div><div id="fancy_content"></div><div id="fancy_title"></div></div></div></div>').appendTo("body");
			$('<div id="fancy_bg"><div class="fancy_bg fancy_bg_n"></div><div class="fancy_bg fancy_bg_ne"></div><div class="fancy_bg fancy_bg_e"></div><div class="fancy_bg fancy_bg_se"></div><div class="fancy_bg fancy_bg_s"></div><div class="fancy_bg fancy_bg_sw"></div><div class="fancy_bg fancy_bg_w"></div><div class="fancy_bg fancy_bg_nw"></div></div>').prependTo("#fancy_inner");
			
			$('<table cellspacing="0" cellpadding="0" border="0"><tr><td id="fancy_title_left"></td><td id="fancy_title_main"><div></div></td><td id="fancy_title_right"></td></tr></table>').appendTo('#fancy_title');
		}

		if ($.browser.msie) {
			$("#fancy_inner").prepend('<iframe id="fancy_freeIframe" scrolling="no" frameborder="0"></iframe>');
		}

		if (jQuery.fn.pngFix) $(document).pngFix();

    	$("#fancy_close").click($.fn.fancybox.close);
	};

	$.fn.fancybox.getPosition = function(el) {
		var pos = el.offset();

		pos.top	+= $.fn.fancybox.num(el, 'paddingTop');
		pos.top	+= $.fn.fancybox.num(el, 'borderTopWidth');

 		pos.left += $.fn.fancybox.num(el, 'paddingLeft');
		pos.left += $.fn.fancybox.num(el, 'borderLeftWidth');

		return pos;
	};

	$.fn.fancybox.num = function (el, prop) {
		return parseInt($.curCSS(el.jquery?el[0]:el,prop,true))||0;
	};

	$.fn.fancybox.getPageScroll = function() {
		var xScroll, yScroll;

		if (self.pageYOffset) {
			yScroll = self.pageYOffset;
			xScroll = self.pageXOffset;
		} else if (document.documentElement && document.documentElement.scrollTop) {
			yScroll = document.documentElement.scrollTop;
			xScroll = document.documentElement.scrollLeft;
		} else if (document.body) {
			yScroll = document.body.scrollTop;
			xScroll = document.body.scrollLeft;	
		}

		return [xScroll, yScroll]; 
	};

	$.fn.fancybox.getViewport = function() {
		var scroll = $.fn.fancybox.getPageScroll();

		return [$(window).width(), $(window).height(), scroll[0], scroll[1]];
	};

	$.fn.fancybox.getMaxSize = function(maxWidth, maxHeight, imageWidth, imageHeight) {
		var r = Math.min(Math.min(maxWidth, imageWidth) / imageWidth, Math.min(maxHeight, imageHeight) / imageHeight);

		return [Math.round(r * imageWidth), Math.round(r * imageHeight)];
	};

	$.fn.fancybox.defaults = {
		hideOnContentClick:	false,
		zoomSpeedIn:		500,
		zoomSpeedOut:		500,
		frameWidth:			600,
		frameHeight:		400,
		overlayShow:		false,
		overlayOpacity:		0.4,
		itemLoadCallback:	null
	};
})(jQuery);