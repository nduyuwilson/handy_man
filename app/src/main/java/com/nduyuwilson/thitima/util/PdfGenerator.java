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

import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfGenerator {

    public static File generateInvoice(Context context, Project project, List<ProjectItem> projectItems, Map<Integer, Item> itemMap, Map<Integer, ItemVariant> variantMap) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint accentPaint = new Paint();
        Paint footerPaint = new Paint();
        
        SharedPreferences prefs = context.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
        String businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
        String userName = prefs.getString("user_name", "Professional Installer");
        String userNumber = prefs.getString("user_number", "");
        String bankDetails = prefs.getString("bank_details", "");
        String paybillNumber = prefs.getString("paybill_number", "");
        String paybillAccount = prefs.getString("paybill_account", "");
        String tillNumber = prefs.getString("till_number", "");
        String currency = Formatter.getCurrencySymbol(context);

        // A4 size: 595 x 842 points
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // 1. Draw Padded Themed Header Background (Centered Blue Bar)
        accentPaint.setColor(Color.rgb(25, 118, 210)); 
        canvas.drawRect(75, 15, 520, 75, accentPaint);

        // 2. Watermark - Name and Phone on Different Lines
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

        // 3. Draw Icons on both sides of the Title Bar (Clear, no blue tint)
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.elec_man);
        if (drawable != null) {
            // Left Icon - Original sharp colors
            drawable.setTintList(null); 
            drawable.setBounds(15, 20, 65, 70);
            drawable.draw(canvas);
            
            // Right Icon - Original sharp colors
            drawable.setBounds(530, 20, 580, 70);
            drawable.draw(canvas);
        }

        // 4. Header Title (Centered in the Blue strip)
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(businessName.toUpperCase(), 297, 55, titlePaint);
        
        // Sub-header info
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

        // Table Header
        y += 40;
        accentPaint.setAlpha(255);
        canvas.drawRect(x - 5, y - 15, 555, y + 10, accentPaint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Item Description", x, y, paint);
        canvas.drawText("Qty", x + 280, y, paint);
        canvas.drawText("Price (" + currency + ")", x + 330, y, paint);
        canvas.drawText("Total (" + currency + ")", x + 450, y, paint);
        
        y += 30;
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        double materialTotal = 0;
        if (projectItems != null) {
            for (ProjectItem pi : projectItems) {
                Item item = itemMap.get(pi.getItemId());
                String baseName = item != null ? item.getName() : "Unknown Item";
                String brandName = "";
                if (pi.getVariantId() != null && variantMap != null) {
                    ItemVariant variant = variantMap.get(pi.getVariantId());
                    if (variant != null) brandName = " (" + variant.getBrandName() + ")";
                }
                
                String fullName = baseName + brandName;
                double total = pi.getQuantity() * pi.getQuotedPrice();
                materialTotal += total;

                if (fullName.length() > 40) fullName = fullName.substring(0, 37) + "...";

                canvas.drawText(fullName, x, y, paint);
                canvas.drawText(String.valueOf(pi.getQuantity()), x + 280, y, paint);
                canvas.drawText(Formatter.formatNumber(pi.getQuotedPrice()), x + 330, y, paint);
                canvas.drawText(Formatter.formatNumber(total), x + 450, y, paint);
                
                y += 20;
                if (y > 650) break; 
            }
        }

        y += 10;
        canvas.drawLine(x, y, 550, y, paint);
        y += 30;

        // Financials
        double labourCost = project.getLabourCost();
        if (project.getLabourPercentage() > 0) {
            labourCost = (project.getLabourPercentage() / 100.0) * materialTotal;
        }

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Material Total:", x + 280, y, paint);
        canvas.drawText(Formatter.formatNumber(materialTotal), x + 450, y, paint);
        
        y += 20;
        String labourLabel = project.getLabourPercentage() > 0 ? "Labour (" + (int)project.getLabourPercentage() + "%):" : "Labour Cost:";
        canvas.drawText(labourLabel, x + 280, y, paint);
        canvas.drawText(Formatter.formatNumber(labourCost), x + 450, y, paint);
        
        y += 30;
        paint.setTextSize(16);
        paint.setColor(Color.rgb(25, 118, 210));
        canvas.drawText("GRAND TOTAL:", x + 280, y, paint);
        canvas.drawText(currency + " " + Formatter.formatNumber(materialTotal + labourCost), x + 420, y, paint);

        // Payments
        y += 50;
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("PAYMENT INSTRUCTIONS:", x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        boolean hasPay = false;
        if (!bankDetails.isEmpty()) {
            canvas.drawText("BANK: " + bankDetails, x, y, paint);
            y += 15;
            hasPay = true;
        }
        if (!paybillNumber.isEmpty()) {
            String pb = "M-PESA PAYBILL: " + paybillNumber;
            if (!paybillAccount.isEmpty()) pb += " | ACCOUNT: " + paybillAccount;
            canvas.drawText(pb, x, y, paint);
            y += 15;
            hasPay = true;
        }
        if (!tillNumber.isEmpty()) {
            canvas.drawText("M-PESA TILL: " + tillNumber, x, y, paint);
            y += 15;
            hasPay = true;
        }

        // Rules
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

        // 6. Draw Themed Footer with Developer Branding
        canvas.drawRect(0, 805, 595, 842, accentPaint);
        footerPaint.setColor(Color.WHITE);
        footerPaint.setTextSize(8);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("© Designed by undrix Int | 0716729060 | wilsonundrix@gmail.com | Wilson Maina", 595/2, 822, footerPaint);
        canvas.drawText("Generated via " + businessName + " Management System", 595/2, 835, footerPaint);

        pdfDocument.finishPage(page);

        File filePath = new File(context.getExternalCacheDir(), "Quotation_" + project.getId() + ".pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            pdfDocument.close();
        }

        return filePath;
    }
}
