package com.sap.akos.samp.adr.viewmodel.product;

import android.app.Application;
import android.os.Parcelable;

import com.sap.akos.samp.adr.viewmodel.EntityViewModel;
import com.sap.cloud.android.odata.espmcontainer.Product;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets;

/*
 * Represents View model for Product
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and
 * return the view model of that type. This is because the ViewModelStore of
 * ViewModelProvider cannot not be able to tell the difference between EntityViewModel<type1>
 * and EntityViewModel<type2>.
 */
public class ProductViewModel extends EntityViewModel<Product> {

    /**
    * Default constructor for a specific view model.
    * @param application - parent application
    */
    public ProductViewModel(Application application) {
        super(application, EntitySets.products, Product.category);
    }

    /**
    * Constructor for a specific view model with navigation data.
    * @param application - parent application
    * @param navigationPropertyName - name of the navigation property
    * @param entityData - parent entity (starting point of the navigation)
    */
	 public ProductViewModel(Application application, String navigationPropertyName, Parcelable entityData) {
        super(application, EntitySets.products, Product.category, navigationPropertyName, entityData);
    }
}
