package com.esri.alejo.ramapa;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
//import com.esri.android.map.Layer

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.LayerContent;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.esri.alejo.ramapa.R.string.url_servicio_ruta;


public class ARActivity extends AppCompatActivity
        implements SensorEventListener,LocationListener,NavigationView.OnNavigationItemSelectedListener {

    private SensorManager sensorManager;
    private SurfaceView surfaceView;
    private FrameLayout cameraContainerLayout;
    private AROverlayView arOverlayView;
    private TextView tvCurrentLocation;
    private final static int REQUEST_CAMERA_PERMISSIONS_CODE = 11;
    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private Camera camera;
    private ARCamera arCamera;

    public Location location;
    boolean isGPSEnabled;
    boolean isNetworkEnabled;
    boolean locationServiceAvailable;
    private LocationManager locationManager;
    private View viewAR;

    private MapView vistaMapLittle;
    private ArcGISMap mapaLittle;

    public fragmentMapa fragMap;
    public LocationDisplay locationDisplay;
    public RelativeLayout contentMap;

    final static String TAG = "ARActivity";

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 0;//1000 * 60 * 1; // 1 minute

    final float radioBuffer = (float) 300;//radio de buffer//modificar puede ser en radianes

    private FeatureLayer restaurantes, parqueaderos, hoteles;
    private LayerList layers;
    //obtener posicion
    private Point posicion;

    RouteTask routeTask;
    RouteParameters routeParameters;
    Route rutaResultado;

    //AROverlayView arOver = new AROverlayView(getBaseContext());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud9088059687,none,HC5X0H4AH4YDXH46C082");

        //geoLocalizacion2();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        contentMap = (RelativeLayout) this.findViewById(R.id.layout_miniMap);

        //agregar mapa pequeño
        createLittleMap();

        //ar content------------------------------------
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        cameraContainerLayout = (FrameLayout) findViewById(R.id.camera_container_layout);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        tvCurrentLocation = (TextView) findViewById(R.id.tv_current_location);
        arOverlayView = new AROverlayView(this);
        toggle.syncState();

        Intent actAr = getIntent();
        //mapa2 = (mapaCarga) actAr.getSerializableExtra("miMapa");

    }

    public void createLittleMap(){

        fragMap = new fragmentMapa();
        vistaMapLittle = this.findViewById(R.id.mapView);
        mapaLittle = new ArcGISMap(this.getResources().getString(R.string.URL_mapa_alrededores));
        //mapaLittle = mapa2.getMap();
        vistaMapLittle.setMap(mapaLittle);
        vistaMapLittle.setVisibility(View.VISIBLE);
        vistaMapLittle.setBackgroundGrid(new BackgroundGrid(Color.WHITE, Color.WHITE, 0, vistaMapLittle.getBackgroundGrid().getGridSize()));
        vistaMapLittle.setWrapAroundMode(WrapAroundMode.DISABLED);

        locationDisplay = vistaMapLittle.getLocationDisplay();
        locationDisplay.startAsync();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        vistaMapLittle.setOnTouchListener(new IdentifyFeatureLayerTouchListener(vistaMapLittle.getContext(), vistaMapLittle));
        mapaLittle.addLoadStatusChangedListener(new LoadStatusChangedListener() {
            @Override
            public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {

                String mapLoadStatus;
                mapLoadStatus = loadStatusChangedEvent.getNewLoadStatus().name();
                switch (mapLoadStatus) {
                    case "LOADED":
                        Toast.makeText(vistaMapLittle.getContext(),"Cargado",Toast.LENGTH_LONG).show();
                        contentMap.setVisibility(View.VISIBLE);
                        //Toast.makeText(vistaMapLittle.getContext(),"antes de onsulta",Toast.LENGTH_LONG).show();
                        hacerConsulta(getResources().getString(R.string.URL_capa_policias),1);
                        hacerConsulta(getResources().getString(R.string.URL_capa_hospitales),2);
                        //Toast.makeText(vistaMapLittle.getContext(),"despues consulta",Toast.LENGTH_LONG).show();
                        LayerList layers = mapaLittle.getOperationalLayers();
                        if(!layers.isEmpty()){
                            parqueaderos = (FeatureLayer) layers.get(0);
                            restaurantes = (FeatureLayer) layers.get(1);
                        }
                        if(mapaLittle.getInitialViewpoint() != null){
                            vistaMapLittle.setViewpoint(mapaLittle.getInitialViewpoint());
                        }

                        break;
                }
            }
        });
    }

    private class IdentifyFeatureLayerTouchListener extends DefaultMapViewOnTouchListener {

        private FeatureLayer layer = null; // reference to the layer to identify features in
        // provide a default constructor
        public IdentifyFeatureLayerTouchListener(Context context, MapView mapView) {
            super(context, mapView);
        }

        // override the onSingleTapConfirmed gesture to handle a single tap on the MapView
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // get the screen point where user tapped
            android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
            final ListenableFuture<List<IdentifyLayerResult>> identifyFuture = super.mMapView.identifyLayersAsync(screenPoint, 5,
                    false);

            // add a listener to the future
            identifyFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        // get the identify results from the future - returns when the operation is complete
                        List<IdentifyLayerResult> identifyLayersResults = identifyFuture.get();

                        // iterate all the layers in the identify result
                        for (IdentifyLayerResult identifyLayerResult : identifyLayersResults) {

                            // each identified layer should find only one or zero results, when identifying topmost GeoElement only
                            if (identifyLayerResult.getElements().size() > 0) {
                                GeoElement topmostElement = identifyLayerResult.getElements().get(0);
                                if (topmostElement instanceof Feature) {
                                    Feature identifiedFeature = (Feature)topmostElement;

                                    // Use feature as required, for example access attributes or geometry, select, build a table, etc...
                                    processIdentifyFeatureResult(identifiedFeature, identifyLayerResult.getLayerContent());
                                }
                            }
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        //dealWithException(ex); // must deal with exceptions thrown from the async identify operation
                    }
                }
            });
            return super.onSingleTapConfirmed(e);
        }
    }

    private void processIdentifyFeatureResult(Feature feature, LayerContent content){
        String nombre = "", direccion = "", foto = "";
        switch (content.getName()){
            case "Bancos":
                nombre = (String) feature.getAttributes().get("Banco");
                break;
            case "EstacionesPolicia":
                Toast.makeText(this,"parqu",Toast.LENGTH_LONG).show();
                nombre = (String) feature.getAttributes().get("Nombre");
                List<ARPoint> lista = arOverlayView.obtenerArPoints();
                for(int i=0;i<=lista.size() ;i++){
                    ARPoint punto = lista.get(i);
                    String nombreFeature = arOverlayView.getNombreLugar(punto);
                    Toast.makeText(this,nombreFeature,Toast.LENGTH_LONG).show();
                    if(nombreFeature.equals(nombre)){
                        Toast.makeText(this,nombre,Toast.LENGTH_LONG).show();
                        Point p = new Point(punto.getLocation().getLatitude()
                                ,punto.getLocation().getLongitude(),mapaLittle.getSpatialReference());
                        //encontrarRuta(p);
                        Toast.makeText(this,"ruta encontrada",Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case "Restaurantes":
                nombre = (String) feature.getAttributes().get("Restaurante");
                break;
        }
    }

    //encontrar ruta desde la posicion actual al punto seleccionado
    private void encontrarRuta(final Point destino ){
        final String routeTaskService = getResources().getString(R.string.url_servicio_ruta);
        // create route task from San Diego service
        routeTask = new RouteTask(this,routeTaskService);
        // load route task
        routeTask.loadAsync();
        routeTask.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                if (routeTask.getLoadError() == null && routeTask.getLoadStatus() == LoadStatus.LOADED) {
                    // route task has loaded successfully
                    try {
                        // get default route parameters
                        routeParameters = routeTask.createDefaultParametersAsync().get();
                        // set flags to return stops and directions
                        routeParameters.setReturnStops(true);
                        routeParameters.setReturnDirections(true);
                        //crea una lista de paradas, y las agrega
                        List routeStops = routeParameters.getStops();
                        routeStops.add(new Stop(posicion));
                        routeStops.add(new Stop(destino));
                        //calcula el resultado de la ruta
                        RouteResult resultadoRuta = routeTask.solveRouteAsync(routeParameters).get();
                        //obtiene la lista de las rutas obtenidas
                        List routes = resultadoRuta.getRoutes();
                        //obtiene la primera ruta que resulta
                        rutaResultado = (Route) routes.get(0);

                        resultadoRuta.getPointBarriers();

                        //representa graficamente la ruta



                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        vistaMapLittle.resume();
        requestLocationPermission();
        requestCameraPermission();
        registerSensors();
        initAROverlayView();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
    }

    @Override
    public void onPause() {
        releaseCamera();
        vistaMapLittle.pause();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        super.onPause();
    }

    public void requestCameraPermission() {
        //colocado para hacer la verificacion en tiempo de ejecucion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSIONS_CODE);
        } else {
            initARCameraView();
        }
    }

    private void releaseCamera() {
        if(camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            arCamera.setCamera(null);
            camera.release();
            camera = null;
        }
    }

    public void initARCameraView() {
        reloadSurfaceView();

        if (arCamera == null) {
            arCamera = new ARCamera(this, surfaceView);
        }
        if (arCamera.getParent() != null) {
            ((ViewGroup) arCamera.getParent()).removeView(arCamera);
        }
        cameraContainerLayout.addView(arCamera);
        arCamera.setKeepScreenOn(true);
        initCamera();
    }

    private void initCamera() {
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open();
                camera.startPreview();
                arCamera.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void reloadSurfaceView() {
        if (surfaceView.getParent() != null) {
            ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
        }

        cameraContainerLayout.addView(surfaceView);
    }

    public void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSIONS_CODE);
        } else {
            initLocationService();
        }
    }

    public void initAROverlayView() {
        if (arOverlayView.getParent() != null) {
            ((ViewGroup) arOverlayView.getParent()).removeView(arOverlayView);
        }
        cameraContainerLayout.addView(arOverlayView);
    }

    private void registerSensors() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void initLocationService() {

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        try   {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

            // Get GPS and network status
            this.isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            this.isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isNetworkEnabled && !isGPSEnabled)    {
                // cannot get location
                this.locationServiceAvailable = false;
            }

            this.locationServiceAvailable = true;

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (locationManager != null)   {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    updateLatestLocation();
                }
            }

            if (isGPSEnabled)  {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null)  {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateLatestLocation();
                }
            }
        } catch (Exception ex)  {
            Log.e(TAG, ex.getMessage());

        }
    }

    //actualizar el punto de la posicion actual para hacer el buffer
    private void actualizarPunto(Location loc){
       posicion = new Point(loc.getLatitude(),loc.getLongitude(),loc.getAltitude(), SpatialReferences.getWgs84());
       //Geometry xx = GeometryEngine.project(posicion,mapaLittle.getSpatialReference());
       //posicion = (Point)xx;
    }

    //hacer consulta para obtener puntos de capa segun el buffer que se hace para representar puntos en AR
    public void hacerConsulta(String urlCapa, final int numCapa) {
        try{
            //Toast.makeText(vistaMapLittle.getContext(),"entra a onsulta",Toast.LENGTH_LONG).show();
        //final ServiceFeatureTable serviceFT = new ServiceFeatureTable(this.getResources().getString(R.string.URL_mapa_alrededores));
        final ServiceFeatureTable serviceFT = new ServiceFeatureTable(urlCapa);
        FeatureLayer featureLayerprueba = new FeatureLayer(serviceFT);

        mapaLittle.getOperationalLayers().add(featureLayerprueba);
            //Toast.makeText(vistaMapLittle.getContext(),"tamaño da capas" + (mapaLittle.getOperationalLayers().size()),Toast.LENGTH_LONG).show();
        serviceFT.setFeatureRequestMode(ServiceFeatureTable.FeatureRequestMode.MANUAL_CACHE);
        serviceFT.loadAsync();
        serviceFT.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                QueryParameters queryParam = new QueryParameters();
                queryParam.getOrderByFields().add(new QueryParameters.OrderBy("Nombre",QueryParameters.SortOrder.DESCENDING));
                queryParam.setWhereClause("1=1");//clausula de busqueda
                queryParam.setReturnGeometry(true);
                queryParam.setOutSpatialReference(SpatialReferences.getWgs84());//referencia espacial del query
                //queryParam.setOutSpatialReference(mapaLittle.getSpatialReference());
                // set all outfields
                List<String> outFields = new ArrayList<>();
                outFields.add("*");
                //arreglo de features que bota la seleccion de features en el feature layer
                final ListenableFuture<FeatureQueryResult> featureQResult = serviceFT.populateFromServiceAsync(queryParam,true,outFields);
                featureQResult.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Toast.makeText(vistaMapLittle.getContext(),"entra a query result",Toast.LENGTH_LONG).show();
                            //Toast.makeText(vistaMapLittle.getContext(),"query result" + featureQResult,Toast.LENGTH_LONG).show();
                            FeatureQueryResult result = featureQResult.get();
                            //Toast.makeText(vistaMapLittle.getContext(),"resultado" + result.toString(),Toast.LENGTH_LONG).show();
                            Iterator<Feature> iterator = result.iterator();
                            Feature feat;

                            ARPoint arPoint = null;
                            actualizarPunto(location);
                            //Toast.makeText(vistaMapLittle.getContext(),"posicion:"+posicion.getSpatialReference().toString(),Toast.LENGTH_LONG).show();
                            Geometry buffer = GeometryEngine.buffer(posicion,radioBuffer);
                            //Toast.makeText(vistaMapLittle.getContext(),buffer.toJson(),Toast.LENGTH_LONG).show();
                            while(iterator.hasNext()){
                                feat = iterator.next();
                                //
                                Point punto = (Point) feat.getGeometry();
                                //Toast.makeText(vistaMapLittle.getContext(),"punto:"+punto.getSpatialReference().toString(),Toast.LENGTH_LONG).show();
                                //Geometry v = GeometryEngine.project(punto,mapaLittle.getSpatialReference());
                                //punto = (Point)v;
                                ///
                                List<Geometry> puntosIntersec = GeometryEngine.intersections(buffer,punto);
                                Iterator<Geometry> iterPoint = puntosIntersec.iterator();
                                Point p;
                                while(iterPoint.hasNext()){
                                    //Toast.makeText(vistaMapLittle.getContext(),"entra al iterador",Toast.LENGTH_LONG).show();
                                    p=(Point)iterPoint.next();
                                    //Toast.makeText(vistaMapLittle.getContext(),"p:"+p.getSpatialReference().toString(),Toast.LENGTH_LONG).show();
                                    GeometryEngine.project(p,mapaLittle.getSpatialReference());

                                    arPoint = new ARPoint((String) feat.getAttributes().get("Nombre"),
                                            p.getY(),p.getX(),location.getAltitude());
                                    //Toast.makeText(vistaMapLittle.getContext(),arPoint.getName(),Toast.LENGTH_LONG).show();

                                    arOverlayView.agregarArPoints(arPoint,numCapa);
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Toast.makeText(vistaMapLittle.getContext(),"error interrupcion" + e,Toast.LENGTH_LONG).show();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                            Toast.makeText(vistaMapLittle.getContext(),"error excepcion" + e,Toast.LENGTH_LONG).show();
                        }
                    }
                });

            }
        });


        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(vistaMapLittle.getContext(),"error de onsulta",Toast.LENGTH_LONG).show();
        }
    }


    private void updateLatestLocation() {
        if (arOverlayView !=null && location != null) {
            arOverlayView.updateCurrentLocation(location);
            tvCurrentLocation.setText(String.format("lat: %s \nlon: %s \naltitude: %s \n",
                    location.getLatitude(), location.getLongitude(), location.getAltitude()));
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            //actualizarPunto(location);
        }
    }

    ////location and sensors--------------------------------


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        android.support.v4.app.FragmentManager fragManager = getSupportFragmentManager();

        switch (item.getItemId()){
            case R.id.nav_layers:
        }
        // Handle navigation view item clicks here.
        /*int id = item.getItemId();

         else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrixFromVector = new float[16];
            float[] projectionMatrix = new float[16];
            float[] rotatedProjectionMatrix = new float[16];

            SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, event.values);

            if (arCamera != null) {
                projectionMatrix = arCamera.getProjectionMatrix();
            }

            Matrix.multiplyMM(rotatedProjectionMatrix, 0, projectionMatrix, 0, rotationMatrixFromVector, 0);
            this.arOverlayView.updateRotatedProjectionMatrix(rotatedProjectionMatrix);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        updateLatestLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
