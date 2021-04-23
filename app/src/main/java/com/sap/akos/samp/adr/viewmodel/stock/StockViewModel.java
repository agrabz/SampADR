package com.sap.akos.samp.adr.viewmodel.stock;

import android.app.Application;
import android.os.Parcelable;

import com.sap.akos.samp.adr.viewmodel.EntityViewModel;
import com.sap.cloud.android.odata.espmcontainer.Stock;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets;

/*
 * Represents View model for Stock
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and
 * return the view model of that type. This is because the ViewModelStore of
 * ViewModelProvider cannot not be able to tell the difference between EntityViewModel<type1>
 * and EntityViewModel<type2>.
 */
public class StockViewModel extends EntityViewModel<Stock> {

    /**
    * Default constructor for a specific view model.
    * @param application - parent application
    */
    public StockViewModel(Application application) {
        super(application, EntitySets.stock, Stock.lotSize);
    }

    /**
    * Constructor for a specific view model with navigation data.
    * @param application - parent application
    * @param navigationPropertyName - name of the navigation property
    * @param entityData - parent entity (starting point of the navigation)
    */
	 public StockViewModel(Application application, String navigationPropertyName, Parcelable entityData) {
        super(application, EntitySets.stock, Stock.lotSize, navigationPropertyName, entityData);
    }
}
