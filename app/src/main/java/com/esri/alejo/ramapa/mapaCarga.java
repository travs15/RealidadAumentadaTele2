package com.esri.alejo.ramapa;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.LayerList;

import java.io.Serializable;

/**
 * Created by alejo on 2/02/2018.
 */

public class mapaCarga implements Serializable {

    public ArcGISMap mapa;
    public LayerList layers;

    public mapaCarga(String urlMapa){
        mapa = new ArcGISMap(urlMapa);
        layers = mapa.getOperationalLayers();
    }

    public ArcGISMap getMap(){
        return mapa;
    }

    public LayerList getLayers(){
        return layers;
    }

}
