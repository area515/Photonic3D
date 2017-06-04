(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("copySLicingProfileController", function ($scope, $http, $uibModalInstance, title, sliceData, nameProfile) {
		$scope.title = title;
		$scope.sliceData = sliceData;
		
		$http.get('/services/machine/serialPorts/list').success(
				function (data) {
					$scope.serialPorts = data;
				});
		
		$http.get('/services/machine/graphicsDisplays/list').success(
				function (data) {
					$scope.graphicsDisplays = data;
				});

		$scope.save = function () {
			$uibModalInstance.close(sliceData);
		};
		
		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
		
	})
	
})();