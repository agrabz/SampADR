package com.sap.akos.samp.adr.mdui.products;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;
import com.sap.akos.samp.adr.R;
import com.sap.akos.samp.adr.app.SAPWizardApplication;
import com.sap.akos.samp.adr.mdui.BundleKeys;
import com.sap.akos.samp.adr.mdui.EntitySetListActivity;
import com.sap.akos.samp.adr.mdui.EntitySetListActivity.EntitySetName;
import com.sap.akos.samp.adr.mdui.InterfacedFragment;
import com.sap.akos.samp.adr.mdui.UIConstants;
import com.sap.akos.samp.adr.mdui.EntityKeyUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.sap.akos.samp.adr.service.SAPServiceManager;
import com.sap.akos.samp.adr.repository.OperationResult;
import com.sap.akos.samp.adr.viewmodel.EntityViewModelFactory;
import com.sap.akos.samp.adr.viewmodel.product.ProductViewModel;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets;
import com.sap.cloud.android.odata.espmcontainer.Product;
import com.sap.cloud.mobile.fiori.object.ObjectCell;
import com.sap.cloud.mobile.odata.DataValue;
import com.sap.cloud.mobile.odata.EntityValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import com.sap.akos.samp.adr.mediaresource.EntityMediaResource;
import android.widget.LinearLayout;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainer;
import com.sap.cloud.android.odata.espmcontainer.SalesOrderItem;
import com.sap.cloud.mobile.fiori.common.FioriItemClickListener;
import com.sap.cloud.mobile.fiori.object.AbstractEntityCell;
import com.sap.cloud.mobile.fiori.object.CollectionView;
import com.sap.cloud.mobile.fiori.object.CollectionViewItem;
import com.sap.cloud.mobile.odata.DataQuery;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ProductsListFragment extends InterfacedFragment<Product> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductsActivity.class);
    private List<Product> productList = new ArrayList<>();
    private HashMap<String, Integer> salesList = new HashMap<>();
    private HashMap<String, Product> productTracker = new HashMap<>();

    /**
     * Service manager to provide root URL of OData Service for Glide to load images if there are media resources
     * associated with the entity type
     */
    private SAPServiceManager sapServiceManager;

    /**
     * List adapter to be used with RecyclerView containing all instances of products
     */
    private ProductListAdapter adapter;

    private SwipeRefreshLayout refreshLayout;

    /**
     * View model of the entity type
     */
    private ProductViewModel viewModel;

    private ActionMode actionMode;
    private Boolean isInActionMode = false;
    private List<Integer> selectedItems = new ArrayList<>();

    // Function to sort hashmap by values
    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer>> list = new LinkedList<>(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // Put data from sorted list to linked hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
            LOGGER.debug("CollectionView: id = " + aa.getKey() + ", count = " + aa.getValue());
        }
        return temp;
    }

    // Function to query the products
    private void queryProducts() {
        SAPServiceManager sapServiceManager = ((SAPWizardApplication) currentActivity.getApplication()).getSAPServiceManager();
        ESPMContainer espmContainer = sapServiceManager.getESPMContainer();
        DataQuery query = new DataQuery().orderBy(Product.productID);
        LOGGER.debug("CollectionView" + query.toString());
        espmContainer.getProductsAsync(query, (List<Product> queryProducts) -> {
            LOGGER.debug("CollectionView: executed query in onCreate");
            for (Product product : queryProducts) {
                LOGGER.debug("CollectionView" + product.getName() + " : " + product.getProductID() + " : " + product.getPrice());
                productTracker.put(product.getProductID(), product);
            }

            LOGGER.debug("CollectionView: size of topProducts = " + queryProducts.size());
            createTopProductsList();
            CollectionView cv = currentActivity.findViewById(R.id.collectionView);
            createCollectionView(cv);
        }, (RuntimeException re) -> {
            LOGGER.debug("CollectionView: An error occurred during products async query: " + re.getMessage());
        });
    }

    // Function to order product list by the sorted sales list
    private void createTopProductsList() {
        Iterator it = salesList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            productList.add(productTracker.get(pair.getKey()));
            it.remove();
        }
    }

    // Function to set features of the CollectionView
    private void createCollectionView(CollectionView cv) {
        LOGGER.debug("CollectionView: in createCollectionView method");
        cv.setHeader(" Top Products");
        cv.setFooter(" SEE ALL (" + productTracker.size() + ")");

        // If the footer "SEE ALL" is clicked then the Products page will open
        cv.setFooterClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cv.setVisibility(View.GONE);
            }
        });

        // If any object is clicked in CollectionView then the Product's detail page for that object will open
        cv.setItemClickListener(new FioriItemClickListener() {
            @Override
            public void onClick(@NonNull View view, int position) {
                if (productList.get(position) != null) {
                    LOGGER.debug("You clicked on: " + productList.get(position).getName() + "(" + productList.get(position).getProductID() + ")");
                    showProductDetailActivity(view.getContext(), UIConstants.OP_READ, productList.get(position));
                }

            }

            @Override
            public void onLongClick(@NonNull View view, int position) {
                Toast.makeText(currentActivity.getApplicationContext(), "You long clicked on: " + position, Toast.LENGTH_SHORT).show();
            }
        });

        CollectionViewAdapter collectionViewAdapter = new CollectionViewAdapter();
        cv.setCollectionViewAdapter(collectionViewAdapter);

        if (getResources().getBoolean(R.bool.two_pane)) {
            refreshLayout = currentActivity.findViewById(R.id.swiperefresh);
            LinearLayout linearLayout = currentActivity.findViewById(R.id.wrapperLayout);
            int height = linearLayout.getHeight() - cv.getHeight();
            refreshLayout.setMinimumHeight(height);
        }
    }

    // Opens the product's detail page activity
    private void showProductDetailActivity(@NonNull Context context, @NonNull String operation, @Nullable Product productEntity) {
        LOGGER.debug("within showProductDetailActivity for " + productEntity.getName());
        boolean isNavigationDisabled = ((ProductsActivity) currentActivity).isNavigationDisabled;
        if (isNavigationDisabled) {
            Toast.makeText(currentActivity, "Please save your changes first...", Toast.LENGTH_LONG).show();
        } else {
            adapter.resetSelected();
            adapter.resetPreviouslyClicked();
            viewModel.setSelectedEntity(productEntity);
            listener.onFragmentStateChange(UIConstants.EVENT_ITEM_CLICKED, productEntity);
        }
    }

    public class CollectionViewAdapter extends CollectionView.CollectionViewAdapter {
        private List<Product> products;

        public CollectionViewAdapter() {
            products = productList;
        }

        @Override
        public void onBindViewHolder(@NonNull CollectionViewItemHolder collectionViewItemHolder, int i) {
            CollectionViewItem cvi = collectionViewItemHolder.collectionViewItem;
            Product prod = products.get(i);
            if (prod != null) {
                String productName = prod.getName();

                cvi.setDetailImage(null);
                cvi.setHeadline(productName);
                cvi.setSubheadline(prod.getCategoryName() + "");
                cvi.setImageOutlineShape(AbstractEntityCell.IMAGE_SHAPE_OVAL);

                if (prod.getPictureUrl() == null) {
                    // No picture is available, so use a character from the product string as the image thumbnail
                    cvi.setDetailImageCharacter(productName.substring(0, 1));
                    cvi.setDetailCharacterBackgroundTintList(com.sap.cloud.mobile.fiori.R.color.sap_ui_contact_placeholder_color_1);
                } else {
                    SAPServiceManager sapServiceManager = ((SAPWizardApplication)currentActivity.getApplication()).getSAPServiceManager();
                    cvi.prepareDetailImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Glide.with(currentActivity.getApplicationContext())
                            .load(EntityMediaResource.getMediaResourceUrl(prod, sapServiceManager.getServiceRoot()))
                            // Import com.bumptech.glide.Glide for RequestOptions()
                            .apply(new RequestOptions().fitCenter())
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(cvi.prepareDetailImageView());
                }
            }

        }

        @Override
        public int getItemCount() {
            return products.size();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshLayout.setRefreshing(true);
                refreshListData();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityTitle = getString(EntitySetListActivity.EntitySetName.Products.getTitleId());
        menu = R.menu.itemlist_menu;
        setHasOptionsMenu(true);
        if( savedInstanceState != null ) {
            isInActionMode = savedInstanceState.getBoolean("ActionMode");
        }

        sapServiceManager = ((SAPWizardApplication) currentActivity.getApplication()).getSAPServiceManager();

        // Get the DataService class, which we will use to query the back-end OData service
        ESPMContainer espmContainer = sapServiceManager.getESPMContainer();

        // Query the SalesOrderItems and order by gross amount received from sales
        DataQuery dq = new DataQuery().orderBy(SalesOrderItem.productID);
        espmContainer.getSalesOrderItemsAsync(dq, (List<SalesOrderItem> querySales) -> {
            LOGGER.debug("CollectionView: executed sales order query in onCreate");
            if (querySales != null) {
                for (SalesOrderItem sale : querySales) {
                    if (salesList.containsKey(sale.getProductID())) {
                        salesList.put(sale.getProductID(), salesList.get(sale.getProductID()).intValue() + sale.getQuantity().intValue());
                    } else {
                        salesList.put(sale.getProductID(), sale.getQuantity().intValue());
                    }
                    LOGGER.debug("CollectionView" + sale.getProductID() + ": " + sale.getQuantity() + ": " + sale.getGrossAmount());
                }

                salesList = sortByValue(salesList);

                LOGGER.debug("CollectionView: salesList size = " + salesList.size());
                queryProducts();
            } else {
                LOGGER.debug("CollectionView: sales query list is null");
            }
        }, (RuntimeException re) -> {
            LOGGER.debug("CollectionView: An error occurred during async sales query: " + re.getMessage());
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View objectHeader = currentActivity.findViewById(R.id.objectHeader);
        if( objectHeader != null) {
            objectHeader.setVisibility(View.GONE);
        }
        return inflater.inflate(R.layout.fragment_entityitem_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(this.menu, menu);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        currentActivity.setTitle(activityTitle);
        RecyclerView recyclerView = currentActivity.findViewById(R.id.item_list);
        if (recyclerView == null) throw new AssertionError();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(currentActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setLayoutManager(linearLayoutManager);
        this.adapter = new ProductListAdapter(currentActivity);
        recyclerView.setAdapter(adapter);

        setupRefreshLayout();
        refreshLayout.setRefreshing(true);

        navigationPropertyName = currentActivity.getIntent().getStringExtra("navigation");
        parentEntityData = currentActivity.getIntent().getParcelableExtra("parent");

        FloatingActionButton floatButton = currentActivity.findViewById(R.id.fab);
        if (floatButton != null) {
            if (navigationPropertyName != null && parentEntityData != null) {
                floatButton.hide();
            } else {
                floatButton.setOnClickListener((v) -> {
                    listener.onFragmentStateChange(UIConstants.EVENT_CREATE_NEW_ITEM, null);
                });
            }
        }

        sapServiceManager.openODataStore(() -> {
            prepareViewModel();
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("ActionMode", isInActionMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshListData();
    }

    /** Initializes the view model and add observers on it */
    private void prepareViewModel() {
        if( navigationPropertyName != null && parentEntityData != null ) {
            viewModel = new ViewModelProvider(currentActivity, new EntityViewModelFactory(currentActivity.getApplication(), navigationPropertyName, parentEntityData))
                .get(ProductViewModel.class);
        } else {
            viewModel = new ViewModelProvider(currentActivity).get(ProductViewModel.class);
            viewModel.initialRead(this::showError);
            CollectionView cv = currentActivity.findViewById(R.id.collectionView);
            createCollectionView(cv);
        }

        viewModel.getObservableItems().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                String category = currentActivity.getIntent().getStringExtra("category");
                if (category != null) {
                    List<Product> matchingProducts = new ArrayList<>();
                    for (Product product : products) {
                        if (product.getCategory() != null && product.getCategory().equals(category)) {
                            matchingProducts.add(product);
                        }
                    }
                    adapter.setItems(matchingProducts);
                } else {
                    adapter.setItems(products);
                }

                Product item = containsItem(products, viewModel.getSelectedEntity().getValue());
                if (item == null) {
                    item = products.isEmpty() ? null : products.get(0);
                }

                if (item == null) {
                    hideDetailFragment();
                } else {
                    viewModel.setInFocusId(adapter.getItemIdForProduct(item));
                    if (currentActivity.getResources().getBoolean(R.bool.two_pane)) {
                        viewModel.setSelectedEntity(item);
                        if (!isInActionMode && !((ProductsActivity) currentActivity).isNavigationDisabled) {
                            listener.onFragmentStateChange(UIConstants.EVENT_ITEM_CLICKED, item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            refreshLayout.setRefreshing(false);
        });

        viewModel.getReadResult().observe(getViewLifecycleOwner(), state -> {
            if (refreshLayout.isRefreshing()) {
                refreshLayout.setRefreshing(false);
            }
        });

        viewModel.getDeleteResult().observe(getViewLifecycleOwner(), result -> {
            onDeleteComplete(result);
        });
    }

    /** Searches 'item' in the refreshed list, if found, returns the one in list */
    private Product containsItem(List<Product> items, Product item) {
        Product found = null;
        if( item != null ) {
            for( Product entity: items ) {
                if( adapter.getItemIdForProduct(entity) == adapter.getItemIdForProduct(item)) {
                    found = entity;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Hides the detail fragment.
     */
    private void hideDetailFragment() {
        Fragment detailFragment = currentActivity.getSupportFragmentManager().findFragmentByTag(UIConstants.DETAIL_FRAGMENT_TAG);
        if( detailFragment != null ) {
            currentActivity.getSupportFragmentManager().beginTransaction()
                .remove(detailFragment).commit();
        }
        if( secondaryToolbar != null ) {
            secondaryToolbar.getMenu().clear();
            secondaryToolbar.setTitle("");
        }
        View objectHeader = currentActivity.findViewById(R.id.objectHeader);
        if( objectHeader != null) {
            objectHeader.setVisibility(View.GONE);
        }
    }

    /** Callback function for delete operation */
    private void onDeleteComplete(@NonNull OperationResult<Product> result) {
        if( progressBar != null ) {
            progressBar.setVisibility(View.INVISIBLE);
        }
        viewModel.removeAllSelected();
        if (actionMode != null) {
            actionMode.finish();
            isInActionMode = false;
        }

        if (result.getError() != null) {
            handleDeleteError();
        } else {
            refreshListData();
        }
    }

    /** Refreshes the list data */
    void refreshListData() {
        if (navigationPropertyName != null && parentEntityData != null) {
            viewModel.refresh((EntityValue) parentEntityData, navigationPropertyName);
        } else {
            viewModel.refresh();
        }
        adapter.notifyDataSetChanged();
    }

    /** Sets the selected item id into view model */
    private Product setItemIdSelected(int itemId) {
        LiveData<List<Product>> liveData = viewModel.getObservableItems();
        List<Product> products = liveData.getValue();
        if (products != null && products.size() > 0) {
            viewModel.setInFocusId(adapter.getItemIdForProduct(products.get(itemId)));
            return products.get(itemId);
        }
        return null;
    }

    /** Sets up the refresh layout */
    private void setupRefreshLayout() {
        refreshLayout = currentActivity.findViewById(R.id.swiperefresh);
        refreshLayout.setColorSchemeColors(UIConstants.FIORI_STANDARD_THEME_GLOBAL_DARK_BASE);
        refreshLayout.setProgressBackgroundColorSchemeColor(UIConstants.FIORI_STANDARD_THEME_BACKGROUND);
        refreshLayout.setOnRefreshListener(this::refreshListData);
    }

    /** Callback function to handle deletion error */
    private void handleDeleteError() {
        showError(getResources().getString(R.string.delete_failed_detail));
        refreshLayout.setRefreshing(false);
    }

    /**
     * Represents the action mode of the list.
     */
    public class ProductsListActionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            isInActionMode = true;
            FloatingActionButton fab = currentActivity.findViewById(R.id.fab);
            if (fab != null) {
                fab.hide();
            }
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.itemlist_view_options, menu);
            hideDetailFragment();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.update_item:
                    Product productEntity = viewModel.getSelected(0);
                    if (viewModel.numberOfSelected() == 1 && productEntity != null) {
                        isInActionMode = false;
                        actionMode.finish();
                        viewModel.setSelectedEntity(productEntity);
                        if( currentActivity.getResources().getBoolean(R.bool.two_pane)) {
                            listener.onFragmentStateChange(UIConstants.EVENT_ITEM_CLICKED, productEntity);
                        }
                        listener.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, productEntity);
                    }
                    return true;
                case R.id.delete_item:
                    listener.onFragmentStateChange(UIConstants.EVENT_ASK_DELETE_CONFIRMATION, null);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            isInActionMode = false;
            if (!(navigationPropertyName != null && parentEntityData != null)) {
                FloatingActionButton fab = currentActivity.findViewById(R.id.fab);
                if (fab != null) {
                    fab.show();
                }
            }
            selectedItems.clear();
            viewModel.removeAllSelected();
            refreshListData();
        }
    }

    /**
     * List adapter to be used with RecyclerView. It contains the set of products.
     */
    public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

        private Context context;

        /** Entire list of Product collection */
        private List<Product> products;

        /** RecyclerView this adapter is associate with */
        private RecyclerView recyclerView;

        /** Flag to indicate whether we have checked retained selected products */
        private boolean checkForSelectedOnCreate = false;

        public ProductListAdapter(Context context) {
            this.context = context;
            this.recyclerView = currentActivity.findViewById(R.id.item_list);
            if (this.recyclerView == null) throw new AssertionError();
            setHasStableIds(true);
        }

        /**
         * Use DiffUtil to calculate the difference and dispatch them to the adapter
         * Note: Please use background thread for calculation if the list is large to avoid blocking main thread
         */
        @WorkerThread
        public void setItems(@NonNull List<Product> currentProducts) {
            if (products == null) {
                products = new ArrayList<>(currentProducts);
                notifyItemRangeInserted(0, currentProducts.size());
            } else {
                DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return products.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return currentProducts.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return products.get(oldItemPosition).getEntityKey().toString().equals(
                                currentProducts.get(newItemPosition).getEntityKey().toString());
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        Product productEntity = products.get(oldItemPosition);
                        return !productEntity.isUpdated() && currentProducts.get(newItemPosition).equals(productEntity);
                    }

                    @Nullable
                    @Override
                    public Object getChangePayload(final int oldItemPosition, final int newItemPosition) {
                        return super.getChangePayload(oldItemPosition, newItemPosition);
                    }
                });
                products.clear();
                products.addAll(currentProducts);
                result.dispatchUpdatesTo(this);
            }
        }

        @Override
        public final long getItemId(int position) {
            return getItemIdForProduct(products.get(position));
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.element_entityitem_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

            checkForRetainedSelection();

            final Product productEntity = products.get(holder.getAdapterPosition());
            DataValue dataValue = productEntity.getDataValue(Product.category);
            if (dataValue != null) {
                holder.masterPropertyValue = dataValue.toString();
            }
            populateObjectCell(holder, productEntity);

            boolean isActive = getItemIdForProduct(productEntity) == viewModel.getInFocusId();
            if (isActive) {
                setItemIdSelected(holder.getAdapterPosition());
            }
            boolean isProductSelected = viewModel.selectedContains(productEntity);
            setViewBackground(holder.objectCell, isProductSelected, isActive);

            holder.view.setOnLongClickListener(new onActionModeStartClickListener(holder));
            setOnClickListener(holder, productEntity);

            setOnCheckedChangeListener(holder, productEntity);
            holder.setSelected(isProductSelected);
            setDetailImage(holder, productEntity);
        }

        /**
         * Check to see if there are an retained selected productEntity on start.
         * This situation occurs when a rotation with selected products is triggered by user.
         */
        private void checkForRetainedSelection() {
            if (!checkForSelectedOnCreate) {
                checkForSelectedOnCreate = true;
                if (viewModel.numberOfSelected() > 0) {
                    manageActionModeOnCheckedTransition();
                }
            }
        }

        /**
         * If there are selected products via long press, clear them as click and long press are mutually exclusive
         * In addition, since we are clearing all selected products via long press, finish the action mode.
         */
        private void resetSelected() {
            if (viewModel.numberOfSelected() > 0) {
                viewModel.removeAllSelected();
                if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
            }
        }

        /**
         * Attempt to locate previously clicked view and reset its background
         * Reset view model's inFocusId
         */
        private void resetPreviouslyClicked() {
            long inFocusId = viewModel.getInFocusId();
            ViewHolder viewHolder = (ViewHolder) recyclerView.findViewHolderForItemId(inFocusId);
            if (viewHolder != null) {
                setViewBackground(viewHolder.objectCell, viewHolder.isSelected, false);
            } else {
                viewModel.refresh();
            }
        }

        private void processClickAction(@NonNull ViewHolder viewHolder, @NonNull Product productEntity) {
            resetPreviouslyClicked();
            setViewBackground(viewHolder.objectCell, false, true);
            viewModel.setInFocusId(getItemIdForProduct(productEntity));
        }

        /**
         * Set ViewHolder's view onClickListener
         *
         * @param holder
         * @param productEntity associated with this ViewHolder
         */
        private void setOnClickListener(@NonNull ViewHolder holder, @NonNull Product productEntity) {
            holder.view.setOnClickListener(view -> {
                boolean isNavigationDisabled = ((ProductsActivity) currentActivity).isNavigationDisabled;
                if(isNavigationDisabled) {
                    Toast.makeText(currentActivity, "Please save your changes first...", Toast.LENGTH_LONG).show();
                } else {
                    resetSelected();
                    resetPreviouslyClicked();
                    processClickAction(holder, productEntity);
                    viewModel.setSelectedEntity(productEntity);
                    listener.onFragmentStateChange(UIConstants.EVENT_ITEM_CLICKED, productEntity);
                }
            });
        }

        /**
         * Represents the listener to start the action mode
         */
        public class onActionModeStartClickListener implements View.OnClickListener, View.OnLongClickListener {

            ViewHolder holder;

            public onActionModeStartClickListener(@NonNull ViewHolder viewHolder) {
                this.holder = viewHolder;
            }

            @Override
            public void onClick(View view) {
                onAnyKindOfClick();
            }

            @Override
            public boolean onLongClick(View view) {
                return onAnyKindOfClick();
            }

            /** callback function for both normal and long click on an entity */
            private boolean onAnyKindOfClick() {
                boolean isNavigationDisabled = ((ProductsActivity) currentActivity).isNavigationDisabled;
                if( isNavigationDisabled ) {
                    Toast.makeText(currentActivity, "Please save your changes first...", Toast.LENGTH_LONG).show();
                } else {
                    if (!isInActionMode) {
                        actionMode = ((AppCompatActivity) currentActivity).startSupportActionMode(new ProductsListActionMode());
                        adapter.notifyDataSetChanged();
                    }
                    holder.setSelected(!holder.isSelected);
                }
                return true;
            }
        }

        /** sets the detail image to the given <code>viewHolder</code> */
        private void setDetailImage(@NonNull ViewHolder viewHolder, @NonNull Product productEntity) {
            if (isInActionMode) {
                int drawable;
                if (viewHolder.isSelected) {
                    drawable = R.drawable.ic_check_circle_black_24dp;
                } else {
                    drawable = R.drawable.ic_uncheck_circle_black_24dp;
                }
                viewHolder.objectCell.prepareDetailImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
                Glide.with(context)
                        .load(getResources().getDrawable(drawable, null))
                        .apply(new RequestOptions().fitCenter())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(viewHolder.objectCell.prepareDetailImageView());
            } else if (EntityMediaResource.hasMediaResources(EntitySets.products)) {
                viewHolder.objectCell.prepareDetailImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
                Glide.with(context)
                        .load(EntityMediaResource.getMediaResourceUrl(productEntity, sapServiceManager.getServiceRoot()))
                        .apply(new RequestOptions().fitCenter())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(viewHolder.objectCell.prepareDetailImageView());
            } else if (viewHolder.masterPropertyValue != null && !viewHolder.masterPropertyValue.isEmpty()) {
                viewHolder.objectCell.setDetailImageCharacter(viewHolder.masterPropertyValue.substring(0, 1));
            } else {
                viewHolder.objectCell.setDetailImageCharacter("?");
            }
        }

        /**
         * Set ViewHolder's CheckBox onCheckedChangeListener
         *
         * @param holder
         * @param productEntity associated with this ViewHolder
         */
        private void setOnCheckedChangeListener(@NonNull ViewHolder holder, @NonNull Product productEntity) {
            holder.checkBox.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    viewModel.addSelected(productEntity);
                    manageActionModeOnCheckedTransition();
                    resetPreviouslyClicked();
                } else {
                    viewModel.removeSelected(productEntity);
                    manageActionModeOnUncheckedTransition();
                }
                setViewBackground(holder.objectCell, viewModel.selectedContains(productEntity), false);
                setDetailImage(holder, productEntity);
            });
        }

        /*
         * Start Action Mode if it has not been started
         * This is only called when long press action results in a selection. Hence action mode may
         * not have been started. Along with starting action mode, title will be set.
         * If this is an additional selection, adjust title appropriately.
         */
        private void manageActionModeOnCheckedTransition() {
            if (actionMode == null) {
                actionMode = ((AppCompatActivity) currentActivity).startSupportActionMode(new ProductsListActionMode());
            }
            if (viewModel.numberOfSelected() > 1) {
                actionMode.getMenu().findItem(R.id.update_item).setVisible(false);
            }
            actionMode.setTitle(String.valueOf(viewModel.numberOfSelected()));
        }

        /*
         * This is called when one of the selected products has been de-selected
         * On this event, we will determine if update action needs to be made visible or
         * action mode should be terminated (no more selected)
         */
        private void manageActionModeOnUncheckedTransition() {
            switch (viewModel.numberOfSelected()) {
                case 1:
                    actionMode.getMenu().findItem(R.id.update_item).setVisible(true);
                    break;

                case 0:
                    if (actionMode != null) {
                        actionMode.finish();
                        actionMode = null;
                    }
                    return;

                default:
            }
            actionMode.setTitle(String.valueOf(viewModel.numberOfSelected()));
        }

        private void populateObjectCell(@NonNull ViewHolder viewHolder, @NonNull Product productEntity) {

            DataValue dataValue = productEntity.getDataValue(Product.name);
            String masterPropertyValue = null;
            if (dataValue != null) {
                masterPropertyValue = dataValue.toString();
            }
            viewHolder.objectCell.setHeadline(masterPropertyValue);
            viewHolder.objectCell.setDetailImage(null);
            setDetailImage(viewHolder, productEntity);

            dataValue = productEntity.getDataValue(Product.category);

            if (dataValue != null) {
                viewHolder.objectCell.setSubheadline(dataValue.toString());
            }
            dataValue = productEntity.getDataValue(Product.shortDescription);
            if (dataValue != null) {
                viewHolder.objectCell.setFootnote(dataValue.toString());
            }

            dataValue = productEntity.getDataValue(Product.price);
            if (dataValue != null) {
                viewHolder.objectCell.setStatusWidth(200);
                viewHolder.objectCell.setStatus("$ " + dataValue.toString(), 1);
            }
        }

        /**
         * Set background of view to indicate productEntity selection status
         * Selected and Active are mutually exclusive. Only one can be true
         *
         * @param view
         * @param isProductSelected - true if productEntity is selected via long press action
         * @param isActive               - true if productEntity is selected via click action
         */
        private void setViewBackground(@NonNull View view, boolean isProductSelected, boolean isActive) {
            boolean isMasterDetailView = currentActivity.getResources().getBoolean(R.bool.two_pane);
            if (isProductSelected) {
                view.setBackground(ContextCompat.getDrawable(context, R.drawable.list_item_selected));
            } else if (isActive && isMasterDetailView && !isInActionMode) {
                view.setBackground(ContextCompat.getDrawable(context, R.drawable.list_item_active));
            } else {
                view.setBackground(ContextCompat.getDrawable(context, R.drawable.list_item_default));
            }
        }

        @Override
        public int getItemCount() {
            if (products == null) {
                return 0;
            } else {
                return products.size();
            }
        }

        /**
         * Computes a stable ID for each Product object for use to locate the ViewHolder
         *
         * @param productEntity
         * @return an ID based on the primary key of Product
         */
        private long getItemIdForProduct(Product productEntity) {
            return productEntity.getEntityKey().toString().hashCode();
        }

        /**
         * ViewHolder for RecyclerView.
         * Each view has a Fiori ObjectCell and a checkbox (used by long press)
         */
        public class ViewHolder extends RecyclerView.ViewHolder {

            public final View view;

            public boolean isSelected;

            public String masterPropertyValue;

            /** Fiori ObjectCell to display productEntity in list */
            public final ObjectCell objectCell;

            /** Checkbox for long press selection */
            public final CheckBox checkBox;

            public ViewHolder(View view) {
                super(view);
                this.view = view;
                objectCell = view.findViewById(R.id.content);
                checkBox = view.findViewById(R.id.cbx);
                isSelected = false;
            }

            public void setSelected(Boolean selected) {
                isSelected = selected;
                checkBox.setChecked(selected);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + objectCell.getDescription() + "'";
            }
        }
    }
}
