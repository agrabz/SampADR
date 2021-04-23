package com.sap.akos.samp.adr.mdui.customers;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.sap.akos.samp.adr.mdui.BundleKeys;
import com.sap.cloud.android.odata.espmcontainer.Customer;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainer;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata;
import com.sap.cloud.mobile.fiori.formcell.ChoiceFormCell;
import com.sap.cloud.mobile.fiori.formcell.FormCell;
import com.sap.cloud.mobile.fiori.formcell.SwitchFormCell;
import com.sap.cloud.mobile.fiori.maps.ActionCell;
import com.sap.cloud.mobile.fiori.maps.AnnotationInfoAdapter;
import com.sap.cloud.mobile.fiori.maps.FioriCircleOptions;
import com.sap.cloud.mobile.fiori.maps.FioriMapSearchView;
import com.sap.cloud.mobile.fiori.maps.FioriMarkerOptions;
import com.sap.cloud.mobile.fiori.maps.FioriPoint;
import com.sap.cloud.mobile.fiori.maps.FioriPolygonOptions;
import com.sap.cloud.mobile.fiori.maps.FioriPolylineOptions;
import com.sap.cloud.mobile.fiori.maps.LegendButton;
import com.sap.cloud.mobile.fiori.maps.LocationButton;
import com.sap.cloud.mobile.fiori.maps.MapListPanel;
import com.sap.cloud.mobile.fiori.maps.MapPreviewPanel;
import com.sap.cloud.mobile.fiori.maps.SettingsButton;
import com.sap.cloud.mobile.fiori.maps.ZoomExtentButton;
import com.sap.cloud.mobile.fiori.maps.edit.PointAnnotation;
import com.sap.cloud.mobile.fiori.maps.edit.PolygonAnnotation;
import com.sap.cloud.mobile.fiori.maps.edit.PolylineAnnotation;
import com.sap.cloud.mobile.fiori.maps.google.GoogleFioriMapView;
import com.sap.cloud.mobile.fiori.maps.google.GoogleMapActionProvider;
import com.sap.cloud.mobile.fiori.maps.google.GoogleMapViewModel;
import com.sap.cloud.mobile.fiori.object.ObjectCell;
import com.sap.cloud.mobile.fiori.object.ObjectHeader;
import com.sap.cloud.mobile.odata.DataQuery;
import com.sap.akos.samp.adr.R;
import com.sap.akos.samp.adr.app.SAPWizardApplication;
import com.sap.akos.samp.adr.service.SAPServiceManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class CustomersFioriMapActivity extends AppCompatActivity implements GoogleFioriMapView.OnMapCreatedListener {
    private GoogleFioriMapView mGoogleFioriMapView;
    private boolean mUseClustering = false;
    private int mMapType;
    private HashMap<String, LatLng> locations = new HashMap<String, LatLng>();  // Used for demo purposes to speed up the process of converting an address to lat, long
    private HashMap<String, FioriMarkerOptions> markers = new HashMap<String, FioriMarkerOptions>();  // Used to associate an address with a marker for search
    private ArrayList<String> addresses = new ArrayList<String>();  // Used to populate the list of addresses that are searchable

    GoogleMapActionProvider mActionProvider;
    private MapListPanel mMapListPanel;
    private MapResultsAdapter mMapResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_fiori_map);

        mGoogleFioriMapView = findViewById(R.id.googleFioriMap);
        mGoogleFioriMapView.onCreate(savedInstanceState);
        // Handle the editor's save event.
