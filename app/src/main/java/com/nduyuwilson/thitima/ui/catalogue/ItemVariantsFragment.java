package com.nduyuwilson.thitima.ui.catalogue;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.util.ImageUtils;
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
                    // Save to internal storage immediately
                    String internalPath = ImageUtils.saveImageToInternalStorage(requireContext(), uri);
                    if (!internalPath.isEmpty()) {
                        selectedImageUri = internalPath;
                        dialogImageView.setImageURI(Uri.parse(internalPath));
                    }
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
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        
        adapter = new ItemVariantAdapter(this::showEditDeleteVariantDialog);
        recyclerView.setAdapter(adapter);

        itemViewModel.getVariantsForItem(itemId).observe(getViewLifecycleOwner(), variants -> {
            adapter.submitList(variants);
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddVariant);
        fab.setOnClickListener(v -> showAddEditVariantDialog(null));

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.item_variants_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_edit_item) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("itemId", itemId);
                    Navigation.findNavController(view).navigate(R.id.action_itemVariantsFragment_to_addItemFragment, bundle);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_delete_item) {
                    confirmDeleteItem();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void confirmDeleteItem() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Component")
                .setMessage("Are you sure you want to delete this entire component and all its variants?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    itemViewModel.getItemById(itemId).observe(getViewLifecycleOwner(), item -> {
                        if (item != null) {
                            itemViewModel.delete(item);
                            Toast.makeText(requireContext(), "Component deleted", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).navigateUp();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDeleteVariantDialog(ItemVariant variant) {
        String[] options = {"Edit Brand", "Delete Brand"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(variant.getBrandName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddEditVariantDialog(variant);
                    } else {
                        confirmDeleteVariant(variant);
                    }
                })
                .show();
    }

    private void confirmDeleteVariant(ItemVariant variant) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Variant")
                .setMessage("Are you sure you want to delete " + variant.getBrandName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    itemViewModel.deleteVariant(variant);
                    Snackbar.make(requireView(), "Variant deleted", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddEditVariantDialog(@Nullable ItemVariant existingVariant) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(existingVariant == null ? "Add Brand Variant" : "Edit Brand Variant");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editBrand = new EditText(requireContext());
        editBrand.setHint("Brand Name");
        if (existingVariant != null) editBrand.setText(existingVariant.getBrandName());
        layout.addView(editBrand);

        final EditText editBuyingPrice = new EditText(requireContext());
        editBuyingPrice.setHint("Buying Price");
        editBuyingPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existingVariant != null) editBuyingPrice.setText(String.valueOf(existingVariant.getBuyingPrice()));
        layout.addView(editBuyingPrice);

        final EditText editSellingPrice = new EditText(requireContext());
        editSellingPrice.setHint("Selling Price");
        editSellingPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existingVariant != null) editSellingPrice.setText(String.valueOf(existingVariant.getSellingPrice()));
        layout.addView(editSellingPrice);

        dialogImageView = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
        params.topMargin = 24;
        params.gravity = android.view.Gravity.CENTER;
        dialogImageView.setLayoutParams(params);
        dialogImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        dialogImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        if (existingVariant != null && !TextUtils.isEmpty(existingVariant.getImageUri())) {
            selectedImageUri = existingVariant.getImageUri();
            dialogImageView.setImageURI(Uri.parse(selectedImageUri));
        }

        dialogImageView.setOnClickListener(v -> mGetContent.launch("image/*"));
        layout.addView(dialogImageView);

        builder.setView(layout);

        builder.setPositiveButton(existingVariant == null ? "Add" : "Update", (dialog, which) -> {
            String brand = editBrand.getText().toString().trim();
            String buyingStr = editBuyingPrice.getText().toString().trim();
            String sellingStr = editSellingPrice.getText().toString().trim();

            if (!TextUtils.isEmpty(brand) && !TextUtils.isEmpty(sellingStr)) {
                try {
                    double buying = buyingStr.isEmpty() ? 0 : Double.parseDouble(buyingStr);
                    double selling = Double.parseDouble(sellingStr);
                    
                    if (existingVariant == null) {
                        ItemVariant variant = new ItemVariant(itemId, brand, buying, selling, selectedImageUri);
                        itemViewModel.insertVariant(variant);
                    } else {
                        existingVariant.setBrandName(brand);
                        existingVariant.setBuyingPrice(buying);
                        existingVariant.setSellingPrice(selling);
                        existingVariant.setImageUri(selectedImageUri);
                        itemViewModel.updateVariant(existingVariant);
                    }
                    selectedImageUri = ""; 
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
