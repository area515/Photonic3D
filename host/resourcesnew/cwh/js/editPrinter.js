(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("EditPrinterController", function ($scope, $http, $uibModalInstance, title, editPrinter) {
		$scope.title = title;
		$scope.editPrinter = editPrinter;
		
		//TODO: All of these things should come from the MachineService
		$scope.comPortSpeeds = [1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200];
		$scope.parities = ["Even", "Mark", "None", "Odd", "Space"];
		$scope.stopBits = ["None", "One", "1.5", "Two"];
		$scope.dataBits = [5, 6, 7, 8, 9];
		$scope.fullScreenModes = ["AlwaysUseFullScreen", "NeverUseFullScreen", "UseFullScreenWhenExclusiveIsAvailable"];
		
		$http.get('/services/machine/serialPorts/list').success(
				function (data) {
					$scope.serialPorts = data;
				});
		
		$http.get('/services/machine/graphicsDisplays/list').success(
				function (data) {
					$scope.graphicsDisplays = data;
				});

		$scope.save = function () {
			$uibModalInstance.close(editPrinter);
		};
		
		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
		
	})
	
})();