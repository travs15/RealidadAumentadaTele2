package com.esri.alejo.ramapa;



import android.app.FragmentManager;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.LayerContent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;

import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;

import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;

import com.koushikdutta.ion.Ion;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
     * A simple {@link Fragment} subclass.
 */
public class fragmentMapa extends Fragment implements View.OnClickListener,Serializable {
    public MapView vistaMap;
    public ArcGISMap map;
    public View view;
    private LinearLayout contentProgress, contentProgressSearch, popup,layersFilter;
    private RelativeLayout contentMap;

    public FeatureLayer policia, hospitales;
    private ImageButton btnParqueaderos, btnHoteles, btnRestaurantes, closePopup, btnAr,locate,btnFilter;
    private Button btnLlamar;
    private TextView categoria, nombreLugar, direccionLugar;
    private ImageView fotoLugar;
    public String telefonoG = "";

    public boolean flagPar,flagRestaurantes;

    public mapaCarga mapaAumented;


    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION, Manifest.permission.CALL_PHONE};

    //Uso para localizacion
    public LocationDisplay locationDisplay;


    public fragmentMapa() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_mapa, container, false);
        vistaMap = view.findViewById(R.id.mapView);
        locationDisplay = vistaMap.getLocationDisplay();
        // Inflate the layout for this fragment
        initRecursos();
        crearMapa();
        geoLocalizacion();
        return view;
    }

    public void crearMapa(){

        vistaMap = (MapView) view.findViewById(R.id.mapView);
        vistaMap.setAttributionTextVisible(false);
        mapaAumented = new mapaCarga(this.getResources().getString(R.string.URL_mapa_alrededores));
        map = mapaAumented.getMap();
        //map = new ArcGISMap(this.getResources().getString(R.string.URL_mapa_alrededores));
        //map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 4.673, -74.051, 12);
        vistaMap.setMap(map);

        map.addLoadStatusChangedListener(new LoadStatusChangedListener() {
            @Override
            public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
                String mapLoadStatus;
                mapLoadStatus = loadStatusChangedEvent.getNewLoadStatus().name();
                switch (mapLoadStatus) {
                    case "LOADED":
                        contentProgress.setVisibility(View.GONE);
                        contentMap.setVisibility(View.VISIBLE);
                        if(map.getInitialViewpoint() != null)
                            vistaMap.setViewpoint(map.getInitialViewpoint());

                        LayerList layers = map.getOperationalLayers();

                        if(!layers.isEmpty()){
                            policia = (FeatureLayer) layers.get(0);
                            hospitales = (FeatureLayer) layers.get(1);
                        }
                        break;
                }
            }
        });

        vistaMap.setOnTouchListener(new IdentifyFeatureLayerTouchListener(view.getContext(), vistaMap));
        vistaMap.setBackgroundGrid(new BackgroundGrid(Color.WHITE, Color.WHITE, 0, vistaMap.getBackgroundGrid().getGridSize()));

        //ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 4.6097100,  -74.0817500, 16);

        vistaMap.setWrapAroundMode(WrapAroundMode.DISABLED);

        //vistaMap.setMap(map);
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

    private  void initRecursos(){
       contentProgress = (LinearLayout) view.findViewById(R.id.linearProgressBar);
       // contentProgressSearch = (LinearLayout) view.findViewById(R.id.linearProgressBarSearch);
        contentMap = (RelativeLayout) view.findViewById(R.id.contentMap);

        locate = (ImageButton) view.findViewById(R.id.myLocationButton);
        locate.setOnClickListener(this);

        btnFilter = (ImageButton)view.findViewById(R.id.layersButton);
        btnFilter.setOnClickListener(this);

        layersFilter= (LinearLayout) view.findViewById(R.id.first);

        btnParqueaderos = (ImageButton) view.findViewById(R.id.botonParqueaderos);
        btnParqueaderos.setSelected(true);
        btnParqueaderos.setOnClickListener(this);

        btnHoteles = (ImageButton) view.findViewById(R.id.botonHoteles);
        btnHoteles.setSelected(true);
        btnHoteles.setOnClickListener(this);

        btnRestaurantes = (ImageButton) view.findViewById(R.id.botonRestaurantes);
        btnRestaurantes.setSelected(true);
        btnRestaurantes.setOnClickListener(this);

        btnAr = (ImageButton) view.findViewById(R.id.btnAumentedR);
        btnAr.setOnClickListener(this);

        popup = (LinearLayout) view.findViewById(R.id.contentPopup);
        categoria = (TextView) view.findViewById(R.id.categoria);
        nombreLugar = (TextView) view.findViewById(R.id.lugar);
        direccionLugar = (TextView) view.findViewById(R.id.direccion);
        fotoLugar = (ImageView) view.findViewById(R.id.fotoLugar);
        btnLlamar = (Button) view.findViewById(R.id.btnLlamar);
        btnLlamar.setOnClickListener(this);

        closePopup = (ImageButton) view.findViewById(R.id.closePopup);
        closePopup.setOnClickListener(this);

    }

    @Override
    public void onPause() {
        vistaMap.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        vistaMap.resume();
    }

    
    private void geoLocalizacion() {
        try {
            locationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                if (dataSourceStatusChangedEvent.getError() == null)
                    return;
                }
             });
            locationDisplay.startAsync();
            } catch (Exception e) {
                Toast.makeText(view.getContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.myLocationButton:
                locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                locationDisplay.startAsync();
                break;
            case R.id.botonParqueaderos:
                activarDesactivaLayer(policia, btnParqueaderos);
                break;
            case R.id.botonHoteles:
                activarDesactivaLayer(hospitales, btnHoteles);
                break;
            case R.id.botonRestaurantes:
                //activarDesactivaLayer(restaurantes, btnRestaurantes);
                break;
            case R.id.closePopup:
                popup.setVisibility(View.GONE);
                break;
            case R.id.btnAumentedR:
                Intent actAr = new Intent(view.getContext(), ARActivity.class);
                //actAr.putExtra("miMapa",mapaAumented);
                startActivity(actAr);
                break;
            case R.id.layersButton:
                activarFiltroLayers();
                break;
            case R.id.btnLlamar:
                callToNumber(telefonoG);
                break;
        }
    }

    private void mostrarPopup(String categoriaNombre, String nombre, String direccion, String foto, String telefono){
        //Log.e(MainActivity.TAG, nombre+", "+direccion+", "+foto);
        categoria.setText(categoriaNombre);
        nombreLugar.setText(nombre);
        direccionLugar.setText(direccion);
        btnLlamar.setText(telefono);
        telefonoG = telefono;
        if(foto != null){
            Ion.with(fotoLugar).load(foto);
        }else{
            Ion.with(fotoLugar).load("http://geoapps.esri.co/recursos/CCU2017/bogota.jpg");
        }

        popup.setVisibility(View.VISIBLE);
    }

    private void processIdentifyFeatureResult(Feature feature, LayerContent content){
        String nombre = "", direccion = "", foto = "", telefono = "";
        switch (content.getName()){
            case "EstacionesPolicia":
                nombre = (String) feature.getAttributes().get("Nombre");
                direccion = (String) feature.getAttributes().get("Direccion");
                telefono = (String) feature.getAttributes().get("TelefonoR");
                foto = (String) feature.getAttributes().get("Imagen");
                mostrarPopup("Seguridad", nombre, direccion, foto, telefono);
                break;
            case "Hospitales":
                nombre = (String) feature.getAttributes().get("Nombre");
                direccion = (String) feature.getAttributes().get("Direccion");
                telefono = (String) feature.getAttributes().get("TelefonoR");
                foto = (String) feature.getAttributes().get("Imagen");
                mostrarPopup("Medicina", nombre, direccion, foto, telefono);
                break;
            /**case "Restaurantes":
                nombre = (String) feature.getAttributes().get("Restaurante");
                direccion = (String) feature.getAttributes().get("Direccion");
                foto = (String) feature.getAttributes().get("Foto");
                mostrarPopup("Restaurante", nombre, direccion, foto);
                break;**/
        }
    }

    private void callToNumber(String number) {
        // If an error is found, handle the failure to start.
        // Check permissions to see if failure may be due to lack of permissions.
        boolean permissionCheck3 = ContextCompat.checkSelfPermission(this.getContext(), reqPermissions[2]) ==
        PackageManager.PERMISSION_GRANTED;

        if (!(permissionCheck3)) {
            // If permissions are not already granted, request permission from the user.
            ActivityCompat.requestPermissions(this.getActivity(), reqPermissions, requestCode);

        } else {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:031$number"));
            this.startActivity(intent);
        }
    }

    private void activarDesactivaLayer(FeatureLayer layer, ImageButton button){
        if(layer != null){
            if(button.isSelected()){
                button.setSelected(false);
                layer.setVisible(false);
            }else{
                button.setSelected(true);
                layer.setVisible(true);
            }
        }
    }

    private void activarFiltroLayers(){
        if(btnFilter.isSelected()){
            btnFilter.setSelected(false);
            layersFilter.setVisibility(View.GONE);
        }else{
            btnFilter.setSelected(true);
            layersFilter.setVisibility(View.VISIBLE);
        }
    }

    private void agregarLayer(FeatureLayer featureLayer){
        map.getOperationalLayers().add(featureLayer);
        //agregar pin
        //agregar pin al menu
        //extender metodo de reconocer capa
    }
}