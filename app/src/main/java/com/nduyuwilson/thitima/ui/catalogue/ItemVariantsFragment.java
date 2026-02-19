package com.nduyuwilson.thitima.ui.catalogue;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;

public class ItemVariantsFragment extends Fragment {

    private int itemId;
    private ItemViewModel itemViewModel;
    private ItemVariantAdapter adapter;
    private String selectedImageUri = "";
    private ImageView dialogImageView;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && dialogImageView != null) {
                    selectedImageUri = uri.toString();
                    dialogImageView.setImageURI(uri);
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getInt("itemId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_item_variants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        
        TextView textViewName = view.findViewById(R.id.textViewItemName);
        itemViewModel.getItemById(itemId).observe(getViewLifecycleOwner(), item -> {
            if (item != null) {
                textViewName.setText("Variants for: " + item.getName());
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewVariants);
        // Set GridLayoutManager with 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        
        adapter = new ItemVariantAdapter(variant -> {
            // Edit variant logic
        });
        recyclerView.setAdapter(adapter);

        itemViewModel.getVariantsForItem(itemId).observe(getViewLifecycleOwner(), variants -> {
            adapter.submitList(variants);
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddVariant);
        fab.setOnClickListener(v -> showAddVariantDialog());
    }

    private void showAddVariantDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Add Brand Variant");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editBrand = new EditText(requireContext());
        editBrand.setHint("Brand Name (e.g. Schneider)");
        layout.addView(editBrand);

        final EditText editBuyingPrice = new EditText(requireContext());
        editBuyingPrice.setHint("Buying Price");
        editBuyingPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(editBuyingPrice);

        final EditText editSellingPrice = new EditText(requireContext());
        editSellingPrice.setHint("Selling Price");
        editSellingPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(editSellingPrice);

        dialogImageView = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
        params.topMargin = 24;
        params.gravity = android.view.Gravity.CENTER;
        dialogImageView.setLayoutParams(params);
        dialogImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        dialogImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        dialogImageView.setOnClickListener(v -> mGetContent.launch("image/*"));
        layout.addView(dialogImageView);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String brand = editBrand.getText().toString().trim();
            String buyingStr = editBuyingPrice.getText().toString().trim();
            String sellingStr = editSellingPrice.getText().toString().trim();

            if (!TextUtils.isEmpty(brand) && !TextUtils.isEmpty(sellingStr)) {
                try {
                    double buying = buyingStr.isEmpty() ? 0 : Double.parseDouble(buyingStr);
                    double selling = Double.parseDouble(sellingStr);
                    
                    ItemVariant variant = new ItemVariant(itemId, brand, buying, selling, selectedImageUri);
                    itemViewModel.insertVariant(variant);
                    selectedImageUri = ""; // Reset
                } catch (NumberFormatException e) {
                    Snackbar.make(requireView(), "Invalid price format", Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(requireView(), "Brand and Selling Price are required", Snackbar.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            selectedImageUri = "";
            dialog.cancel();
        });

        builder.show();
    }
}
