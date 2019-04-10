(function() {
  var defaults = {
      0: {
        src: 'example-thumbnail.png'
      }
    },
    extend = function() {
      var args, target, i, object, property;
      args = Array.prototype.slice.call(arguments);
      target = args.shift() || {};
      for (i in args) {
        object = args[i];
        for (property in object) {
          if (object.hasOwnProperty(property)) {
            if (typeof object[property] === 'object') {
              target[property] = extend(target[property], object[property]);
            } else {
              target[property] = object[property];
            }
          }
        }
      }
      return target;
    };

  /**
   * register the thubmnails plugin
   */
  videojs.plugin('thumbnails', function(options) {
    var div, settings, img, player, progressControl, duration;
    var settings = extend({}, defaults, options);
    var player = this;

    // create the thumbnail
    div = document.createElement('div');
    div.className = 'vjs-thumbnail-holder';
    img = document.createElement('img');
    div.appendChild(img);
    img.src = settings['0'].src;
    img.className = 'vjs-thumbnail';
    extend(img.style, settings['0'].style);
    // center the thumbnail over the cursor if an offset wasn't provided
    if (!img.style.left && !img.style.right) {
      img.onload = function() {
        img.style.left = -((img.naturalWidth || img.width) / 2) + 'px';
      }
    };

    // keep track of the duration to calculate correct thumbnail to display
    duration = player.duration();
    player.on('durationchange', function(event) {
      duration = player.duration();
    });

    // add the thumbnail to the player
    progressControl = player.controlBar.progressControl;
    progressControl.el().appendChild(div);

    // update the thumbnail while hovering
    function updateThumbnail(event) {
      var mouseTime, time, active, left, setting;
      active = 0;

      // find the page offset of the mouse
      left = event.pageX || (event.clientX + document.body.scrollLeft + document.documentElement.scrollLeft);
      // subtract the page offset of the progress control
      left -= progressControl.el().getBoundingClientRect().left +  (window.scrollX || window.pageXOffset || document.body.scrollLeft);
      div.style.left = left + 'px';

      var x = event.offsetX==undefined?event.layerX:event.offsetX;
      // apply updated styles to the thumbnail if necessary
      mouseTime = Math.floor(x / progressControl.width() * duration);
      for (time in settings) {
        ftTime =  parseFloat(time);

        if (mouseTime > ftTime) {
          active = Math.max(active, ftTime);
        }
      }

      setting = settings[active];
      if (setting.src && img.src != setting.src) {
        img.src = setting.src;
      }
      if (setting.style && img.style != setting.style) {
        extend(img.style, setting.style);
      }
    }

    if (document.addEventListener) {
      progressControl.el().addEventListener('mousemove', updateThumbnail, false);
    } else if (document.attachEvent) {
      progressControl.el().attachEvent('onmousemove', updateThumbnail);
    }
  });
})();
