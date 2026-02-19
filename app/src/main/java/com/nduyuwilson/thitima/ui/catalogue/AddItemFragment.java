package com.nduyuwilson.thitima.ui.catalogue;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;

public class AddItemFragment extends Fragment {

    private TextInputEditText editTextName, editTextDescription, editTextBuyingPrice, editTextSellingPrice;
    private ImageView imageViewPreview;
    private ItemViewModel itemViewModel;
    private String selectedImageUri = "";
    private int itemId = -1;
    private Item existingItem;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri.toString();
                    imageViewPreview.setImageURI(uri);
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getInt("itemId", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);

        editTextName = view.findViewById(R.id.editTextItemName);
        editTextDescription = view.findViewById(R.id.editTextItemDescription);
        editTextBuyingPrice = view.findViewById(R.id.editTextBuyingPrice);
        editTextSellingPrice = view.findViewById(R.id.editTextSellingPrice);
        imageViewPreview = view.findViewById(R.id.imageViewItemPreview);

        view.findViewById(R.id.fabSelectImage).setOnClickListener(v -> mGetContent.launch("image/*"));

        Button buttonSave = view.findViewById(R.id.buttonSaveItem);
        TextView textViewHeader = view.findViewById(R.id.textViewHeader);

        if (itemId != -1) {
            textViewHeader.setText("Edit Component");
            buttonSave.setText("Update Component");
            itemViewModel.getItemById(itemId).observe(getViewLifecycleOwner(), item -> {
                if (item != null) {
                    existingItem = item;
                    populateFields(item);
                }
            });
        }

        buttonSave.setOnClickListener(v -> saveItem(v));
    }

    private void populateFields(Item item) {
        editTextName.setText(item.getName());
        editTextDescription.setText(item.getDescription());
        editTextBuyingPrice.setText(String.valueOf(item.getBuyingPrice()));
        editTextSellingPrice.setText(String.valueOf(item.getSellingPrice()));
        selectedImageUri = item.getImageUri();
        Glide.with(this).load(selectedImageUri).placeholder(R.drawable.ic_splash_logo).into(imageViewPreview);
    }

    private void saveItem(View view) {
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String buyingPriceStr = editTextBuyingPrice.getText().toString().trim();
        String sellingPriceStr = editTextSellingPrice.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(buyingPriceStr) || TextUtils.isEmpty(sellingPriceStr)) {
            Snackbar.make(view, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show();
            return;
        }

        double buyingPrice = Double.parseDouble(buyingPriceStr);
        double sellingPrice = Double.parseDouble(sellingPriceStr);

        if (itemId != -1 && existingItem != null) {
            existingItem.setName(name);
            existingItem.setDescription(description);
            existingItem.setBuyingPrice(buyingPrice);
            existingItem.setSellingPrice(sellingPrice);
            existingItem.setImageUri(selectedImageUri);
            itemViewModel.update(existingItem);
            Snackbar.make(view, "Item updated", Snackbar.LENGTH_SHORT).show();
        } else {
            Item item = new Item(name, description, buyingPrice, sellingPrice, selectedImageUri);
            itemViewModel.insert(item);
            Snackbar.make(view, "Item added to catalogue", Snackbar.LENGTH_SHORT).show();
        }

        Navigation.findNavController(view).navigateUp();
    }
}
