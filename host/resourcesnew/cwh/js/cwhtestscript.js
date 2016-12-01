angular.module('cwh.testscript', []).directive('cwhTestscript', ['$window', '$animate', '$sce', function($window, $animate, $sce) {
    return {
        restrict: 'AE', //Use as attribute and element
        template: 	'<span class="input-group-btn">' +
			'<a class="btn btn-primary" ng-click="testClick()">Test</a>' +
			'</span>' +
			'<textarea rows="1" cols="50" class="form-control" ng-keyup="setModel()" ng-focus="onFocus()" ng-blur="onBlur()" placeholder="none"></textarea>' +
			'<span uib-popover-html="helpURL" popover-title="Help Options" popover-append-to-body="true" popover-trigger="click outsideClick" class="input-group-addon input-large btn btn-success" ng-click="helpClick()" ng-show="helpURL">Help</span>',
			
        scope: {},
        require: 'ngModel',
        link: function(scope, iElement, iAttrs, ngModelController) {//TODO: should be using formatters and parsers for this.
        	scope.helpURL = $sce.trustAsHtml(iAttrs.helpurl);
        	iElement.find('textarea')[0].placeholder = iAttrs.placeholder == null?"":iAttrs.placeholder;
        	
            ngModelController.$render = function() {
                iElement.find('textarea')[0].value = ngModelController.$viewValue;
            };
            scope.setModel = function() {
            	ngModelController.$setViewValue(iElement.find('textarea')[0].value);
            }
            scope.helpClick = function() {
            	//$window.location.href = scope.helpURL;
            }
            scope.onFocus = function() {
            	$animate.addClass(iElement.find('textarea')[0], 'cwh-text-large-area');
            }
            scope.onBlur = function() {
            	$animate.removeClass(iElement.find('textarea')[0], 'cwh-text-large-area');
            }
            scope.testClick = function() {
            	scope.$parent.$eval(iAttrs.testclick);
            }
        }
    }
}]);