package com.nduyuwilson.thitima.auth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.nduyuwilson.thitima.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminTenantsAdapter extends RecyclerView.Adapter<AdminTenantsAdapter.TenantViewHolder> {

    public interface TenantActionListener {
        void onToggleSubscription(TenantModel tenant);
        void onResetDeviceLock(TenantModel tenant);
        void onTenantClick(TenantModel tenant);
    }

    private List<TenantModel> tenantList = new ArrayList<>();
    private final TenantActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public AdminTenantsAdapter(TenantActionListener listener) {
        this.listener = listener;
    }

    public void setTenants(List<TenantModel> tenants) {
        this.tenantList = new ArrayList<>(tenants);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TenantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_tenant, parent, false);
        return new TenantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TenantViewHolder holder, int position) {
        TenantModel tenant = tenantList.get(position);
        Context context = holder.itemView.getContext();

        String email = tenant.getEmail() != null ? tenant.getEmail() : "No Email";
        holder.tvTenantEmail.setText(email);
        holder.tvTenantUid.setText("UID: " + (tenant.getUid() != null ? tenant.getUid() : "N/A"));

        // Avatar letter
        String initial = !email.isEmpty() ? email.substring(0, 1).toUpperCase(Locale.getDefault()) : "T";
        holder.tvTenantAvatar.setText(initial);

        // Plan Chip
        if (tenant.isPremium()) {
            holder.chipPlanStatus.setText("PRO SAAS");
            holder.chipPlanStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark);
            holder.chipPlanStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            holder.btnToggleSubscription.setText("Suspend");
            holder.btnToggleSubscription.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        } else {
            holder.chipPlanStatus.setText("TRIAL / FIELD");
            holder.chipPlanStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
            holder.chipPlanStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            holder.btnToggleSubscription.setText("Activate PRO");
            holder.btnToggleSubscription.setBackgroundColor(ContextCompat.getColor(context, R.color.primary));
        }

        // Hardware Device Lock
        String deviceId = tenant.getDeviceId();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            String masked = deviceId.length() > 8 ? deviceId.substring(0, 8) + "..." : deviceId;
            holder.tvDeviceLockStatus.setText("Hardware Lock: " + masked + " (Active)");
            holder.btnResetDeviceLock.setEnabled(true);
            holder.btnResetDeviceLock.setAlpha(1.0f);
        } else {
            holder.tvDeviceLockStatus.setText("Hardware Lock: None (Ready to pair)");
            holder.btnResetDeviceLock.setEnabled(false);
            holder.btnResetDeviceLock.setAlpha(0.5f);
        }

        // Created Date
        if (tenant.getCreatedAt() != null) {
            holder.tvRegisteredDate.setText("Joined " + dateFormat.format(tenant.getCreatedAt().toDate()));
        } else {
            holder.tvRegisteredDate.setText("Active Tenant");
        }

        // Click listeners
        holder.btnToggleSubscription.setOnClickListener(v -> {
            if (listener != null) listener.onToggleSubscription(tenant);
        });

        holder.btnResetDeviceLock.setOnClickListener(v -> {
            if (listener != null) listener.onResetDeviceLock(tenant);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTenantClick(tenant);
        });
    }

    @Override
    public int getItemCount() {
        return tenantList.size();
    }

    static class TenantViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenantAvatar, tvTenantEmail, tvTenantUid, tvDeviceLockStatus, tvRegisteredDate;
        Chip chipPlanStatus;
        MaterialButton btnResetDeviceLock, btnToggleSubscription;

        public TenantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTenantAvatar = itemView.findViewById(R.id.tvTenantAvatar);
            tvTenantEmail = itemView.findViewById(R.id.tvTenantEmail);
            tvTenantUid = itemView.findViewById(R.id.tvTenantUid);
            tvDeviceLockStatus = itemView.findViewById(R.id.tvDeviceLockStatus);
            tvRegisteredDate = itemView.findViewById(R.id.tvRegisteredDate);
            chipPlanStatus = itemView.findViewById(R.id.chipPlanStatus);
            btnResetDeviceLock = itemView.findViewById(R.id.btnResetDeviceLock);
            btnToggleSubscription = itemView.findViewById(R.id.btnToggleSubscription);
        }
    }
}