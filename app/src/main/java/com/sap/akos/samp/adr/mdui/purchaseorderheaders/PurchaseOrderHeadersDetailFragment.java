package com.sap.akos.samp.adr.mdui.purchaseorderheaders;

import android.content.Intent;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.sap.akos.samp.adr.service.SAPServiceManager;
import com.sap.akos.samp.adr.R;
import com.sap.akos.samp.adr.databinding.FragmentPurchaseorderheadersDetailBinding;
import com.sap.akos.samp.adr.mdui.BundleKeys;
import com.sap.akos.samp.adr.mdui.InterfacedFragment;
import com.sap.akos.samp.adr.mdui.UIConstants;
import com.sap.akos.samp.adr.mdui.EntityKeyUtil;
import com.sap.akos.samp.adr.repository.OperationResult;
import com.sap.akos.samp.adr.viewmodel.purchaseorderheader.PurchaseOrderHeaderViewModel;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets;
import com.sap.cloud.android.odata.espmcontainer.PurchaseOrderHeader;
import com.sap.cloud.mobile.fiori.object.ObjectHeader;
import com.sap.cloud.mobile.odata.DataValue;
import com.sap.akos.samp.adr.mdui.purchaseorderitems.PurchaseOrderItemsActivity;
import com.sap.akos.samp.adr.mdui.suppliers.SuppliersActivity;

/**
 * A fragment representing a single PurchaseOrderHeader detail screen.
 * This fragment is contained in an PurchaseOrderHeadersActivity.
 */
public class PurchaseOrderHeadersDetailFragment extends InterfacedFragment<PurchaseOrderHeader> {

    /** Generated data binding class based on layout file */
    private FragmentPurchaseorderheadersDetailBinding binding;

    /** PurchaseOrderHeader entity to be displayed */
    private PurchaseOrderHeader purchaseOrderHeaderEntity = null;

    /** Fiori ObjectHeader component used when entity is to be displayed on phone */
    private ObjectHeader objectHeader;

    /** View model of the entity type that the displayed entity belongs to */
    private PurchaseOrderHeaderViewModel viewModel;

    /**
     * Service manager to provide root URL of OData Service for Glide to load images if there are media resources
     * associated with the entity type
     */
    private SAPServiceManager sapServiceManager;

    /** Arguments: PurchaseOrderHeader for display */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        menu = R.menu.itemlist_view_options;
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return setupDataBinding(inflater, container);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = new ViewModelProvider(currentActivity).get(PurchaseOrderHeaderViewModel.class);
        viewModel.getDeleteResult().observe(getViewLifecycleOwner(), this::onDeleteComplete);
        viewModel.getSelectedEntity().observe(getViewLifecycleOwner(), entity -> {
            purchaseOrderHeaderEntity = entity;
            binding.setPurchaseOrderHeader(entity);
            setupObjectHeader();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.update_item:
                listener.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, purchaseOrderHeaderEntity);
                return true;
            case R.id.delete_item:
                listener.onFragmentStateChange(UIConstants.EVENT_ASK_DELETE_CONFIRMATION,null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onNavigationClickedToPurchaseOrderItems_Items(View v) {
        Intent intent = new Intent(this.currentActivity, PurchaseOrderItemsActivity.class);
        intent.putExtra("parent", purchaseOrderHeaderEntity);
        intent.putExtra("navigation", "Items");
        startActivity(intent);
    }

    public void onNavigationClickedToSuppliers_SupplierDetails(View v) {
        Intent intent = new Intent(this.currentActivity, SuppliersActivity.class);
        intent.putExtra("parent", purchaseOrderHeaderEntity);
        intent.putExtra("navigation", "SupplierDetails");
        startActivity(intent);
    }


    /** Completion callback for delete operation */
    private void onDeleteComplete(@NonNull OperationResult<PurchaseOrderHeader> result) {
        if( progressBar != null ) {
            progressBar.setVisibility(View.INVISIBLE);
        }
        viewModel.removeAllSelected(); //to make sure the 'action mode' not activated in the list
        Exception ex = result.getError();
        if (ex != null) {
            showError(getString(R.string.delete_failed_detail));
            return;
        }
        listener.onFragmentStateChange(UIConstants.EVENT_DELETION_COMPLETED, purchaseOrderHeaderEntity);
    }

    /**
     * Set detail image of ObjectHeader.
     * When the entity does not provides picture, set the first character of the masterProperty.
     */
    private void setDetailImage(@NonNull ObjectHeader objectHeader, @NonNull PurchaseOrderHeader purchaseOrderHeaderEntity) {
        if (purchaseOrderHeaderEntity.getDataValue(PurchaseOrderHeader.currencyCode) != null && !purchaseOrderHeaderEntity.getDataValue(PurchaseOrderHeader.currencyCode).toString().isEmpty()) {
            objectHeader.setDetailImageCharacter(purchaseOrderHeaderEntity.getDataValue(PurchaseOrderHeader.currencyCode).toString().substring(0, 1));
        } else {
            objectHeader.setDetailImageCharacter("?");
        }
    }

    /**
     * Setup ObjectHeader with an instance of PurchaseOrderHeader
     */
    private void setupObjectHeader() {
        Toolbar secondToolbar = currentActivity.findViewById(R.id.secondaryToolbar);
        if (secondToolbar != null) {
            secondToolbar.setTitle(purchaseOrderHeaderEntity.getEntityType().getLocalName());
        } else {
            currentActivity.setTitle(purchaseOrderHeaderEntity.getEntityType().getLocalName());
        }

        // Object Header is not available in tablet mode
        objectHeader = currentActivity.findViewById(R.id.objectHeader);
        if (objectHeader != null) {
            // Use of getDataValue() avoids the knowledge of what data type the master property is.
            // This is a convenience for wizard generated code. Normally, developer will use the proxy class
            // get<Property>() method and add code to convert to string
            DataValue dataValue = purchaseOrderHeaderEntity.getDataValue(PurchaseOrderHeader.currencyCode);
            if (dataValue != null) {
                objectHeader.setHeadline(dataValue.toString());
            } else {
                objectHeader.setHeadline(null);
            }
            // EntityKey in string format: '{"key":value,"key2":value2}'
            objectHeader.setSubheadline(EntityKeyUtil.getOptionalEntityKey(purchaseOrderHeaderEntity));
            objectHeader.setTag("#tag1", 0);
            objectHeader.setTag("#tag3", 2);
            objectHeader.setTag("#tag2", 1);

            objectHeader.setBody("You can set the header body text here.");
            objectHeader.setFootnote("You can set the header footnote here.");
            objectHeader.setDescription("You can add a detailed item description here.");

            setDetailImage(objectHeader, purchaseOrderHeaderEntity);
            objectHeader.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set up databinding for this view
     *
     * @param inflater - layout inflater from onCreateView
     * @param container - view group from onCreateView
     * @return view - rootView from generated databinding code
     */
    private View setupDataBinding(LayoutInflater inflater, ViewGroup container) {
        binding = FragmentPurchaseorderheadersDetailBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();
        binding.setHandler(this);
        return rootView;
    }
}