// This is where you can implement functions to save the new annotated points, polylines and polygons
        mGoogleFioriMapView.getEditorView().setOnSaveListener(annotation -> {
            String message = null;
            if (annotation instanceof PointAnnotation) {
                message = "This is a point";
            } else if (annotation instanceof PolylineAnnotation) {
                message = "This is a polyline with " + annotation.getPoints().size() + " points";
            } else if (annotation instanceof PolygonAnnotation) {
                message = "This is a polygon with " + annotation.getPoints().size() + " points";
            }

            // import android.app AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(mGoogleFioriMapView.getContext(), com.sap.cloud.mobile.fiori.R.style.FioriAlertDialogStyle);
            builder.setMessage(message);
            builder.setPositiveButton("Save", (dialogInterface, i) -> {
                // Close the editor.
                mGoogleFioriMapView.setEditable(false);
                if (annotation instanceof PointAnnotation) {
                    mActionProvider.addCircle(
                            new FioriCircleOptions.Builder().center((FioriPoint) annotation.getPoints().get(0)).
                                    radius(40).
                                    strokeColor(getResources().getColor(R.color.maps_marker_color_5, null)).
                                    fillColor(getResources().getColor(R.color.maps_marker_color_6, null)).
                                    title("Editor Circle").
                                    build());
                } else if (annotation instanceof PolylineAnnotation) {
                    mActionProvider.addPolyline(
                            new FioriPolylineOptions.Builder().addAll(annotation.getPoints()).
                                    color(getResources().getColor(R.color.maps_marker_color_3, null)).
                                    strokeWidth(4).
                                    title("Editor Polyline").
                                    build());
                } else if (annotation instanceof PolygonAnnotation) {
                    mActionProvider.addPolygon(
                            new FioriPolygonOptions.Builder().addAll(annotation.getPoints()).
                                    strokeColor(getResources().getColor(R.color.maps_marker_color_3, null)).
                                    fillColor(getResources().getColor(R.color.maps_marker_color_4, null)).
                                    strokeWidth(4).
                                    title("Editor Polygon").
                                    build());
                }
            });
            builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        if (savedInstanceState != null) {
            mUseClustering = savedInstanceState.getBoolean("UseClustering", false);
            mMapType = savedInstanceState.getInt("MapType", GoogleMap.MAP_TYPE_NORMAL);
        }
        mGoogleFioriMapView.setOnMapCreatedListener(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Toronto, Canada.
     */
    @Override
    public void onMapCreated() {
        mActionProvider = new GoogleMapActionProvider(mGoogleFioriMapView, this);
        setupInfoProvider();
        setListAdapter();
        // For demo purposes, speed up the lookup of address details.
        // Will use Geocoder to translate an address to a LatLng if address is not in this list
        locations.put("Wilmington, Delaware, US", new LatLng(39.744655, -75.5483909));
        locations.put("Antioch, Illinois, US", new LatLng(42.4772418, -88.0956396));
        locations.put("Santa Clara, California, US", new LatLng(37.354107899999995, -121.9552356));
        locations.put("Hermosillo, MX", new LatLng(29.0729673, -110.9559192));
        locations.put("Bismarck, North Dakota, US", new LatLng(46.808326799999996, -100.7837392));
        locations.put("Ottawa, CA", new LatLng(45.4215296, -75.69719309999999));
        locations.put("México, MX", new LatLng(23.634501, -102.55278399999999));
        locations.put("Boca Raton, Florida, US", new LatLng(26.368306399999998, -80.1289321));
        locations.put("Carrollton, Texas, US", new LatLng(32.9756415, -96.8899636));
        locations.put("Lombard, Illinois, US", new LatLng(41.8800296, -88.00784349999999));
        locations.put("Moorestown, US", new LatLng(39.9688817, -74.948886));
        addCustomersToMap();

        // Setup toolbar buttons and add to the view.
        SettingsButton settingsButton = new SettingsButton(mGoogleFioriMapView.getToolbar().getContext());
        LegendButton legendButton = new LegendButton(mGoogleFioriMapView.getToolbar().getContext());
        LocationButton locationButton = new LocationButton(mGoogleFioriMapView.getToolbar().getContext());
        ZoomExtentButton extentButton = new ZoomExtentButton(mGoogleFioriMapView.getToolbar().getContext());
        ImageButton[] buttons = {settingsButton, legendButton, locationButton, extentButton};
        mGoogleFioriMapView.getToolbar().addButtons(Arrays.asList(buttons));

        // Setup draggable bottom panel
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View detailView = inflater.inflate(R.layout.detail_panel, null);
//        mGoogleFioriMapView.setDefaultPanelContent(detailView);

        View settingsView = inflater.inflate(R.layout.settings_panel, null);

// Setup selection of a different map type
        ChoiceFormCell mapTypeChoice = settingsView.findViewById(R.id.map_type);
        mapTypeChoice.setValueOptions(new String[]{"Normal", "Satellite", "Terrain", "Hybrid"});
        mapTypeChoice.setCellValueChangeListener(new FormCell.CellValueChangeListener<Integer>() {
            @Override
            public void cellChangeHandler(Integer value) {
                switch(value) {
                    case 0:
                        mMapType = GoogleMap.MAP_TYPE_NORMAL;
                        break;
                    case 1:
                        mMapType = GoogleMap.MAP_TYPE_SATELLITE;
                        break;
                    case 2:
                        mMapType = GoogleMap.MAP_TYPE_TERRAIN;
                        break;
                    case 3:
                        mMapType = GoogleMap.MAP_TYPE_HYBRID;
                        break;
                }
                mGoogleFioriMapView.getMap().setMapType(mMapType);
            }
        });

        if (mMapType == 0) {
            mapTypeChoice.setValue(mMapType);
        } else {
            mapTypeChoice.setValue(mMapType - 1);
        }

        mGoogleFioriMapView.setSettingsView(settingsView);

        if (mMapType != GoogleMap.MAP_TYPE_NONE) {
            mGoogleFioriMapView.getMap().setMapType(mMapType);
        }

// Setup clustering selection.
        SwitchFormCell useClusteringSwitch = settingsView.findViewById(R.id.use_clustering);
        useClusteringSwitch.setValue(mUseClustering);
        mActionProvider.setClustering(mUseClustering);
        useClusteringSwitch.setCellValueChangeListener(new FormCell.CellValueChangeListener<Boolean>() {
            @Override
            protected void cellChangeHandler(@NonNull Boolean value) {
                mUseClustering = value;
                mActionProvider.setClustering(mUseClustering);
            }
        });

        LatLng currentPosition = ((GoogleMapViewModel)mActionProvider.getMapViewModel()).getLatLng();
        float currentZoom = ((GoogleMapViewModel)mActionProvider.getMapViewModel()).getZoom();
        if (currentPosition != null && currentZoom != 0) {
            // Position the camera after a lifecycle event.
            mGoogleFioriMapView.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, currentZoom));
        } else {
            // Move the camera to the centre of North America
            LatLng centre = new LatLng(39.8283, -98.5795);
            mGoogleFioriMapView.getMap().animateCamera(CameraUpdateFactory.newLatLng(centre));
        }

        FioriMapSearchView mFioriMapSearchView = findViewById(R.id.fiori_map_search_view);
        if (mFioriMapSearchView != null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            mFioriMapSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mFioriMapSearchView.setAdapter(new ArrayAdapter<String>(CustomersFioriMapActivity.this, R.layout.search_auto_complete, R.id.search_auto_complete_text, addresses));
            mFioriMapSearchView.setThreshold(2);
            mFioriMapSearchView.setOnItemClickListener((parent, view, position, id) -> {
                mFioriMapSearchView.setQuery(parent.getItemAtPosition(position).toString(), false);
                searchResultSelected((String) parent.getItemAtPosition(position));
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mFioriMapSearchView.getWindowToken(), 0);
            });
        }
    }

    private void searchResultSelected(String selectedSearchResult) {
        LatLng latLng = locations.get(selectedSearchResult);
        if (latLng != null) {
            // Center the marker.
            mGoogleFioriMapView.getMap().moveCamera(CameraUpdateFactory.newLatLng(latLng));
            // Select the marker (or cluster the marker is in).
            mActionProvider.selectMarker(markers.get(selectedSearchResult));
        }
    }

    // Methods overriding the lifecycle events are required for FioriMapView to run properly
    @Override
    public void onStart() {
        super.onStart();
        mGoogleFioriMapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleFioriMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleFioriMapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleFioriMapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleFioriMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        mGoogleFioriMapView.onSaveInstanceState(bundle);

        bundle.putBoolean("UseClustering", mUseClustering);
        bundle.putInt("MapType", mMapType);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mGoogleFioriMapView.onLowMemory();
    }

    private LatLng getCustomerLatLongFromAddress(String address) {
        // import android.location.Address;
        List<Address> addresses;
        LatLng latLng = locations.get(address);
        if (latLng != null) {
            return latLng;
        }

        // String strAddress = "Wilmington, Delaware";
        Geocoder coder = new Geocoder(this);

        try {
            // May throw an IOException
            addresses = coder.getFromLocationName(address, 5);
            if (addresses == null || addresses.size() == 0) {
                return null;
            }

            Address location = addresses.get(0);
            latLng = new LatLng(location.getLatitude(), location.getLongitude());
            return latLng;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void addCustomerMarkerToMap(Customer customer) {
        FioriMarkerOptions customerMarker;
        String country = customer.getCountry();
        LatLng latLng = getCustomerLatLongFromAddress(customer.getCity() + ", " + customer.getCountry());
        if (latLng != null) {
            int color = (Color.parseColor("#E9573E")); // US
            if (country.equals("MX")) {
                color = (Color.parseColor("#FFA02B"));
            } else if (country.equals("CA")){
                color =  Color.parseColor("#2E4A62");
            }
            customerMarker = new FioriMarkerOptions.Builder()
                    .tag(customer)
                    .point(new FioriPoint(latLng.latitude, latLng.longitude))
                    .title(customer.getFirstName() + " " + customer.getLastName())
                    .legendTitle(customer.getCountry())
                    .color(color)
                    .build();
            mActionProvider.addMarker(customerMarker);
            markers.put(customer.getCity() + ", " + customer.getCountry(), customerMarker);
            mGoogleFioriMapView.getMap().moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    private void addCustomersToMap() {
        DataQuery query = new DataQuery()
                .from(ESPMContainerMetadata.EntitySets.customers)
                .where(Customer.country.equal("US")
                        .or(Customer.country.equal("CA"))
                        .or(Customer.country.equal("MX")));
        SAPServiceManager sapServiceManager = ((SAPWizardApplication) getApplication()).getSAPServiceManager();
        ESPMContainer espmContainer = sapServiceManager.getESPMContainer();
        espmContainer.getCustomersAsync(query, (List<Customer> customers) -> {
            for (Customer customer : customers) {
                addCustomerMarkerToMap(customer);
                addresses.add(customer.getCity() + ", " + customer.getCountry());
            }
            mActionProvider.doExtentsAction();
        }, (RuntimeException re) -> Log.d("", "An error occurred during async query:  " + re.getMessage()));
    }
    private void setupInfoProvider() {
        AnnotationInfoAdapter infoAdapter = new AnnotationInfoAdapter() {
            @Override
            public Object getInfo(Object tag) {
                return tag;
            }

            @Override
            public void onBindView(MapPreviewPanel mapPreviewPanel, Object info) {
                Customer customer = (Customer) info;
                mapPreviewPanel.setTitle(customer.getFirstName() + " " + customer.getLastName());
                ObjectHeader objectHeader = mapPreviewPanel.getObjectHeader();
                objectHeader.setHeadline(customer.getCity() + ", " + customer.getCountry());
                LatLng customerLatLng = getCustomerLatLongFromAddress(customer.getCity() + ", " + customer.getCountry());
                objectHeader.setBody("Latitude: " + customerLatLng.latitude);
                objectHeader.setFootnote("Longitude " + customerLatLng.longitude);
                ActionCell cell = new ActionCell(CustomersFioriMapActivity.this);
                cell.setText(customer.getPhoneNumber());
                cell.setIcon(R.drawable.ic_phone_black_24dp);
                ActionCell cell2 = new ActionCell(CustomersFioriMapActivity.this);
                cell2.setText(customer.getEmailAddress());
                cell2.setIcon(R.drawable.ic_email_black_24dp);
                ActionCell cell3 = new ActionCell(CustomersFioriMapActivity.this);
                cell3.setText(customer.getHouseNumber() + " " + customer.getStreet());
                cell3.setIcon(R.drawable.ic_map_marker_unselected);
                ActionCell cell4 = new ActionCell(CustomersFioriMapActivity.this);
                cell4.setText("Additional Details");
                cell4.setIcon(R.drawable.ic_list_24dp);
                cell4.setOnClickListener(v -> {
                            Intent intent = new Intent(CustomersFioriMapActivity.this, CustomersActivity.class);
                            intent.putExtra(BundleKeys.ENTITY_INSTANCE, customer);
                            startActivity(intent);
                        }
                );
                mapPreviewPanel.setActionCells(cell, cell2, cell3, cell4);
            }
        };
        mActionProvider.setAnnotationInfoAdapter(infoAdapter);
    }

    private void setListAdapter() {
        if (mMapListPanel == null) {
            mMapListPanel = mGoogleFioriMapView.getMapListPanel();
            mMapResultsAdapter = new MapResultsAdapter();
            mMapListPanel.setAdapter(mMapResultsAdapter);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ObjectCell objectCell;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            if (itemView instanceof ObjectCell) {
                objectCell = (ObjectCell) itemView;
            }
        }
    }

    class MapResultsAdapter extends RecyclerView.Adapter<ViewHolder> implements MapListPanel.MapListAdapter {
        List<Customer> customers = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ObjectCell cell = new ObjectCell(parent.getContext());
            cell.setPreserveIconStackSpacing(true);
            cell.setPreserveDetailImageSpacing(false);
            ViewHolder viewHolder = new ViewHolder(cell);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Customer customer = customers.get(position);
            ObjectCell resultCell = holder.objectCell;
            resultCell.setHeadline(customer.getFirstName() + " " + customer.getLastName());
            resultCell.setSubheadline(customer.getHouseNumber() + " " + customer.getStreet() + ", " + customer.getCity());
            resultCell.setStatus(customer.getCountry(), 1);
            resultCell.setOnClickListener(v -> {
                Intent intent = new Intent(CustomersFioriMapActivity.this, CustomersActivity.class);
                intent.putExtra(BundleKeys.ENTITY_INSTANCE, customer);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return customers.size();
        }

        @Override
        public void clusterSelected(@Nullable List<FioriMarkerOptions> list) {
            customers.clear();
            for (FioriMarkerOptions fmo : list) {
                Object tag = fmo.tag;
                if (tag != null) {
                    customers.add((Customer) tag);
                }
            }
        }
    }
}
