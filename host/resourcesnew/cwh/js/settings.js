(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("SettingsController", ['$scope', '$http', '$location', '$routeParams', '$interval', function ($scope, $http, $location, $routeParams, $interval) {
		controller = this;
		var timeoutValue = 500;
		var maxUnmatchedPings = 3;//Maximum number of pings before we assume that we lost our connection
		var unmatchedPing = -1;    //Number of pings left until we lose our connection
		var thankYouMessage = " Thank you for unplugging the network cable. This configuration process could take a few minutes to complete. You can close your browser now and use the CWH Client to find your printer.";
		this.loadingNetworksMessage = "--- Loading wifi networks from server ---"
			
		//TODO: Websocket code needs to be an angular plugin to deal with(two way binding, socket destruction on scope loss and automatic content-type parsing)
		function createWebSocketURL(relativeURL) {
			  var loc = window.location;
			  var schema;
			  if (loc.protocol === "https:") {
				  schema = "wss:";
			  } else {
				  schema = "ws:";
			  }
			  
			  return schema + "//" + loc.host + "/services/" + relativeURL;
		}
		
		function attachToHost() {
		  	if ("WebSocket" in window) {
				var ws = new WebSocket(createWebSocketURL("hostNotification"));
				var lostScope = "Lost Scope";

				//This needs to be closed when we leave the page
			 	$scope.$on('$destroy', function() {
			        ws.close(1000, lostScope);
			    });
			 	
				//ws.onopen = function() {};//Do nothing on open...
				var wsOnMessage = function(event) {
					var hostEvent = JSON.parse(event.data);
					
					$scope.$apply(function () {
						controller.restartMessage = " " + hostEvent.message;
					});
					
					if (hostEvent.notificationEvent == "Ping") {
						var unmatchedPingCheck = function() {
							ws.send(event.data);
							
							if (unmatchedPing === 0) {
								controller.restartMessage = thankYouMessage;
								unmatchedPing = -1;//Start over from scratch if we get another ping!!!
							} else {
								unmatchedPing--;
								$interval(unmatchedPingCheck, timeoutValue, 1);
							}
						}
						
						if (unmatchedPing === -1) {
							$interval(unmatchedPingCheck, timeoutValue, 1);
						}
						
						unmatchedPing = maxUnmatchedPings;
					}
				};
				
				var wsOnClose = function(code) {
					//There is only one good reason to close the websocket and that is through a scope change
					//If there is any other reason for the close, we are going too reopen a new socket.
					if (code.reason !== lostScope) {
						ws = new WebSocket(createWebSocketURL("hostNotification"));
						ws.onclose = wsOnClose;
						ws.onmessage = wsOnMessage;
					}
					$scope.$apply(function () {
						controller.restartMessage = thankYouMessage;
					});
				};
				
				ws.onclose = wsOnClose;
				ws.onmessage = wsOnMessage;
			} else {
				$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
			}
		}
		
		this.connectToWireless = function connectToWireless() {
			controller.shutdownInProgress = true;
			$http.put("services/machine/wirelessConnect", controller.selectedNetworkInterface).then(
		    		function (data) {
		    			//$location.path("/printersPage");
		    			controller.restartMessage = " Waiting for host to start monitoring process.";
		    			$('#editModal').modal();
		    			
		    			$http.post("services/machine/startNetworkRestartProcess/600000/" + timeoutValue + "/" + maxUnmatchedPings).then(
		    		    		function (data) {
		    		    			controller.restartMessage = " Network monitoring has been setup.";
		    		    		},
		    		    		function (error) {
		    		    			controller.restartMessage = " Print host was unable to start network monitoring process. Click cancel."
		    		    		}
		    		    )
		    		},
		    		function (error) {
 	        			$scope.$emit("HTTPError", {status:error.status, statusText:error.data});
 	        			controller.shutdownInProgress = false;
		    		}
		    )
		};
		
		//TODO: this needs to be attached to more than just the cancel button so that we can kill the web socket.
		this.cancelRestartProcess = function cancelRestartProcess() {
			$http.post("services/machine/cancelNetworkRestartProcess").then(
		    		function (data) {
		    			controller.shutdownInProgress = false;
		    		},
		    		function (error) {
		    			controller.shutdownInProgress = false;
 	        			$scope.$emit("HTTPError", {status:error.status, statusText:error.data});
		    		}
		    )
		}
		this.saveEmailSettings = function saveEmailSettings() {
			if (!Array.isArray(controller.emailSettings.notificationEmailAddresses)) {
				controller.emailSettings.notificationEmailAddresses = [controller.emailSettings.notificationEmailAddresses];
			}
			if (!Array.isArray(controller.emailSettings.serviceEmailAddresses)) {
				controller.emailSettings.serviceEmailAddresses = [controller.emailSettings.serviceEmailAddresses];
			}
			$http.put("services/settings/emailSettings", controller.emailSettings).then(
		    		function (data) {
		    			alert("Email settings saved.");
		    		},
		    		function (error) {
 	        			$scope.$emit("HTTPError", {status:error.status, statusText:error.data});
		    		}
		    )
		};
		
		$http.get("services/settings/emailSettings").success(
	    		function (data) {
	    			controller.emailSettings = data;
	    		})
	    		
		$http.get("services/machine/wirelessNetworks/list").success(
	    		function (data) {
	    			controller.networkInterfaces = data;
	    			controller.loadingNetworksMessage = "Select a wifi network";
	    		})
	
		attachToHost();
	}])
})();