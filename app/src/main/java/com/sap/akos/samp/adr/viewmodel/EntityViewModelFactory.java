package com.sap.akos.samp.adr.viewmodel;

import android.app.Application;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.os.Parcelable;

import com.sap.akos.samp.adr.viewmodel.customer.CustomerViewModel;
import com.sap.akos.samp.adr.viewmodel.productcategory.ProductCategoryViewModel;
import com.sap.akos.samp.adr.viewmodel.producttext.ProductTextViewModel;
import com.sap.akos.samp.adr.viewmodel.product.ProductViewModel;
import com.sap.akos.samp.adr.viewmodel.purchaseorderheader.PurchaseOrderHeaderViewModel;
import com.sap.akos.samp.adr.viewmodel.purchaseorderitem.PurchaseOrderItemViewModel;
import com.sap.akos.samp.adr.viewmodel.salesorderheader.SalesOrderHeaderViewModel;
import com.sap.akos.samp.adr.viewmodel.salesorderitem.SalesOrderItemViewModel;
import com.sap.akos.samp.adr.viewmodel.stock.StockViewModel;
import com.sap.akos.samp.adr.viewmodel.supplier.SupplierViewModel;


/**
 * Custom factory class, which can create view models for entity subsets, which are
 * reached from a parent entity through a navigation property.
 */
public class EntityViewModelFactory implements ViewModelProvider.Factory {

	// application class
    private Application application;
	// name of the navigation property
    private String navigationPropertyName;
	// parent entity
    private Parcelable entityData;

	/**
	 * Creates a factory class for entity view models created following a navigation link.
	 *
	 * @param application parent application
	 * @param navigationPropertyName name of the navigation link
	 * @param entityData parent entity
	 */
    public EntityViewModelFactory(Application application, String navigationPropertyName, Parcelable entityData) {
        this.application = application;
        this.navigationPropertyName = navigationPropertyName;
        this.entityData = entityData;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        T retValue = null;
		switch(modelClass.getSimpleName()) {



			case "CustomerViewModel":
				retValue = (T) new CustomerViewModel(application, navigationPropertyName, entityData);
				break;
			case "ProductCategoryViewModel":
				retValue = (T) new ProductCategoryViewModel(application, navigationPropertyName, entityData);
				break;
			case "ProductTextViewModel":
				retValue = (T) new ProductTextViewModel(application, navigationPropertyName, entityData);
				break;
			case "ProductViewModel":
				retValue = (T) new ProductViewModel(application, navigationPropertyName, entityData);
				break;
			case "PurchaseOrderHeaderViewModel":
				retValue = (T) new PurchaseOrderHeaderViewModel(application, navigationPropertyName, entityData);
				break;
			case "PurchaseOrderItemViewModel":
				retValue = (T) new PurchaseOrderItemViewModel(application, navigationPropertyName, entityData);
				break;
			case "SalesOrderHeaderViewModel":
				retValue = (T) new SalesOrderHeaderViewModel(application, navigationPropertyName, entityData);
				break;
			case "SalesOrderItemViewModel":
				retValue = (T) new SalesOrderItemViewModel(application, navigationPropertyName, entityData);
				break;
			case "StockViewModel":
				retValue = (T) new StockViewModel(application, navigationPropertyName, entityData);
				break;
			case "SupplierViewModel":
				retValue = (T) new SupplierViewModel(application, navigationPropertyName, entityData);
				break;
		}
		return retValue;
	}
}