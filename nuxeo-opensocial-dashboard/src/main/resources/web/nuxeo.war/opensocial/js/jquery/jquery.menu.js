/*
 * jQuery Menu plugin
 * Version: 0.0.9
 *
 * Copyright (c) 2007 Roman Weich
 * http://p.sohei.org
 *
 * Dual licensed under the MIT and GPL licenses
 * (This means that you can choose the license that best suits your project, and use it accordingly):
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * Changelog:
 * v 0.0.9 - 2008-01-19
 */

(function(jQuery)
{
  var menus = [], //list of all menus
    visibleMenus = [], //list of all visible menus
    activeMenu = activeItem = null,
    menuDIVElement = jQuery('<div class="menu-div outerbox" style="position:absolute;top:0;left:0;display:none;"><div class="shadowbox1"></div><div class="shadowbox2"></div><div class="shadowbox3"></div></div>')[0],
    menuULElement = jQuery('<ul class="menu-ul innerbox"></ul>')[0],
    menuItemElement = jQuery('<li style="position:relative;"><div class="menu-item"></div></li>')[0],
    arrowElement = jQuery('<img class="menu-item-arrow" />')[0],
    jQueryrootDiv = jQuery('<div id="root-menu-div" style="position:absolute;top:0;left:0;"></div>'), //create main menu div
    defaults = {
      // jQuery.Menu options
      showDelay : 200,
      hideDelay : 200,
      hoverOpenDelay: 0,
      offsetTop : 0,
      offsetLeft : 0,
      minWidth: 0,
      onOpen: null,
      onClose: null,

      // jQuery.MenuItem options
      onClick: null,
      arrowSrc: null,
      addExpando: false,

      // jQuery.fn.menuFromElement options
      copyClassAttr: false
    };

  jQuery(function(){
    jQueryrootDiv.appendTo('body');
  });

  jQuery.extend({
    MenuCollection : function(items) {

      this.menus = [];

      this.init(items);
    }
  });
  jQuery.extend(jQuery.MenuCollection, {
    prototype : {
      init : function(items)
      {
        if ( items && items.length )
        {
          for ( var i = 0; i < items.length; i++ )
          {
            this.addMenu(items[i]);
            items[i].menuCollection = this;
          }
        }
      },
      addMenu : function(menu)
      {
        if ( menu instanceof jQuery.Menu )
          this.menus.push(menu);

        menu.menuCollection = this;

        var self = this;
        jQuery(menu.target).hover(function(){
          if ( menu.visible )
            return;

          //when there is an open menu in this collection, hide it and show the new one
          for ( var i = 0; i < self.menus.length; i++ )
          {
            if ( self.menus[i].visible )
            {
              self.menus[i].hide();
              menu.show();
              return;
            }
          }
        }, function(){});
      }
    }
  });


  jQuery.extend({
    Menu : function(target, items, options) {
      this.menuItems = []; //all direct child jQuery.MenuItem objects
      this.subMenus = []; //all subMenus from this.menuItems
      this.visible = false;
      this.active = false; //this menu has hover or one of its submenus is open
      this.parentMenuItem = null;
      this.settings = jQuery.extend({}, defaults, options);
      this.target = target;
      this.jQueryeDIV = null;
      this.jQueryeUL = null;
      this.timer = null;
      this.menuCollection = null;
      this.openTimer = null;

      this.init();
      if ( items && items.constructor == Array )
        this.addItems(items);
    }
  });

  jQuery.extend(jQuery.Menu, {
    checkMouse : function(e)
    {
      var t = e.target;

      //the user clicked on the target of the currenty open menu
      if ( visibleMenus.length && t == visibleMenus[0].target )
        return;

      //get the last node before the #root-menu-div
      while ( t.parentNode && t.parentNode != jQueryrootDiv[0] )
        t = t.parentNode;

      //is the found node one of the visible menu elements?
      if ( !jQuery(visibleMenus).filter(function(){ return this.jQueryeDIV[0] == t }).length )
      {
        jQuery.Menu.closeAll();
      }
    },
    checkKey : function(e)
    {
      switch ( e.keyCode )
      {
        case 13: //return
          if ( activeItem )
            activeItem.click(e, activeItem.jQueryeLI[0]);
          break;
        case 27: //ESC
          jQuery.Menu.closeAll();
          break;
        case 37: //left
          if ( !activeMenu )
            activeMenu = visibleMenus[0];
          var a = activeMenu;
          if ( a && a.parentMenuItem ) //select the parent menu and close the submenu
          {
            //unbind the events temporary, as we dont want the hoverout event to fire
            var pmi = a.parentMenuItem;
            pmi.jQueryeLI.unbind('mouseout').unbind('mouseover');
            a.hide();
            pmi.hoverIn(true);
            setTimeout(function(){ //bind again..but delay it
              pmi.bindHover();
            });
          }
          else if ( a && a.menuCollection ) //select the previous menu in the collection
          {
            var pos,
              mcm = a.menuCollection.menus;
            if ( (pos = jQuery.inArray(a, mcm)) > -1 )
            {
              if ( --pos < 0 )
                pos = mcm.length - 1;
              jQuery.Menu.closeAll();
              mcm[pos].show();
              mcm[pos].setActive();
              if ( mcm[pos].menuItems.length ) //select the first item
                mcm[pos].menuItems[0].hoverIn(true);
            }
          }
          break;
        case 38: //up
          if ( activeMenu )
            activeMenu.selectNextItem(-1);
          break;
        case 39: //right
          if ( !activeMenu )
            activeMenu = visibleMenus[0];
          var m,
            a = activeMenu,
            asm = activeItem ? activeItem.subMenu : null;
          if ( a )
          {
            if ( asm && asm.menuItems.length ) //select the submenu
            {
              asm.show();
              asm.menuItems[0].hoverIn();
            }
            else if ( (a = a.inMenuCollection()) ) //select the next menu in the collection
            {
              var pos,
                mcm = a.menuCollection.menus;
              if ( (pos = jQuery.inArray(a, mcm)) > -1 )
              {
                if ( ++pos >= mcm.length )
                  pos = 0;
                jQuery.Menu.closeAll();
                mcm[pos].show();
                mcm[pos].setActive();
                if ( mcm[pos].menuItems.length ) //select the first item
                  mcm[pos].menuItems[0].hoverIn(true);
              }
            }
          }
          break;
        case 40: //down
          if ( !activeMenu )
          {
            if ( visibleMenus.length && visibleMenus[0].menuItems.length )
              visibleMenus[0].menuItems[0].hoverIn();
          }
          else
            activeMenu.selectNextItem();
          break;
      }
      if ( e.keyCode > 36 && e.keyCode < 41 )
        return false; //this will prevent scrolling
    },
    closeAll : function()
    {
      while ( visibleMenus.length )
        visibleMenus[0].hide();
    },
    setDefaults : function(d)
    {
      jQuery.extend(defaults, d);
    },
    prototype : {
      /**
       * create / initialize new menu
       */
      init : function()
      {
        var self = this;
        if ( !this.target )
          return;
        else if ( this.target instanceof jQuery.MenuItem )
        {
          this.parentMenuItem = this.target;
          this.target.addSubMenu(this);
          this.target = this.target.jQueryeLI;
        }

        menus.push(this);

        //use the dom methods instead the ones from jquery (faster)
        this.jQueryeDIV = jQuery(menuDIVElement.cloneNode(1));
        this.jQueryeUL = jQuery(menuULElement.cloneNode(1));
        this.jQueryeDIV[0].appendChild(this.jQueryeUL[0]);
        jQueryrootDiv[0].appendChild(this.jQueryeDIV[0]);

        //bind events
        if ( !this.parentMenuItem )
        {
          jQuery(this.target).click(function(e){
            self.onClick(e);
          }).hover(function(e){
            self.setActive();

            if ( self.settings.hoverOpenDelay )
            {
              self.openTimer = setTimeout(function(){
                if ( !self.visible )
                  self.onClick(e);
              }, self.settings.hoverOpenDelay);
            }
          }, function(){
            if ( !self.visible )
              jQuery(this).removeClass('activetarget');

            if ( self.openTimer )
              clearTimeout(self.openTimer);
          });
        }
        else
        {
          this.jQueryeDIV.hover(function(){
            self.setActive();
          }, function(){});
        }
      },
      setActive : function()
      {
        if ( !this.parentMenuItem )
          jQuery(this.target).addClass('activetarget');
        else
          this.active = true;
      },
      addItem : function(item)
      {
        if ( item instanceof jQuery.MenuItem )
        {
          if ( jQuery.inArray(item, this.menuItems) == -1 )
          {
            this.jQueryeUL.append(item.jQueryeLI);
            this.menuItems.push(item);
            item.parentMenu = this;
            if ( item.subMenu )
              this.subMenus.push(item.subMenu);
          }
        }
        else
        {
          this.addItem(new jQuery.MenuItem(item, this.settings));
        }
      },
      addItems : function(items)
      {
        for ( var i = 0; i < items.length; i++ )
        {
          this.addItem(items[i]);
        }
      },
      removeItem : function(item)
      {
        var pos = jQuery.inArray(item, this.menuItems);
        if ( pos > -1 )
          this.menuItems.splice(pos, 1);
        item.parentMenu = null;
      },
      hide : function()
      {
        if ( !this.visible )
          return;

        var i,
          pos = jQuery.inArray(this, visibleMenus);

        this.jQueryeDIV.hide();

        if ( pos >= 0 )
          visibleMenus.splice(pos, 1);
        this.visible = this.active = false;

        jQuery(this.target).removeClass('activetarget');

        //hide all submenus
        for ( i = 0; i < this.subMenus.length; i++ )
        {
          this.subMenus[i].hide();
        }

        //set all items inactive (e.g. remove hover class..)
        for ( i = 0; i < this.menuItems.length; i++ )
        {
          if ( this.menuItems[i].active )
            this.menuItems[i].setInactive();
        }

        if ( !visibleMenus.length ) //unbind events when the last menu was closed
          jQuery(document).unbind('mousedown', jQuery.Menu.checkMouse).unbind('keydown', jQuery.Menu.checkKey);

        if ( activeMenu == this )
          activeMenu = null;

        if ( this.settings.onClose )
          this.settings.onClose.call(this);
      },
      show : function(e)
      {
        if ( this.visible )
          return;

        var zi,
          pmi = this.parentMenuItem;

        if ( this.menuItems.length ) //show only when it has items
        {
          if ( pmi ) //set z-index
          {
            zi = parseInt(pmi.parentMenu.jQueryeDIV.css('z-index'));
            this.jQueryeDIV.css('z-index', (isNaN(zi) ? 1 : zi + 1));
          }
          this.jQueryeDIV.css({visibility: 'hidden', display:'block'});

          //set min-width
          if ( this.settings.minWidth )
          {
            if ( this.jQueryeDIV.width() < this.settings.minWidth )
              this.jQueryeDIV.css('width', this.settings.minWidth);
          }

          this.setPosition();
          this.jQueryeDIV.css({display:'none', visibility: ''}).show();

          //IEs default width: auto is bad! ie6 and ie7 have are producing different errors.. (7 = 5px shadowbox + 2px border)
          if ( jQuery.browser.msie )
            this.jQueryeUL.css('width', parseInt(jQuery.browser.version) == 6 ? this.jQueryeDIV.width() - 7 : this.jQueryeUL.width());

          if ( this.settings.onOpen )
            this.settings.onOpen.call(this);
        }
        if ( visibleMenus.length == 0 )
          jQuery(document).bind('mousedown', jQuery.Menu.checkMouse).bind('keydown', jQuery.Menu.checkKey);

        this.visible = true;
        visibleMenus.push(this);
      },
      setPosition : function()
      {
        var jQueryt, o, posX, posY,
          pmo, //parent menu offset
          wst, //window scroll top
          wsl, //window scroll left
          ww = jQuery(window).width(),
          wh = jQuery(window).height(),
          pmi = this.parentMenuItem,
          height = this.jQueryeDIV[0].clientHeight,
          width = this.jQueryeDIV[0].clientWidth,
          pheight; //parent height

        if ( pmi )
        {
          //position on the right side of the parent menu item
          o = pmi.jQueryeLI.offset();
          posX = o.left + pmi.jQueryeLI.width();
          posY = o.top;
        }
        else
        {
          //position right below the target
          jQueryt = jQuery(this.target);
          o = jQueryt.offset();
          posX = o.left + this.settings.offsetLeft;
          posY = o.top + jQueryt.height() + this.settings.offsetTop;
        }

        //y-pos
        if ( jQuery.fn.scrollTop )
        {
          wst = jQuery(window).scrollTop();
          if ( wh < height ) //menu is bigger than the window
          {
            //position the menu at the top of the visible area
            posY = wst;
          }
          else if ( wh + wst < posY + height ) //outside on the bottom?
          {
            if ( pmi )
            {
              pmo = pmi.parentMenu.jQueryeDIV.offset();
              pheight = pmi.parentMenu.jQueryeDIV[0].clientHeight;
              if ( height <= pheight )
              {
                //bottom position = parentmenu-bottom position
                posY = pmo.top + pheight - height;
              }
              else
              {
                //top position = parentmenu-top position
                posY = pmo.top;
              }
              //still outside on the bottom?
              if ( wh + wst < posY + height )
              {
                //shift the menu upwards till the bottom is visible
                posY -= posY + height - (wh + wst);
              }
            }
            else
            {
              //shift the menu upwards till the bottom is visible
              posY -= posY + height - (wh + wst);
            }
          }
        }
        //x-pos
        if ( jQuery.fn.scrollLeft )
        {
          wsl = jQuery(window).scrollLeft();
          if ( ww + wsl < posX + width )
          {
            if ( pmi )
            {
              //display the menu not on the right side but on the left side
              posX -= pmi.jQueryeLI.width() + width;
              //outside on the left now?
              if ( posX < wsl )
                posX = wsl;
            }
            else
            {
              //shift the menu to the left until it fits
              posX -= posX + width - (ww + wsl);
            }
          }
        }

        //set position
        this.jQueryeDIV.css({left: posX, top: posY});
      },
      onClick : function(e)
      {
        if ( this.visible )
        {
          this.hide();
          this.setActive(); //the class is removed in the hide() method..add it again
        }
        else
        {
          //close all open menus
          jQuery.Menu.closeAll();
          this.show(e);
        }
      },
      addTimer : function(callback, delay)
      {
        var self = this;
        this.timer = setTimeout(function(){
          callback.call(self);
          self.timer = null;
        }, delay);
      },
      removeTimer : function()
      {
        if ( this.timer )
        {
          clearTimeout(this.timer);
          this.timer = null;
        }
      },
      selectNextItem : function(offset)
      {
        var i, pos = 0,
          mil = this.menuItems.length,
          o = offset || 1;

        //get current pos
        for ( i = 0; i < mil; i++ )
        {
          if ( this.menuItems[i].active )
          {
            pos = i;
            break;
          }
        }
        this.menuItems[pos].hoverOut();

        do //jump over the separators
        {
          pos += o;
          if ( pos >= mil )
            pos = 0;
          else if ( pos < 0 )
            pos = mil - 1;
        } while ( this.menuItems[pos].separator );
        this.menuItems[pos].hoverIn(true);
      },
      inMenuCollection : function()
      {
        var m = this;
        while ( m.parentMenuItem )
          m = m.parentMenuItem.parentMenu;
        return m.menuCollection ? m : null;
      },
      destroy : function() //delete menu
      {
        var pos, item;

        this.hide();

        //unbind events
        if ( !this.parentMenuItem )
          jQuery(this.target).unbind('click').unbind('mouseover').unbind('mouseout');
        else
          this.jQueryeDIV.unbind('mouseover').unbind('mouseout');

        //destroy all items
        while ( this.menuItems.length )
        {
          item = this.menuItems[0];
          item.destroy();
          delete item;
        }

        if ( (pos = jQuery.inArray(this, menus)) > -1 )
          menus.splice(pos, 1);

        if ( this.menuCollection )
        {
          if ( (pos = jQuery.inArray(this, this.menuCollection.menus)) > -1 )
            this.menuCollection.menus.splice(pos, 1);
        }

        this.jQueryeDIV.remove();
      }
    }
  });

  jQuery.extend({
    MenuItem : function(obj, options)
    {
      if ( typeof obj == 'string' )
        obj = {src: obj};

      this.src = obj.src || '';
      this.url = obj.url || null;
      this.urlTarget = obj.target || null;
      this.addClass = obj.addClass || null;
      this.data = obj.data || null;

      this.jQueryeLI = null;
      this.parentMenu = null;
      this.subMenu = null;
      this.settings = jQuery.extend({}, defaults, options);
      this.active = false;
      this.enabled = true;
      this.separator = false;

      this.init();

      if ( obj.subMenu )
        new jQuery.Menu(this, obj.subMenu, options);
    }
  });

  jQuery.extend(jQuery.MenuItem, {
    prototype : {
      init : function()
      {
        var i, isStr,
          src = this.src,
          self = this;

        this.jQueryeLI = jQuery(menuItemElement.cloneNode(1));

        if ( this.addClass )
          this.jQueryeLI[0].setAttribute('class', this.addClass);

        if ( this.settings.addExpando && this.data )
          this.jQueryeLI[0].menuData = this.data;

        if ( src == '' )
        {
          this.jQueryeLI.addClass('menu-separator');
          this.separator = true;
        }
        else
        {
          isStr = typeof src == 'string';
          if ( isStr && this.url ) //create a link node, when we have an url
            src = jQuery('<a href="' + this.url + '"' + (this.urlTarget ? 'target="' + this.urlTarget + '"' : '') + '>' + src + '</a>');
          else if ( isStr || !src.length )
            src = [src];
          //go through the passed DOM-Elements (or jquery objects or text nodes.) and append them to the menus list item
          //this.jQueryeLI.append(this.src) is really slow when having a lot(!!!) of items
          for ( i = 0; i < src.length; i++ )
          {
            if ( typeof src[i] == 'string' )
            {
              //we cant use createTextNode, as html entities won't be displayed correctly (eg. &copy;)
              elem = document.createElement('span');
              elem.innerHTML = src[i];
              this.jQueryeLI[0].firstChild.appendChild(elem);
            }
            else
              this.jQueryeLI[0].firstChild.appendChild(src[i].cloneNode(1));
          }
        }

        this.jQueryeLI.click(function(e){
          self.click(e, this);
        });
        this.bindHover();
      },
      click : function(e, scope)
      {
        if ( this.enabled && this.settings.onClick )
          this.settings.onClick.call(scope, e, this);
      },
      bindHover : function()
      {
        var self = this;
        this.jQueryeLI.hover(function(){
            self.hoverIn();
          }, function(){
            self.hoverOut();
        });
      },
      hoverIn : function(noSubMenu)
      {
        this.removeTimer();

        var i,
          pms = this.parentMenu.subMenus,
          pmi = this.parentMenu.menuItems,
          self = this;

        //remove the timer from the parent item, when there is one (e.g. to close the menu)
        if ( this.parentMenu.timer )
          this.parentMenu.removeTimer();

        if ( !this.enabled )
          return;

        //deactivate all menuItems on the same level
        for ( i = 0; i < pmi.length; i++ )
        {
          if ( pmi[i].active )
            pmi[i].setInactive();
        }

        this.setActive();
        activeMenu = this.parentMenu;

        //are there open submenus on the same level? close them!
        for ( i = 0; i < pms.length; i++ )
        {
          if ( pms[i].visible && pms[i] != this.subMenu && !pms[i].timer ) //close if there is no closetimer running already
            pms[i].addTimer(function(){
              this.hide();
            }, pms[i].settings.hideDelay);
        }

        if ( this.subMenu && !noSubMenu )
        {
          //set timeout to show menu
          this.subMenu.addTimer(function(){
            this.show();
          }, this.subMenu.settings.showDelay);
        }
      },
      hoverOut : function()
      {
        this.removeTimer();

        if ( !this.enabled )
          return;

        if ( !this.subMenu || !this.subMenu.visible )
          this.setInactive();
      },
      removeTimer : function()
      {
        if ( this.subMenu )
        {
          this.subMenu.removeTimer();
        }
      },
      setActive : function()
      {
        this.active = true;
        this.jQueryeLI.addClass('active');

        //set the parent menu item active too if necessary
        var pmi = this.parentMenu.parentMenuItem;
        if ( pmi && !pmi.active )
          pmi.setActive();

        activeItem = this;
      },
      setInactive : function()
      {
        this.active = false;
        this.jQueryeLI.removeClass('active');
        if ( this == activeItem )
          activeItem = null;
      },
      enable : function()
      {
        this.jQueryeLI.removeClass('disabled');
        this.enabled = true;
      },
      disable : function()
      {
        this.jQueryeLI.addClass('disabled');
        this.enabled = false;
      },
      destroy : function()
      {
        this.removeTimer();

        this.jQueryeLI.remove();

        //unbind events
        this.jQueryeLI.unbind('mouseover').unbind('mouseout').unbind('click');
        //delete submenu
        if ( this.subMenu )
        {
          this.subMenu.destroy();
          delete this.subMenu;
        }
        this.parentMenu.removeItem(this);
      },
      addSubMenu : function(menu)
      {
        if ( this.subMenu )
          return;
        this.subMenu = menu;
        if ( this.parentMenu && jQuery.inArray(menu, this.parentMenu.subMenus) == -1 )
          this.parentMenu.subMenus.push(menu);
        if ( this.settings.arrowSrc )
        {
          var a = arrowElement.cloneNode(0);
          a.setAttribute('src', this.settings.arrowSrc);
          this.jQueryeLI[0].firstChild.appendChild(a);
        }
      }
    }
  });


  jQuery.extend(jQuery.fn, {
    menuFromElement : function(options, list, bar)
    {
      var createItems = function(ul)
      {
        var menuItems = [],
          subItems,
          menuItem,
          lis, jQueryli, i, subUL, submenu, target,
          classNames = null;

        lis = getAllChilds(ul, 'LI');
        for ( i = 0; i < lis.length; i++ )
        {
          subItems = [];

          if ( !lis[i].childNodes.length ) //empty item? add separator
          {
            menuItems.push(new jQuery.MenuItem('', options));
            continue;
          }

          if ( (subUL = getOneChild(lis[i], 'UL')) )
          {
            subItems = createItems(subUL);
            //remove subUL from DOM
            jQuery(subUL).remove();
          }

          //select the target...get the elements inside the li
          jQueryli = jQuery(lis[i]);
          if ( jQueryli[0].childNodes.length == 1 && jQueryli[0].childNodes[0].nodeType == 3 )
            target = jQueryli[0].childNodes[0].nodeValue;
          else
            target = jQueryli[0].childNodes;

          if ( options && options.copyClassAttr )
            classNames = jQueryli.attr('class');

          //create item
          menuItem = new jQuery.MenuItem({src: target, addClass: classNames}, options);
          menuItems.push(menuItem);
          //add submenu
          if ( subItems.length )
            new jQuery.Menu(menuItem, subItems, options);

        }
        return menuItems;
      };
      return this.each(function()
      {
        var ul, m;
        //get the list element
        if ( list || (ul = getOneChild(this, 'UL')) )
        {
          //if a specific list element is used, clone it, as we probably need it more than once
          ul = list ? jQuery(list).clone(true)[0] : ul;
          menuItems = createItems(ul);
          if ( menuItems.length )
          {
            m = new jQuery.Menu(this, menuItems, options);
            if ( bar )
              bar.addMenu(m);
          }
          jQuery(ul).hide();
        }
      });
    },
    menuBarFromUL : function(options)
    {
      return this.each(function()
      {
        var i,
          lis = getAllChilds(this, 'LI');

        if ( lis.length )
        {
          bar = new jQuery.MenuCollection();
          for ( i = 0; i < lis.length; i++ )
            jQuery(lis[i]).menuFromElement(options, null, bar);
        }
      });
    },
    menu : function(options, items)
    {
      return this.each(function()
      {
        if ( items && items.constructor == Array )
          new jQuery.Menu(this, items, options);
        else
        {
          if ( this.nodeName.toUpperCase() == 'UL' )
            jQuery(this).menuBarFromUL(options);
          else
            jQuery(this).menuFromElement(options, items);
        }
      });
    }
  });



})(jQuery);

//faster than using jquery
function getOneChild(elem, name)
{
  if ( !elem )
    return null;

  var n = elem.firstChild;
  for ( ; n; n = n.nextSibling )
  {
    if ( n.nodeType == 1 && n.nodeName.toUpperCase() == name )
      return n;
  }
  return null;
};
//faster than using jquery
function getAllChilds(elem, name)
{
  if ( !elem )
    return [];

  var r = [],
    n = elem.firstChild;
  for ( ; n; n = n.nextSibling )
  {
    if ( n.nodeType == 1 && n.nodeName.toUpperCase() == name )
      r[r.length] = n;
  }
  return r;
};
