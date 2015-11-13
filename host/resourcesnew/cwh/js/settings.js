(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("SettingsController", ['$scope', '$http', '$location', '$routeParams', function ($scope, $http, $location, $routeParams) {
		controller = this;
		
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
				//ws.onopen = function() {};//Do nothing on open...
				ws.onmessage = function createJobWebSocket(event) {
						var hostEvent = JSON.parse(event.data);
						$scope.$apply(function () {
							controller.restartMessage = " " + hostEvent.message;
						});
				};
				
				//TODO: This needs to be closed when we leave the page
				ws.onclose = function() {
				 	$scope.$on('$destroy', function() {
				        ws.close();
						$scope.$apply(function () {
							controller.restartMessage = " Thank you for unplugging the network cable. This configuration process could take a few minutes to complete. You can close your browser now and use the CWH Client to find your printer.";
						});
				    });
				};
			} else {
				$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
			}
		}
		
		this.connectToWireless = function connectToWireless() {
			$http.put("services/machine/wirelessConnect", controller.selectedNetworkInterface).then(
		    		function (data) {
		    			//$location.path("/printersPage");
		    			controller.restartMessage = " Waiting for host to start monitoring process.";
		    			$('#editModal').modal();
		    			
		    			$http.post("services/machine/startNetworkRestartProcess/600000/500").then(
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
		    		}
		    )
		};
		
		//TODO: this needs to be attached to more than just the cancel button so that we can kill the web socket.
		this.cancelRestartProcess = function cancelRestartProcess() {
			$http.post("services/machine/cancelNetworkRestartProcess").then(
		    		function (data) {
		    		},
		    		function (error) {
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
	    		})
	
		attachToHost();
	}])
})();