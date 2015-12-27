angular.module('cwh.webSocket', []).factory('cwhWebSocket', function() {
	//TODO: Add two way binding back to the websocket with angular watches!!! That would be cool...
	
	function Socket(relativeURL, $scope, useReconnect) {
		function createWebSocketURL(relativeURL) {
			  var loc = window.location;
			  var schema;
			  if (loc.protocol === "https:") {
				  schema = "wss:";
			  } else {
				  schema = "ws:";
			  }
			  
			  return schema + "//" + loc.host + "/" + relativeURL;
		}
		function assignOnMessage() {
			ws.onmessage = function(event) {
				$scope.$apply(function () {
					if (onJsonContentFunction != null) {
						onJsonContentFunction(JSON.parse(event.data));
					} else {
						onMessageFunction(event);
					}
				});
			}
		}
		var ws = new WebSocket(createWebSocketURL(relativeURL));
		var onMessageFunction = null;
		var onErrorFunction = null;
		var onCloseFunction = null;
		var onJsonContentFunction = null;
		var outerThis = this;
		this.useReconnect = useReconnect == null?true:useReconnect == true;
		
		this.sendMessage = function sendMessage(data) {
			ws.send(data);
		}
		this.onMessage = function onMessage(_onMessageFunction) {
			onMessageFunction = _onMessageFunction;
			onJsonContentFunction = null;
			assignOnMessage();
			return outerThis;
		}
		this.onJsonContent = function onJsonContent(_onJsonContentFunction) {
			onJsonContentFunction = _onJsonContentFunction;
			assignOnMessage();
			return outerThis;
		}
		this.onError = function onError(_onErrorFunction) {
			onErrorFunction = _onErrorFunction
			ws.onerror = function(event) {
				$scope.$apply(function () {
					_onErrorFunction(event);
				});
			}
			return outerThis;
		}
		this.onClose = function onClose(_onCloseFunction) {
			onCloseFunction = _onCloseFunction;
			ws.onclose = function(event) {
				$scope.$apply(function () {
					_onCloseFunction(event);
				});
				if (outerThis.useReconnect) {
					ws = new WebSocket(createWebSocketURL(relativeURL));
					outerThis.onClose(onCloseFunction);
					outerThis.onMessage(onMessageFunction);
					outerThis.onError(onErrorFunction);
				}
			}
			return outerThis;
		}
		this.disconnect = function disconnect() {
			outerThis.useReconnect = false;
			ws.close(1000, "Direct disconnect");
		}
		//This needs to be closed when we leave the page
		if ($scope != null) {
		 	$scope.$on('$destroy', function() {
		 		outerThis.useReconnect = false;
		        ws.close(1000, "Angular lost scope");
		    });
		}
	}
	

	//printerName = encodeURIComponent(printerName);
	//"services/printerNotification/" + printerName
	this.connect = function connect(relativeURL, $scope, useReconnect) {
	  	if ("WebSocket" in window) {
		  	return new Socket(relativeURL, $scope);
		}

	  	return null;
	}
	
    return this;
});