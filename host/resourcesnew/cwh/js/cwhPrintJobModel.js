angular.module("printJobModelViewer", [])
	.directive("printJobModelViewer", ['$http', '$compile', function ($http, $compile) {
			return {
				restrict: "AE",
				scope: {
					printJob: "=printJob"
				},
			    template: '<i class="pull-left fa fa-2x fa-exclamation-triangle" ng-show="errorMessage"></i><h3 class="list-group-item-heading" ng-show="errorMessage">{{errorMessage}}</h3>',
			    link: function (scope, elem, attr) {
					var camera;
					var scene;
					var renderer;
					var currentMesh;
					var parentDiv = elem[0].parentElement;
					var raycaster;
					var mouseVector = new THREE.Vector2();
					var buildAreaMesh;
					var axisRoseLines;
					var solidPreviewCanvas;
					
					init();
					
					var loader1 = new THREE.Photonic3dTriangleLoader();
 
					scope.$watch("printJob", function(newValue, oldValue) {
						//When the actual print job changes
						if ((newValue != null && oldValue != null && newValue.id != oldValue.id) || (oldValue == null && newValue != null)) {
							if (newValue != null) {
								loadModel(newValue);
							} else {
								stopAnimation("No printJob selected");
							}
						}
					});
					
					function colorizeErrors(errorURL) {
						$http.get(errorURL).success(
								function (json) {
									json.forEach(function(item, index) {
										currentMesh.geometry.faces[item.i].color.setHex(0xff0000);
									});
									if (json != null) {
										currentMesh.geometry.colorsNeedUpdate = true;
									}
								});
					}
					
					function showAxisRose() {
						if (axisRoseLines != null) {
							scene.remove(axisRoseLines[0]);
							scene.remove(axisRoseLines[1]);
							scene.remove(axisRoseLines[2]);
						}
						
						
						var xRose = new THREE.Geometry();
						var yRose = new THREE.Geometry();
						var zRose = new THREE.Geometry();
						xRose.vertices.push(new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( 50, 0, 0 ));
						yRose.vertices.push(new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( 0, 50, 0 ));
						zRose.vertices.push(new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( 0, 0, 50 ));
						xRose.computeLineDistances();
						yRose.computeLineDistances();
						zRose.computeLineDistances();
						
						axisRoseLines = [new THREE.LineSegments( xRose, new THREE.LineDashedMaterial( { color: 0xff0000, dashSize: 3, gapSize: 1, linewidth: 2 } ) ),
						                 new THREE.LineSegments( yRose, new THREE.LineDashedMaterial( { color: 0x00ff00, dashSize: 3, gapSize: 1, linewidth: 2 } ) ),
						                 new THREE.LineSegments( zRose, new THREE.LineDashedMaterial( { color: 0x0000ff, dashSize: 3, gapSize: 1, linewidth: 2 } ) )];
						
						scene.add( axisRoseLines[0] );
						scene.add( axisRoseLines[1] );
						scene.add( axisRoseLines[2] );
					}
						
					function showPrinterBox() {
						if (buildAreaMesh != null) {
							scene.remove(buildAreaMesh);
						}
						
						var bottomX = 0;
						var bottomY = 0;
						var bottomZ = 0;
						var width = printJob.printer.configuration.machineConfig.PlatformXSize;
						var length = printJob.printer.configuration.machineConfig.PlatformYSize;
						var height = printJob.printer.configuration.machineConfig.PlatformZSize;
						
						var geometryCube = new THREE.Geometry();
						geometryCube.vertices.push(
							new THREE.Vector3( bottomX, bottomY, bottomZ ),          //Front left
							new THREE.Vector3( bottomX, bottomY, bottomZ + height ),
							new THREE.Vector3( bottomX + width, bottomY, bottomZ),   //Front right
							new THREE.Vector3( bottomX + width, bottomY, bottomZ + height ),
							new THREE.Vector3( bottomX, bottomY, bottomZ + height ), //Front Top
							new THREE.Vector3( bottomX + width, bottomY, bottomZ + height ),
							new THREE.Vector3( bottomX, bottomY, bottomZ ),          //Front Bottom
							new THREE.Vector3( bottomX + width, bottomY, bottomZ ),
							
							new THREE.Vector3( bottomX, bottomY, bottomZ + height),
							new THREE.Vector3( bottomX, bottomY + length, bottomZ + height),
							new THREE.Vector3( bottomX, bottomY, bottomZ ),
							new THREE.Vector3( bottomX, bottomY + length, bottomZ ),
							new THREE.Vector3( bottomX + width, bottomY, bottomZ + height ),
							new THREE.Vector3( bottomX + width, bottomY + length, bottomZ + height ),
							new THREE.Vector3( bottomX + width, bottomY, bottomZ ),
							new THREE.Vector3( bottomX + width, bottomY + length, bottomZ ),
							
							new THREE.Vector3( bottomX, bottomY + length, bottomZ ),          //Rear left
							new THREE.Vector3( bottomX, bottomY + length, bottomZ + height ),
							new THREE.Vector3( bottomX + width, bottomY + length, bottomZ),   //Rear right
							new THREE.Vector3( bottomX + width, bottomY + length, bottomZ + height ),
							new THREE.Vector3( bottomX, bottomY + length, bottomZ + height ), //Rear Top
							new THREE.Vector3( bottomX + width, bottomY + length, bottomZ + height ),
							new THREE.Vector3( bottomX, bottomY + length, bottomZ ),          //Rear Bottom
							new THREE.Vector3( bottomX + width, bottomY + length, bottomZ )
						 );

						buildAreaMesh = new THREE.LineSegments( geometryCube, new THREE.LineDashedMaterial( { color: 0xffaa00, dashSize: 3, gapSize: 1, linewidth: 2 } ) );
						geometryCube.computeLineDistances();
						scene.add( buildAreaMesh );
					}
					
					function loadModel(printJobToLoad) {
						scope.errorMessage = "Please wait while loading 3d model";
						var geometryURL = "/services/printJobs/geometry/" + printJobToLoad.id;
						var geometryErrorsURL = "/services/printJobs/geometryErrors/" + printJobToLoad.id;
						
						scope.$evalAsync(function() {
							loader1.load(geometryURL, 
								function (geometry) {
									//TODO: if there is a custom scale we could adjust it here
									var material = new THREE.MeshPhongMaterial({shading: THREE.FlatShading, vertexColors: THREE.FaceColors});
									var newMesh = new THREE.Mesh( geometry, material );
									if (currentMesh) {
										scene.remove(currentMesh);
									}
									
									scene.add(newMesh);
									currentMesh = newMesh;
									camera.lookAt(currentMesh.geometry.boundingBox.min);
									controls.target = currentMesh.geometry.boundingBox.min;
									colorizeErrors(geometryErrorsURL);
									startAnimation();
									//showPrinterBox();
									//showAxisRose();
								},
								null,
								function (event) {
									stopAnimation(event.target.responseText);
								}
							);
		                });
					}
					
					if (scope.printJob) {
						loadModel(scope.printJob);
					}
					
					function getWidth() {
						return parentDiv.clientWidth - (parentDiv.offsetLeft * 2);
					}
					
					function getHeight() {
						return 500;
					}
					
					function init() {
						var width = getWidth();
						var height = getHeight();
												
						//Scene and camera setup
						scene = new THREE.Scene();
						camera = new THREE.PerspectiveCamera(45, width / height, 1, 1000 );
						camera.position.x = 80;
						camera.position.y = -80;
						camera.position.z = 300;
						camera.up = new THREE.Vector3(0, 0, 1);
						
						// Renderer setup
						renderer = new THREE.WebGLRenderer();
						renderer.setSize(width, height);
						solidPreviewCanvas = renderer.domElement;
						elem[0].appendChild(solidPreviewCanvas);
						
						//Lights for reflective textures
						scene.add( new THREE.AmbientLight( 0x443333 ) );
						var light = new THREE.DirectionalLight( 0xffddcc, 1 );
						light.position.set( 1, 0.75, 0.5 );
						scene.add( light );
						
						var light = new THREE.DirectionalLight( 0xccccff, 1 );
						light.position.set( -1, 0.75, -0.5 );
						scene.add( light );
						
						// This is for movement controls
						controls = new THREE.OrbitControls( camera, renderer.domElement );
						controls.minDistance = 50;
						controls.maxDistance = 2000;
						
						// Events
						window.addEventListener('resize', onWindowResize, false);
					}
 
					function onWindowResize(event) {
						var width = getWidth();
						var height = getHeight();
						
						renderer.setSize(width, height);
						camera.aspect = width / height;
						camera.updateProjectionMatrix();
					}
 
					function startAnimation() {
						scope.$apply(new function() {
							scope.errorMessage = null;
							renderer.domElement.style.display = "";
						});
						
						render();
					}
					
					function stopAnimation(textToShow) {
						scope.$apply(new function() {
							scope.errorMessage = textToShow;
							renderer.domElement.style.display = "none";
						});
						
						if (currentMesh) {
							scene.remove(currentMesh);
							currentMesh = null;
						}
					}
					
					function render() {
						if (currentMesh != null) {
							requestAnimationFrame(render);
							renderer.render(scene, camera);
						}
					}
				}
			}
		}
	]);