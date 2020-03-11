/*
 * function createCloud()
{
      var tagCloud = new TagCloud();
      tagCloud.setValues([{val:10,tag:'Google',href:'http://www.google.com'},
                          {val:31,tag:'Yahoo',href:'http://www.google.com'},
                          {val:15,tag:'AOL',href:'http://www.google.com'},
                          {val:50,tag:'Microsoft',href:'http://www.google.com'},
                          {val:15,tag:'CoolSearch',href:'http://www.google.com'},
                          {val:52,tag:'Main',href:'http://www.google.com'},
                          {val:45,tag:'Lulu',href:'http://www.google.com'},
                          {val:25,tag:'Lili',href:'http://www.google.com'}]);
      tagCloud.create('cloud');
}
 */

function TagCloud() {
    var _this = this;
    _this.values = null;
    _this.min = 100;
    _this.max = 0;

    _this.getFontSize = function(min, max, val) {
        return Math.round((150.0 * (1.0 + (1.5 * val - min / 2) / max))) / 2;
    }

    _this.setValues = function(vals) {
        if (!vals instanceof Array) {
            alert("This is not an array of objects as expected.");
            return;
        }

        for ( var i = 0; i < vals.length; i++) {
            if (vals[i].val > _this.max) {
                _this.max = vals[i].val;
            }

            if (vals[i].val < _this.min) {
                _this.min = vals[i].val;
            }
        }

        _this.values = vals;
    }

    _this.create = function(divId) {
        var div = document.getElementById(divId);
        if (div) {
            div.innerHTML = "";
            var vals = _this.values;
            for ( var i = 0; i < vals.length; i++) {
                var a = document.createElement('a');
                a.href = vals[i].href;
                a.style.fontSize = _this.getFontSize(_this.min, _this.max,
                        vals[i].val) + '%';
                a.style.padding = "2px";
                a.innerHTML = vals[i].tag;

                div.appendChild(a);
                div.appendChild(document.createElement('wbr'));
            }
        }
    }
}
