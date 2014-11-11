function InputContextKeeper(containerId) {
    if (containerId === undefined) {
        throw new Error("InputContextKeeper needs a 'containerId' to initialize itself.");
    }
    var _this = this;
    var fileInputContextKeeper = null;

    _this.remove = function(arr, from, to) {
        var rest = arr.slice((to || from) + 1 || arr.length);
        arr.length = from < 0 ? arr.length + from : from;
        return arr.push.apply(arr, rest);
    }

    // checks whether keeper exists
    _this.keeperExists = function() {
        if (_this.fileInputContextKeeper == null)
            return false;
        if (_this.fileInputContextKeeper == 'undefined')
            return false;

        return true;
    }

    // creates one
    _this.createKeeper = function() {
        _this.fileInputContextKeeper = new Array();
    }

    // Get the keeper or create one if there is none
    _this.getKeeper = function() {
        if (!_this.keeperExists()) {
            _this.createKeeper();
        }

        return _this.fileInputContextKeeper;
    }

    // Gets an input from keeper
    _this.getInputFromKeeper = function(index) {

        if (_this.getKeeper().length > index) {
            return _this.getKeeper()[index];
        }

        return null;
    }

    // Puts an object in the keeper
    _this.putInKeep = function(obj) {
        _this.getKeeper().push(obj);
    }

    _this.clearKeeper = function() {
        _this.createKeeper();
    }

    _this.getIndexFromId = function(id) {
        var arr = id.split(':');
        var nr = -1;
        for ( var i = 0; i < arr.length; i++) {

            var lnr = parseInt(arr[i]);
            if (!isNaN(lnr)) {
                nr = lnr;
            }
        }

        return nr;
    }

    _this.rebuildIdIndex = function(id, newIndex) {
        var arr = id.split(':');
        var index = -1;
        var ret = "";
        for ( var i = 0; i < arr.length; i++) {

            var lnr = parseInt(arr[i]);
            if (!isNaN(lnr)) {
                index = i;
            }
        }

        if (index == -1)
            return null;

        arr[index] = newIndex;
        for ( var i = 0; i < arr.length - 1; i++) {
            ret = ret + arr[i] + ":";
        }

        ret = ret + arr[arr.length - 1];

        return ret;
    }

    // Removes an input from keeper
    _this.removeFromKeeper = function(index) {
        try {
            _this.gather();

            if (_this.getKeeper().length > index) {

                _this.remove(_this.getKeeper(),index);
                for ( var i = 0; i < _this.getKeeper().length; i++) {
                    _this.getKeeper()[i].id = _this.rebuildIdIndex(_this.getKeeper()[i].id, i);
                    _this.getKeeper()[i].name = _this.rebuildIdIndex(_this.getKeeper()[i].name, i);
                }

                //_this.replaceAllInputs();
            }
        } catch (e) {
            alert(e);
        }
    }

    _this.replaceAllInputs = function() {
        var index = 0;
        // Get all inputs
        var container = document.getElementById(containerId);
        var allInputs = container.getElementsByTagName('input');

        // Iterate to see if there is any input file
        for ( var i = 0; i < allInputs.length; i++) {
            if (allInputs[i].type == 'file') {

                var parent = allInputs[i].parentNode;
                var inputInKeeper = _this.getInputFromKeeper(index);

                if (inputInKeeper != null) {
                    inputInKeeper.onclick = allInputs[i].onclick;
                    parent.removeChild(allInputs[i]);
                    parent.appendChild(inputInKeeper);
                    index++;
                }
            }
        }
    }

    _this.gather = function() {
        // Get all inputs
        var container = document.getElementById(containerId);
        var allInputs = container.getElementsByTagName('input');
        _this.clearKeeper();

        // Iterate to see if there is any input file
        for ( var i = 0; i < allInputs.length; i++) {
            if (allInputs[i].type == 'file') {
                _this.putInKeep(allInputs[i].cloneNode(true));
            }
        }
    }

    // Used on add button(onclick) to save the file inputs
    _this.onAddFile = function() {
        try {
            _this.gather();
        } catch (e) {
            alert(e);
        }
    }

    // User on add button(oncomplete) to restore file input values
    _this.onReturnAnswer = function() {
        try {
            _this.replaceAllInputs();
        } catch (e) {
            alert(e);
        }
    }

}
