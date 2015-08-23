(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintersController", ['$scope', '$http', '$location', '$anchorScroll', function ($scope, $http, $location, $anchorScroll) {
		controller = this;
		
		function refreshPrinters() {
	        $http.get('/services/printers/list').success(function(data) {
            	$scope.printers = data;
            	if (data != null && data.length > 0) {
            		controller.currentPrinter = data[0];
            	} else {
            		controller.currentPrinter = null;
            	}
            });
	    }
		
		this.editCurrentPrinter = function editCurrentPrinter(editTitle) {
			controller.editTitle = editTitle;
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
        	$('#editModal').modal();
		}
		
		this.editCurrentPrinter = function createNewPrinter(editTitle) {
			controller.editTitle = editTitle;
			if (controller.currentPrinter == null) {
		        $http.post('/services/printers/deletePrinter/' + printerName).success(
		        		function (data) {
		        			controller.editPrinter = data;
		                	$('#editModal').modal();
		        		}).error(
	    				function (data, status, headers, config, statusText) {
	 	        			$scope.$emit("HTTPError", {status:status, statusText:statusText});
		        		})
		        return;
			}
			
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
			controller.editPrinter.name = controller.editPrinter.name + " (Copy)";
        	$('#editModal').modal();
		}
		
		this.changeCurrentPrinter = function changeCurrentPrinter(newPrinter) {
			controller.currentPrinter = newPrinter;
			
			//TODO: Fix for Mobile!
		    //$location.hash('printer');
		    //$anchorScroll();
		}
		
		this.deleteCurrentPrinter = function deleteCurrentPrinter() {
			var printerName = encodeURIComponent(controller.currentPrinter.name);
			
	        $http.post('/services/printers/deletePrinter/' + printerName).success(
	        		function (data) {
	        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrinters, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:statusText});
	        		})
		}
		
		$http.get('/services/machine/serialPorts/list').success(
				function (data) {
					controller.serialPorts = data;
				});
		
		$http.get('/services/machine/graphicsDisplays/list').success(
				function (data) {
					controller.graphicsDisplays = data;
				});
		
		refreshPrinters();
	}])

})();