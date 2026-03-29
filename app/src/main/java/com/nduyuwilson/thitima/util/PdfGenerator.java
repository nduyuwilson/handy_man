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
import com.nduyuwilson.thitima.data.entity.Payment;
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

    private static final int PAGE_HEIGHT = 842;
    private static final int FOOTER_START = 805;
    private static final int CONTENT_END_LIMIT = 780;

    public static File generateInvoice(Context context, Project project, List<ProjectItem> projectItems, List<LabourActivity> labourActivities, Map<Integer, Item> itemMap, Map<Integer, ItemVariant> variantMap) {
        PdfDocument pdfDocument = new PdfDocument();
        
        SharedPreferences prefs = context.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
        String businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
        String userName = prefs.getString("user_name", "Professional Installer");
        String userNumber = prefs.getString("user_number", "");
        String currency = Formatter.getCurrencySymbol(context);

        String paymentMethodsJson = prefs.getString("payment_methods_json", "[]");
        Type listType = new TypeToken<ArrayList<PaymentMethod>>(){}.getType();
        List<PaymentMethod> paymentMethods = new Gson().fromJson(paymentMethodsJson, listType);

        boolean isLabourOnly = (projectItems == null || projectItems.isEmpty());

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, PAGE_HEIGHT, 1).create();
        final PdfDocument.Page[] currentPage = {pdfDocument.startPage(pageInfo)};
        final Canvas[] canvas = {currentPage[0].getCanvas()};
        final int[] y = {110};

        drawHeader(context, canvas[0], businessName, isLabourOnly);
        drawWatermark(canvas[0], businessName, userNumber);
        drawFooter(canvas[0], businessName);

        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas[0].drawText("Issued by: " + userName, 40, y[0], paint);
        y[0] += 15;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        if (!userNumber.isEmpty()) {
            canvas[0].drawText("Contact: " + userNumber, 40, y[0], paint);
            y[0] += 25;
        }

        drawProjectAndClientInfo(canvas[0], project, y[0]);
        y[0] += 80;

        drawTableHeader(canvas[0], currency, y[0]);
        y[0] += 30;

        double materialTotal = 0;
        double labourTotal = 0;

        if (projectItems != null && !projectItems.isEmpty()) {
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
            canvas[0].drawText("--- MATERIALS ---", 40, y[0], paint);
            y[0] += 20;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            for (ProjectItem pi : projectItems) {
                if (y[0] > CONTENT_END_LIMIT) {
                    canvas[0] = startNewPage(pdfDocument, pageInfo, currentPage, businessName, isLabourOnly, userNumber);
                    y[0] = 110;
                    drawTableHeader(canvas[0], currency, y[0]);
                    y[0] += 30;
                }
                Item item = itemMap.get(pi.getItemId());
                String baseName = item != null ? item.getName() : "Item";
                if (pi.getVariantId() != null && variantMap != null) {
                    ItemVariant v = variantMap.get(pi.getVariantId());
                    if (v != null) baseName += " (" + v.getBrandName() + ")";
                }
                double total = pi.getQuantity() * pi.getQuotedPrice();
                materialTotal += total;
                canvas[0].drawText(baseName, 40, y[0], paint);
                canvas[0].drawText(String.valueOf(pi.getQuantity()), 320, y[0], paint);
                canvas[0].drawText(Formatter.formatNumber(pi.getQuotedPrice()), 400, y[0], paint);
                canvas[0].drawText(Formatter.formatNumber(total), 500, y[0], paint);
                y[0] += 20;
            }
            y[0] += 10;
        }

        if (labourActivities != null && !labourActivities.isEmpty()) {
            if (y[0] > CONTENT_END_LIMIT) {
                canvas[0] = startNewPage(pdfDocument, pageInfo, currentPage, businessName, isLabourOnly, userNumber);
                y[0] = 110;
                drawTableHeader(canvas[0], currency, y[0]);
                y[0] += 30;
            }
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
            canvas[0].drawText("--- LABOUR & ACTIVITIES ---", 40, y[0], paint);
            y[0] += 20;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            for (LabourActivity activity : labourActivities) {
                if (y[0] > CONTENT_END_LIMIT) {
                    canvas[0] = startNewPage(pdfDocument, pageInfo, currentPage, businessName, isLabourOnly, userNumber);
                    y[0] = 110;
                    drawTableHeader(canvas[0], currency, y[0]);
                    y[0] += 30;
                }
                labourTotal += activity.getCost();
                canvas[0].drawText(activity.getName(), 40, y[0], paint);
                canvas[0].drawText("Activity", 320, y[0], paint);
                canvas[0].drawText(Formatter.formatNumber(activity.getCost()), 400, y[0], paint);
                canvas[0].drawText(Formatter.formatNumber(activity.getCost()), 500, y[0], paint);
                y[0] += 20;
            }
        }

        double baseLabour = 0;
        if (project.getLabourPercentage() > 0) baseLabour = (project.getLabourPercentage() / 100.0) * materialTotal;
        else baseLabour = project.getLabourCost();
        
        if (baseLabour > 0) {
            if (y[0] > CONTENT_END_LIMIT) {
                canvas[0] = startNewPage(pdfDocument, pageInfo, currentPage, businessName, isLabourOnly, userNumber);
                y[0] = 110;
            }
            labourTotal += baseLabour;
            canvas[0].drawText("Project Base Labour", 40, y[0], paint);
            canvas[0].drawText("Base", 320, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(baseLabour), 400, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(baseLabour), 500, y[0], paint);
            y[0] += 30;
        }

        if (y[0] > 600) {
            canvas[0] = startNewPage(pdfDocument, pageInfo, currentPage, businessName, isLabourOnly, userNumber);
            y[0] = 110;
        }
        y[0] += 10;
        canvas[0].drawLine(40, y[0], 550, y[0], paint);
        y[0] += 30;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        if (!isLabourOnly) {
            canvas[0].drawText("Material Total:", 300, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(materialTotal), 500, y[0], paint);
            y[0] += 20;
        }
        canvas[0].drawText("Labour Total:", 300, y[0], paint);
        canvas[0].drawText(Formatter.formatNumber(labourTotal), 500, y[0], paint);
        y[0] += 30;
        paint.setTextSize(16);
        paint.setColor(Color.rgb(25, 118, 210));
        canvas[0].drawText("GRAND TOTAL:", 300, y[0], paint);
        canvas[0].drawText(currency + " " + Formatter.formatNumber(materialTotal + labourTotal), 440, y[0], paint);

        y[0] += 50;
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas[0].drawText("PAYMENT INSTRUCTIONS:", 40, y[0], paint);
        y[0] += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        if (paymentMethods != null && !paymentMethods.isEmpty()) {
            for (PaymentMethod pm : paymentMethods) {
                canvas[0].drawText(pm.getDisplayText(), 40, y[0], paint);
                y[0] += 15;
            }
        }

        y[0] += 30;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas[0].drawText("Rules of Engagement:", 40, y[0], paint);
        y[0] += 15;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextSize(10);
        String rules = project.getRulesOfEngagement() != null ? project.getRulesOfEngagement() : "N/A";
        canvas[0].drawText(rules.length() > 90 ? rules.substring(0, 90) + "..." : rules, 40, y[0], paint);

        pdfDocument.finishPage(currentPage[0]);
        File filePath = new File(context.getExternalCacheDir(), "Invoice_" + project.getId() + ".pdf");
        try { pdfDocument.writeTo(new FileOutputStream(filePath)); } catch (IOException e) { return null; } finally { pdfDocument.close(); }
        return filePath;
    }

    public static File generateReceipt(Context context, Project project, Payment payment) {
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

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Header
        accentPaint.setColor(Color.rgb(25, 118, 210));
        canvas.drawRect(75, 15, 520, 75, accentPaint);

        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(22);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(businessName.toUpperCase(), 297, 45, titlePaint);

        titlePaint.setTextSize(14);
        canvas.drawText("OFFICIAL RECEIPT", 297, 65, titlePaint);

        drawWatermark(canvas, businessName, userNumber);

        int x = 40;
        int y = 120;
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("RECEIPT DETAILS", x, y, paint);
        y += 25;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Receipt No: RCPT-" + payment.getId(), x, y, paint);
        y += 20;
        canvas.drawText("Date: " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(new Date(payment.getDate())), x, y, paint);
        y += 20;
        canvas.drawText("Project: " + project.getName(), x, y, paint);

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("RECEIVED FROM", x, y, paint);
        y += 25;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Client Name: " + project.getClientName(), x, y, paint);
        y += 15;
        canvas.drawText("Client Contact: " + project.getClientContact(), x, y, paint);

        y += 50;
        accentPaint.setAlpha(255);
        canvas.drawRect(35, y - 15, 555, y + 10, accentPaint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Payment Method", x, y, paint);
        canvas.drawText("Reference", x + 200, y, paint);
        canvas.drawText("Amount", x + 400, y, paint);

        y += 30;
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(payment.getMethod(), x, y, paint);
        canvas.drawText(payment.getReference(), x + 200, y, paint);
        canvas.drawText(currency + " " + Formatter.formatNumber(payment.getAmount()), x + 400, y, paint);

        y += 60;
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("TOTAL PAID: " + currency + " " + Formatter.formatNumber(payment.getAmount()), x, y, paint);

        y += 100;
        paint.setTextSize(12);
        canvas.drawText("________________________", x, y, paint);
        y += 20;
        canvas.drawText("Signature / Stamp", x, y, paint);

        drawFooter(canvas, businessName);

        pdfDocument.finishPage(page);
        File filePath = new File(context.getExternalCacheDir(), "Receipt_" + payment.getId() + ".pdf");
        try { pdfDocument.writeTo(new FileOutputStream(filePath)); } catch (IOException e) { return null; } finally { pdfDocument.close(); }
        return filePath;
    }

    private static void drawHeader(Context context, Canvas canvas, String businessName, boolean isLabourOnly) {
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.rgb(25, 118, 210));
        canvas.drawRect(75, 15, 520, 75, accentPaint);

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.elec_man);
        if (drawable != null) {
            drawable.setTintList(null);
            drawable.setBounds(15, 20, 65, 70);
            drawable.draw(canvas);
            drawable.setBounds(530, 20, 580, 70);
            drawable.draw(canvas);
        }

        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(22);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(businessName.toUpperCase(), 297, 45, titlePaint);

        titlePaint.setTextSize(14);
        canvas.drawText(isLabourOnly ? "LABOUR INVOICE" : "QUOTATION", 297, 65, titlePaint);
    }

    private static void drawFooter(Canvas canvas, String businessName) {
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.rgb(25, 118, 210));
        canvas.drawRect(0, 805, 595, 842, accentPaint);

        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.WHITE);
        footerPaint.setTextSize(8);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("© Designed by undrix Int | 0716729060 | wilsonundrix@gmail.com | Wilson Maina", 595/2, 822, footerPaint);
        canvas.drawText("Generated via " + businessName + " Management System", 595/2, 835, footerPaint);
    }

    private static void drawWatermark(Canvas canvas, String businessName, String userNumber) {
        Paint watermarkPaint = new Paint();
        watermarkPaint.setColor(Color.LTGRAY);
        watermarkPaint.setAlpha(15);
        watermarkPaint.setTextSize(50);
        watermarkPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.save();
        canvas.rotate(-45, 297, 421);
        canvas.drawText(businessName, 50, 400, watermarkPaint);
        if (!userNumber.isEmpty()) canvas.drawText(userNumber, 80, 460, watermarkPaint);
        canvas.restore();
    }

    private static void drawProjectAndClientInfo(Canvas canvas, Project project, int y) {
        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("PROJECT DETAILS", 40, y, paint);
        canvas.drawText("CLIENT INFORMATION", 320, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Name: " + project.getName(), 40, y, paint);
        canvas.drawText("Name: " + project.getClientName(), 320, y, paint);
        y += 15;
        canvas.drawText("Location: " + project.getLocation(), 40, y, paint);
        canvas.drawText("Contact: " + project.getClientContact(), 320, y, paint);
        y += 15;
        canvas.drawText("Date: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()), 40, y, paint);
    }

    private static void drawTableHeader(Canvas canvas, String currency, int y) {
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.rgb(25, 118, 210));
        canvas.drawRect(35, y - 15, 555, y + 10, accentPaint);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(12);
        canvas.drawText("Description", 40, y, paint);
        canvas.drawText("Qty", 320, y, paint);
        canvas.drawText("Rate (" + currency + ")", 400, y, paint);
        canvas.drawText("Total (" + currency + ")", 500, y, paint);
    }

    private static Canvas startNewPage(PdfDocument pdfDocument, PdfDocument.PageInfo pageInfo, PdfDocument.Page[] currentPage, String businessName, boolean isLabourOnly, String userNumber) {
        pdfDocument.finishPage(currentPage[0]);
        currentPage[0] = pdfDocument.startPage(pageInfo);
        Canvas canvas = currentPage[0].getCanvas();
        drawHeader(null, canvas, businessName, isLabourOnly);
        drawWatermark(canvas, businessName, userNumber);
        drawFooter(canvas, businessName);
        return canvas;
    }
}
