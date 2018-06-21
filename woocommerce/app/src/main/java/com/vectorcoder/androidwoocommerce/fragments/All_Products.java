package com.vectorcoder.androidwoocommerce.fragments;


import android.support.annotation.Nullable;
import android.os.Bundle;
import android.os.AsyncTask;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ToggleButton;
import android.widget.LinearLayout;
import android.widget.CompoundButton;

import com.vectorcoder.androidwoocommerce.R;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

import com.vectorcoder.androidwoocommerce.app.App;
import com.vectorcoder.androidwoocommerce.constant.ConstantValues;
import com.vectorcoder.androidwoocommerce.customs.FilterDialog;
import com.vectorcoder.androidwoocommerce.models.api_response_model.ErrorResponse;
import com.vectorcoder.androidwoocommerce.models.product_model.Filters;
import com.vectorcoder.androidwoocommerce.network.APIClient;
import com.vectorcoder.androidwoocommerce.adapters.ProductAdapter;
import com.vectorcoder.androidwoocommerce.customs.EndlessRecyclerViewScroll;
import com.vectorcoder.androidwoocommerce.models.product_model.ProductDetails;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;


public class All_Products extends Fragment {

    View rootView;
    
    int pageNo = 1;
    boolean isGridView;
    boolean isFilterApplied;
    boolean isSaleApplied = false;
    boolean isFeaturedApplied = false;
    
    String customerID;
    String order = "desc";
    String sortBy = "date";

    LinearLayout bottomBar;
    LinearLayout sortList;
    TextView emptyRecord;
    TextView sortListText;
    ProgressBar progressBar;
    ProgressBar loadingProgress;
    ImageButton removeFilterBtn;
    ToggleButton filterButton;
    ToggleButton toggleLayoutView;
    RecyclerView all_products_recycler;
    
    Filters filters = null;
    LoadMoreTask loadMoreTask;
    FilterDialog filterDialog;
    
    ProductAdapter productAdapter;
    List<ProductDetails> productsList;
    
