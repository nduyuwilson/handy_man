package com.nduyuwilson.thitima.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.data.model.PaymentMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfGenerator {

    public static File generateInvoice(Context context, Project project, List<ProjectItem> projectItems, List<LabourActivity> labourActivities, Map<Integer, Item> itemMap, Map<Integer, ItemVariant> variantMap) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint accentPaint = new Paint();
        Paint footerPaint = new Paint();
        
        SharedPreferences prefs = context.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
        String businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
        String userName = prefs.getString("user_name", "Professional Installer");
        String userNumber = prefs.getString("user_number", "");
        String currency = Formatter.getCurrencySymbol(context);

        String paymentMethodsJson = prefs.getString("payment_methods_json", "[]");
        Type listType = new TypeToken<ArrayList<PaymentMethod>>(){}.getType();
        List<PaymentMethod> paymentMethods = new Gson().fromJson(paymentMethodsJson, listType);

        boolean isLabourOnly = (projectItems == null || projectItems.isEmpty());

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // 1. Header Background
        accentPaint.setColor(Color.rgb(25, 118, 210)); 
        canvas.drawRect(75, 15, 520, 75, accentPaint);

        // 2. Watermark
        Paint watermarkPaint = new Paint();
        watermarkPaint.setColor(Color.LTGRAY);
        watermarkPaint.setAlpha(15);
        watermarkPaint.setTextSize(50);
        watermarkPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        canvas.save();
        canvas.rotate(-45, 297, 421);
        canvas.drawText(businessName, 50, 400, watermarkPaint);
        if (!userNumber.isEmpty()) {
            canvas.drawText(userNumber, 80, 460, watermarkPaint);
        }
        canvas.restore();

        // 3. Icons (Clear)
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.elec_man);
        if (drawable != null) {
            drawable.setTintList(null); 
            drawable.setBounds(15, 20, 65, 70);
            drawable.draw(canvas);
            drawable.setBounds(530, 20, 580, 70);
            drawable.draw(canvas);
        }

        // 4. Header Title
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(22);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        String headerType = isLabourOnly ? " - LABOUR INVOICE" : " - QUOTATION";
        canvas.drawText(businessName.toUpperCase() + headerType, 297, 55, titlePaint);
        
        int x = 40;
        int y = 110;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Issued by: " + userName, x, y, paint);
        y += 15;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        if (!userNumber.isEmpty()) {
            canvas.drawText("Contact: " + userNumber, x, y, paint);
        }

        // 5. Side-by-side Project and Client info
        y += 40;
        int rightColX = 320;
        int topSectionY = y;
        
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("PROJECT DETAILS", x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Name: " + project.getName(), x, y, paint);
        y += 15;
        canvas.drawText("Location: " + project.getLocation(), x, y, paint);
        y += 15;
        canvas.drawText("Date: " + new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date()), x, y, paint);

        int cy = topSectionY;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("CLIENT INFORMATION", rightColX, cy, paint);
        cy += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Name: " + project.getClientName(), rightColX, cy, paint);
        cy += 15;
        canvas.drawText("Contact: " + project.getClientContact(), rightColX, cy, paint);

        // 6. Table Header
        y += 40;
        accentPaint.setAlpha(255);
        canvas.drawRect(x - 5, y - 15, 555, y + 10, accentPaint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Description", x, y, paint);
        canvas.drawText("Qty/Info", x + 280, y, paint);
        canvas.drawText("Rate (" + currency + ")", x + 360, y, paint);
        canvas.drawText("Total (" + currency + ")", x + 460, y, paint);
        
        y += 30;
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        double materialTotal = 0;
        double labourTotal = 0;

        // Draw Materials if not labour-only
        if (projectItems != null && !projectItems.isEmpty()) {
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
            canvas.drawText("--- MATERIALS ---", x, y, paint);
            y += 20;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            for (ProjectItem pi : projectItems) {
                Item item = itemMap.get(pi.getItemId());
                String baseName = item != null ? item.getName() : "Item";
                if (pi.getVariantId() != null && variantMap != null) {
                    ItemVariant v = variantMap.get(pi.getVariantId());
                    if (v != null) baseName += " (" + v.getBrandName() + ")";
                }
                double total = pi.getQuantity() * pi.getQuotedPrice();
                materialTotal += total;

                canvas.drawText(baseName, x, y, paint);
                canvas.drawText(String.valueOf(pi.getQuantity()), x + 280, y, paint);
                canvas.drawText(Formatter.formatNumber(pi.getQuotedPrice()), x + 360, y, paint);
                canvas.drawText(Formatter.formatNumber(total), x + 460, y, paint);
                y += 20;
            }
            y += 10;
        }

        // Draw Labour Activities
        if (labourActivities != null && !labourActivities.isEmpty()) {
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
            canvas.drawText("--- LABOUR & ACTIVITIES ---", x, y, paint);
            y += 20;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            for (LabourActivity activity : labourActivities) {
                labourTotal += activity.getCost();
                canvas.drawText(activity.getName(), x, y, paint);
                canvas.drawText("Activity", x + 280, y, paint);
                canvas.drawText(Formatter.formatNumber(activity.getCost()), x + 360, y, paint);
                canvas.drawText(Formatter.formatNumber(activity.getCost()), x + 460, y, paint);
                y += 20;
            }
        }

        // Add Base Labour from Project
        double baseLabour = 0;
        if (project.getLabourPercentage() > 0) {
            baseLabour = (project.getLabourPercentage() / 100.0) * materialTotal;
        } else {
            baseLabour = project.getLabourCost();
        }
        
        if (baseLabour > 0) {
            labourTotal += baseLabour;
            String label = project.getLabourPercentage() > 0 ? "Project Labour (" + (int)project.getLabourPercentage() + "%)" : "Project Labour (Base)";
            canvas.drawText(label, x, y, paint);
            canvas.drawText("Base", x + 280, y, paint);
            canvas.drawText(Formatter.formatNumber(baseLabour), x + 360, y, paint);
            canvas.drawText(Formatter.formatNumber(baseLabour), x + 460, y, paint);
            y += 20;
        }

        y += 10;
        canvas.drawLine(x, y, 550, y, paint);
        y += 30;

        // Final Totals
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        if (!isLabourOnly) {
            canvas.drawText("Material Total:", x + 300, y, paint);
            canvas.drawText(Formatter.formatNumber(materialTotal), x + 460, y, paint);
            y += 20;
        }
        canvas.drawText("Labour Total:", x + 300, y, paint);
        canvas.drawText(Formatter.formatNumber(labourTotal), x + 460, y, paint);
        
        y += 30;
        paint.setTextSize(16);
        paint.setColor(Color.rgb(25, 118, 210));
        canvas.drawText("GRAND TOTAL:", x + 300, y, paint);
        canvas.drawText(currency + " " + Formatter.formatNumber(materialTotal + labourTotal), x + 440, y, paint);

        // Payments and Rules... (rest of the logic remains similar)
        y += 50;
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("PAYMENT INSTRUCTIONS:", x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        if (paymentMethods != null && !paymentMethods.isEmpty()) {
            for (int i = 0; i < paymentMethods.size(); i++) {
                canvas.drawText(paymentMethods.get(i).getDisplayText(), x, y, paint);
                y += 15;
                if (i < paymentMethods.size() - 1) y += 5;
            }
        }

        y += 30;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Rules of Engagement:", x, y, paint);
        y += 15;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextSize(10);
        String rules = project.getRulesOfEngagement() != null ? project.getRulesOfEngagement() : "N/A";
        if (rules.length() > 90) {
             canvas.drawText(rules.substring(0, 90), x, y, paint);
             y += 12;
             canvas.drawText(rules.substring(90, Math.min(rules.length(), 180)), x, y, paint);
        } else {
             canvas.drawText(rules, x, y, paint);
        }

        // Footer
        canvas.drawRect(0, 805, 595, 842, accentPaint);
        footerPaint.setColor(Color.WHITE);
        footerPaint.setTextSize(8);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("© Designed by undrix Int | 0716729060 | wilsonundrix@gmail.com | Wilson Maina", 595/2, 822, footerPaint);
        canvas.drawText("Generated via " + businessName + " Management System", 595/2, 835, footerPaint);

        pdfDocument.finishPage(page);
        File filePath = new File(context.getExternalCacheDir(), "Invoice_" + project.getId() + ".pdf");
        try { pdfDocument.writeTo(new FileOutputStream(filePath)); } catch (IOException e) { return null; } finally { pdfDocument.close(); }
        return filePath;
    }
}
