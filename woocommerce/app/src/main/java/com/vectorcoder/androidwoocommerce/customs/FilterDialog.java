package com.vectorcoder.androidwoocommerce.customs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import com.vectorcoder.androidwoocommerce.R;

import com.vectorcoder.androidwoocommerce.constant.ConstantValues;
import com.vectorcoder.androidwoocommerce.models.product_model.Filters;


/**
 * FilterDialog will be used to implement Price and Attribute Filters on Products in different categories
 **/

public abstract class FilterDialog extends Dialog {
    
    private Filters filters;

    private CrystalRangeSeekbar filter_price_slider;
    private CheckBox sale_checkbox, featured_checkbox;
    private TextView filter_min_price, filter_max_price;
    private Button filter_cancel_btn, filter_clear_btn, filter_apply_btn;
    

    
    public FilterDialog(Context context, Filters filters) {
        super(context);
        this.filters = filters;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the Window Full Screen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setContentView(R.layout.filter_dialog);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    
        sale_checkbox = (CheckBox) findViewById(R.id.sale_checkbox);
        featured_checkbox = (CheckBox) findViewById(R.id.featured_checkbox);
        filter_min_price = (TextView) findViewById(R.id.filter_min_price);
        filter_max_price = (TextView) findViewById(R.id.filter_max_price);
        filter_cancel_btn = (Button) findViewById(R.id.filter_cancel_btn);
        filter_clear_btn = (Button) findViewById(R.id.filter_clear_btn);
        filter_apply_btn = (Button) findViewById(R.id.filter_apply_btn);
        filter_price_slider = (CrystalRangeSeekbar) findViewById(R.id.filter_price_slider);


        if (filters != null  &&  Long.parseLong(ConstantValues.FILTER_MAX_PRICE) > 0) {
            filter_max_price.setText(ConstantValues.FILTER_MAX_PRICE);
            filter_price_slider.setMaxValue(Float.parseFloat(ConstantValues.FILTER_MAX_PRICE));
        } else {
            filter_max_price.setText(String.valueOf(10000));
            filter_price_slider.setMaxValue(Float.parseFloat(String.valueOf(10000)));
        }
        
        
        if (filters != null) {
            if (filters.getSale() != null) {
                sale_checkbox.setChecked(filters.getSale());
            }
            if (filters.getFeature() != null) {
                featured_checkbox.setChecked(filters.getFeature());
            }
        }
        
        
        // Get the Price RangeBar Values
        filter_price_slider.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener() {
            @Override
            public void valueChanged(Number minValue, Number maxValue) {
                // Set the Minimum and Maximum Price Values
                filter_min_price.setText(String.valueOf(minValue));
                filter_max_price.setText(String.valueOf(maxValue));
            }
        });


        // Dismiss the FilterDialog
        filter_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });


        // Clear Selected Filters
        filter_clear_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear Filters
                clearFilters();
                dismiss();
            }
        });


        // Apply Selected Filters
        filter_apply_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                
                Filters appliedFilters = new Filters();
    
                appliedFilters.setSale(sale_checkbox.isChecked());
                appliedFilters.setFeature(featured_checkbox.isChecked());
                appliedFilters.setMin_price(filter_min_price.getText().toString());
                appliedFilters.setMax_price(filter_max_price.getText().toString());

                // Apply Filters
                applyFilters(appliedFilters);

                dismiss();
            }
        });
    }



    //*********** Apply Selected Filters on the Products of a Category ********//

    public abstract void applyFilters(Filters filters);



    //*********** Clear All Filters on the Products of a Category ********//

    public abstract void clearFilters();
    
}