    GridLayoutManager gridLayoutManager;
    LinearLayoutManager linearLayoutManager;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.f_products_vertical, container, false);
    
        if (getArguments() != null) {
            if (getArguments().containsKey("sortBy")) {
                sortBy = getArguments().getString("sortBy", "date");
            }
            if (getArguments().containsKey("on_sale")) {
                isSaleApplied = getArguments().getBoolean("on_sale", false);
            }
            if (getArguments().containsKey("featured")) {
                isFeaturedApplied = getArguments().getBoolean("featured", false);
            }
        }
        

        // Get the Customer's ID from SharedPreferences
        customerID = getActivity().getSharedPreferences("UserInfo", getContext().MODE_PRIVATE).getString("userID", "");


        // Binding Layout Views
        bottomBar = (LinearLayout) rootView.findViewById(R.id.bottomBar);
        sortList = (LinearLayout) rootView.findViewById(R.id.sort_list);
        sortListText = (TextView) rootView.findViewById(R.id.sort_text);
        emptyRecord = (TextView) rootView.findViewById(R.id.empty_record);
        progressBar = (ProgressBar) rootView.findViewById(R.id.loading_bar);
        loadingProgress = (ProgressBar) rootView.findViewById(R.id.loading_progress);
        removeFilterBtn = (ImageButton) rootView.findViewById(R.id.removeFilterBtn);
        filterButton = (ToggleButton) rootView.findViewById(R.id.filterBtn);
        toggleLayoutView = (ToggleButton) rootView.findViewById(R.id.layout_toggleBtn);
        all_products_recycler = (RecyclerView) rootView.findViewById(R.id.products_recycler);


        // Hide some of the Views
        emptyRecord.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.GONE);
    
    
        sortListText.setText(getString(R.string.Newest));
    
        isGridView = true;
        isFilterApplied = (isSaleApplied || isFeaturedApplied);
    
        toggleLayoutView.setChecked(isGridView);
        filterButton.setChecked(isFilterApplied);
        removeFilterBtn.setVisibility(isFilterApplied? View.VISIBLE : View.GONE);
        
        productsList = new ArrayList<>();
    
        
        if (isFilterApplied) {
            filters = new Filters();
            filters.setSale(isSaleApplied);
            filters.setFeature(isFeaturedApplied);
            filters.setMin_price("0");
            filters.setMax_price(ConstantValues.FILTER_MAX_PRICE);
            loadingProgress.setVisibility(View.VISIBLE);
        
            RequestFilteredProducts(pageNo, filters);
        }
        else {
            filters = new Filters();
            loadingProgress.setVisibility(View.VISIBLE);
            
            // Request for Products of given Category based on PageNo.
            RequestAllProducts(pageNo);
        }


        // Initialize GridLayoutManager and LinearLayoutManager
        gridLayoutManager = new GridLayoutManager(getContext(), 2);
        linearLayoutManager = new LinearLayoutManager(getContext());


        // Initialize the ProductAdapter for RecyclerView
        productAdapter = new ProductAdapter(getContext(), productsList, false);
        
        setRecyclerViewLayoutManager(isGridView);
        all_products_recycler.setAdapter(productAdapter);
    
    
        // Handle the Scroll event of Product's RecyclerView
        all_products_recycler.addOnScrollListener(new EndlessRecyclerViewScroll(bottomBar) {
            @Override
            public void onLoadMore(final int current_page) {
            
                progressBar.setVisibility(View.VISIBLE);
            
                if (isFilterApplied) {
                    // Initialize LoadMoreTask to Load More Products from Server against some Filters
                    loadMoreTask = new LoadMoreTask(current_page, filters);
                } else {
                    // Initialize LoadMoreTask to Load More Products from Server without Filters
                    loadMoreTask = new LoadMoreTask(current_page, null);
                }
            
                // Execute AsyncTask LoadMoreTask to Load More Products from Server
                loadMoreTask.execute();
            }
        });

        productAdapter.notifyDataSetChanged();
    
    
        // Toggle RecyclerView's LayoutManager
        toggleLayoutView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isGridView = isChecked;
                setRecyclerViewLayoutManager(isGridView);
            }
        });
    
    
        // Initialize FilterDialog and Override its abstract methods
        filterDialog = new FilterDialog(getContext(), filters) {
            @Override
            public void clearFilters() {
                // Clear Filters
                isFilterApplied = false;
                filterButton.setChecked(false);
                removeFilterBtn.setVisibility(View.GONE);
                filters = null;
                productsList.clear();
                productAdapter.notifyDataSetChanged();
                loadingProgress.setVisibility(View.VISIBLE);
                new LoadMoreTask(pageNo, filters).execute();
            }
        
            @Override
            public void applyFilters(Filters postFilterData) {
                // Apply Filters
                isFilterApplied = true;
                filterButton.setChecked(true);
                removeFilterBtn.setVisibility(View.VISIBLE);
                filters = postFilterData;
                productsList.clear();
                productAdapter.notifyDataSetChanged();
                loadingProgress.setVisibility(View.VISIBLE);
                new LoadMoreTask(pageNo, filters).execute();
            }
        };
    
    
        // Handle the Click event of Filter Button
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            
                if (isFilterApplied) {
                    filterButton.setChecked(true);
                    filterDialog.show();
                
                } else {
                    filterButton.setChecked(false);
                    filterDialog = new FilterDialog(getContext(), filters) {
                        @Override
                        public void clearFilters() {
                            isFilterApplied = false;
                            filterButton.setChecked(false);
                            removeFilterBtn.setVisibility(View.GONE);
                            filters = null;
                            productsList.clear();
                            productAdapter.notifyDataSetChanged();
                            loadingProgress.setVisibility(View.VISIBLE);
                            new LoadMoreTask(pageNo, filters).execute();
                        }
                    
                        @Override
                        public void applyFilters(Filters appliedFilters) {
                            isFilterApplied = true;
                            filterButton.setChecked(true);
                            removeFilterBtn.setVisibility(View.VISIBLE);
                            filters = appliedFilters;
                            productsList.clear();
                            productAdapter.notifyDataSetChanged();
                            loadingProgress.setVisibility(View.VISIBLE);
                            new LoadMoreTask(pageNo, filters).execute();
                        }
                    };
                    filterDialog.show();
                }
            }
        });
        

        sortList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Get sortBy_array from String_Resources
                final String[] sortArray = getResources().getStringArray(R.array.sortBy_array);

                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                dialog.setCancelable(true);
                
                dialog.setItems(sortArray, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        
                        String selectedText = sortArray[which];
                        sortListText.setText(selectedText);
    
    
                        if (selectedText.equalsIgnoreCase(sortArray[0])) {
                            order = "asc";
                            sortBy = "title";
                        }
                        else if (selectedText.equalsIgnoreCase(sortArray[1])) {
                            order = "desc";
                            sortBy = "title";
                        }
                        else if (selectedText.equalsIgnoreCase(sortArray[2])) {
                            order = "desc";
                            sortBy = "date";
                        }
                        else {
                            order = "desc";
                            sortBy = "date";
                        }
    
    
                        productsList.clear();
                        productAdapter.notifyDataSetChanged();
                        loadingProgress.setVisibility(View.VISIBLE);
                        
                        if(isFilterApplied){
                            // Initialize LoadMoreTask to Load More Products from Server against some Filters
                            RequestFilteredProducts(pageNo, filters);
                        }
                        else {
                            // Initialize LoadMoreTask to Load More Products from Server without Filters
                            RequestAllProducts(pageNo);
                        }
                        dialog.dismiss();


                        // Handle the Scroll event of Product's RecyclerView
                        all_products_recycler.addOnScrollListener(new EndlessRecyclerViewScroll(bottomBar) {
                            @Override
                            public void onLoadMore(int current_page) {
                                
                                progressBar.setVisibility(View.VISIBLE);
    
                                // Execute AsyncTask LoadMoreTask to Load More Products from Server
                                loadMoreTask = new LoadMoreTask(current_page, filters);
                                loadMoreTask.execute();
                            }
                        });

                    }
                });
                dialog.show();
            }
        });
    
    
        removeFilterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFilterApplied = false;
                filterButton.setChecked(false);
                removeFilterBtn.setVisibility(View.GONE);
                filters = null;
                productsList.clear();
                productAdapter.notifyDataSetChanged();
                loadingProgress.setVisibility(View.VISIBLE);
                new LoadMoreTask(pageNo, filters).execute();
            }
        });

        
        return rootView;
        
    }

    

    //*********** Switch RecyclerView's LayoutManager ********//
    
    public void setRecyclerViewLayoutManager(Boolean isGridView) {
        int scrollPosition = 0;
        
        // If a LayoutManager has already been set, get current Scroll Position
        if (all_products_recycler.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) all_products_recycler.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }
        
        productAdapter.toggleLayout(isGridView);
        
        all_products_recycler.setLayoutManager(isGridView ? gridLayoutManager : linearLayoutManager);
        all_products_recycler.setAdapter(productAdapter);
        
        all_products_recycler.scrollToPosition(scrollPosition);
    }



    //*********** Adds Products returned from the Server to the ProductsList ********//

    private void addProducts(List<ProductDetails> products) {
    
        // Add Products to ProductsList from the List of ProductData
        if (products.size() > 0) {
            productsList.addAll(products);
    
            for (int i=0;  i<products.size();  i++) {
                if (products.get(i).getStatus() != null  &&  !"publish".equalsIgnoreCase(products.get(i).getStatus())) {
                    for (int j=0;  j<productsList.size();  j++) {
                        if (products.get(i).getId() == productsList.get(j).getId()) {
                            productsList.remove(j);
                        }
                    }
                }
            }
        }

        productAdapter.notifyDataSetChanged();


        // Change the Visibility of emptyRecord Text based on ProductList's Size
        if (productAdapter.getItemCount() == 0) {
            emptyRecord.setVisibility(View.VISIBLE);
        }
        else {
            emptyRecord.setVisibility(View.GONE);
        }
        
    }



    //*********** Request Products from the Server based on PageNo. ********//

    public void RequestAllProducts(final int pageNumber) {
    
        emptyRecord.setVisibility(View.GONE);
        
        Map<String, String> params = new LinkedHashMap<>();
        params.put("page", String.valueOf(pageNumber));
        params.put("order", order);
        params.put("orderby", sortBy);
        

        Call<List<ProductDetails>> call = APIClient.getInstance()
                .getAllProducts
                        (
                            params
                        );

        call.enqueue(new Callback<List<ProductDetails>>() {
            @Override
            public void onResponse(Call<List<ProductDetails>> call, retrofit2.Response<List<ProductDetails>> response) {
    
                // Hide the ProgressBar
                progressBar.setVisibility(View.GONE);
                loadingProgress.setVisibility(View.GONE);
                
                
                if (response.isSuccessful()) {
    
                    addProducts(response.body());

                }
                else {
                    Converter<ResponseBody, ErrorResponse> converter = APIClient.retrofit.responseBodyConverter(ErrorResponse.class, new Annotation[0]);
                    ErrorResponse error;
                    try {
                        error = converter.convert(response.errorBody());
                    } catch (IOException e) {
                        error = new ErrorResponse();
                    }
                        
                    Toast.makeText(App.getContext(), "Error : "+error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ProductDetails>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(App.getContext(), "NetworkCallFailure : "+t, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    
    
    //*********** Request Products from the Server based on PageNo. against some Filters ********//
    
    public void RequestFilteredProducts(final int pageNumber, Filters filters) {
    
        emptyRecord.setVisibility(View.GONE);
        
        Map<String, String> params = new LinkedHashMap<>();
        params.put("page", String.valueOf(pageNumber));
        params.put("order", order);
        params.put("orderby", sortBy);
    
        if (filters.getMin_price() != null  &&  !TextUtils.isEmpty(filters.getMin_price())  &&  !"0".equalsIgnoreCase(filters.getMin_price()))
            params.put("min_price", ""+filters.getMin_price());
    
        if (filters.getMax_price() != null  &&  !TextUtils.isEmpty(filters.getMax_price())  &&  !"0".equalsIgnoreCase(filters.getMax_price()))
            params.put("max_price", ""+filters.getMax_price());
    
        if (filters.getSale())
            params.put("on_sale", ""+filters.getSale());
    
        if (filters.getFeature())
            params.put("featured", ""+filters.getFeature());
        
        
        Call<List<ProductDetails>> call = APIClient.getInstance()
                .getAllProducts
                        (
                                params
                        );
        
        call.enqueue(new Callback<List<ProductDetails>>() {
            @Override
            public void onResponse(Call<List<ProductDetails>> call, retrofit2.Response<List<ProductDetails>> response) {
    
                // Hide the ProgressBar
                progressBar.setVisibility(View.GONE);
                loadingProgress.setVisibility(View.GONE);
                
                
                if (response.isSuccessful()) {
                    
                    addProducts(response.body());
                    
                }
                else {
                    Converter<ResponseBody, ErrorResponse> converter = APIClient.retrofit.responseBodyConverter(ErrorResponse.class, new Annotation[0]);
                    ErrorResponse error;
                    try {
                        error = converter.convert(response.errorBody());
                    } catch (IOException e) {
                        error = new ErrorResponse();
                    }
                    
                    Toast.makeText(App.getContext(), "Error : "+error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<ProductDetails>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(App.getContext(), "NetworkCallFailure : "+t, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    

    /*********** LoadMoreTask Used to Load more Products from the Server in the Background Thread using AsyncTask ********/

    private class LoadMoreTask extends AsyncTask<String, Void, String> {

        int page_number;
        Filters filters;


        private LoadMoreTask(int page_number, Filters filters) {
            this.page_number = page_number;
            this.filters = filters;
        }


        //*********** Runs on the UI thread before #doInBackground() ********//

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        //*********** Performs some Processes on Background Thread and Returns a specified Result  ********//

        @Override
        protected String doInBackground(String... params) {
            
            // Check if any of the Filter is applied
            if (isFilterApplied) {
                // Request for Products against specified Filters, based on PageNo.
                RequestFilteredProducts(page_number, filters);
            }
            else {
                // Request for Products based on PageNo.
                RequestAllProducts(page_number);
            }

            return "All Done!";
        }


        //*********** Runs on the UI thread after #doInBackground() ********//

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
}