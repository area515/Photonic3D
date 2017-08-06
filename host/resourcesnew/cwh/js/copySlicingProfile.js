(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("copySLicingProfileController", function ($scope, $uibModalInstance, title, sliceData, nameProfile) {
		$scope.title = title;
		$scope.sliceData = sliceData;
		$scope.save = function () {
			$uibModalInstance.close(sliceData);
		};
		
		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
		
	})
	
})();
