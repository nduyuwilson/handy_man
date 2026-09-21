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
    private static final int PAGE_WIDTH = 595;
    private static final int FOOTER_START = 805;
    private static final int CONTENT_END_LIMIT = 780;

    public static File generateInvoice(Context context, Project project, List<ProjectItem> projectItems, List<LabourActivity> labourActivities, Map<Integer, Item> itemMap, Map<Integer, ItemVariant> variantMap) {
        PdfDocument pdfDocument = new PdfDocument();
        
        SharedPreferences prefs = AppPrefs.getPreferences(context);
        String businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
        String userName = prefs.getString("user_name", "Professional Installer");
        String userNumber = prefs.getString("user_number", "");
        String currency = Formatter.getCurrencySymbol(context);

        String paymentMethodsJson = prefs.getString("payment_methods_json", "[]");
        Type listType = new TypeToken<ArrayList<PaymentMethod>>(){}.getType();
        List<PaymentMethod> paymentMethods = new Gson().fromJson(paymentMethodsJson, listType);

        boolean isLabourOnly = (projectItems == null || projectItems.isEmpty());

        final int[] pageNumber = {1};
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber[0]).create();
        final PdfDocument.Page[] currentPage = {pdfDocument.startPage(pageInfo)};
        final Canvas[] canvas = {currentPage[0].getCanvas()};
        canvas[0].drawColor(Color.WHITE);
        final int[] y = {110};

        drawHeader(context, canvas[0], businessName, isLabourOnly);
        drawWatermark(canvas[0], businessName, userNumber);
        drawFooter(canvas[0], businessName, pageNumber[0]);

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
                    canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
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

                // Ellipsize description if it exceeds column boundary
                String displayName = baseName;
                if (paint.measureText(displayName) > 270) {
                    while (displayName.length() > 3 && paint.measureText(displayName + "...") > 270) {
                        displayName = displayName.substring(0, displayName.length() - 1);
                    }
                    displayName += "...";
                }

                canvas[0].drawText(displayName, 40, y[0], paint);
                canvas[0].drawText(String.valueOf(pi.getQuantity()), 320, y[0], paint);
                canvas[0].drawText(Formatter.formatNumber(pi.getQuotedPrice()), 400, y[0], paint);
                canvas[0].drawText(Formatter.formatNumber(total), 500, y[0], paint);
                y[0] += 20;
            }
            y[0] += 10;
        }

        if (labourActivities != null && !labourActivities.isEmpty()) {
            if (y[0] > CONTENT_END_LIMIT) {
                canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
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
                    canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                    y[0] = 110;
                    drawTableHeader(canvas[0], currency, y[0]);
                    y[0] += 30;
                }
                labourTotal += activity.getCost();

                String actName = activity.getName();
                if (paint.measureText(actName) > 270) {
                    while (actName.length() > 3 && paint.measureText(actName + "...") > 270) {
                        actName = actName.substring(0, actName.length() - 1);
                    }
                    actName += "...";
                }

                canvas[0].drawText(actName, 40, y[0], paint);
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
                canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                y[0] = 110;
            }
            labourTotal += baseLabour;
            canvas[0].drawText("Project Base Labour", 40, y[0], paint);
            canvas[0].drawText("Base", 320, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(baseLabour), 400, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(baseLabour), 500, y[0], paint);
            y[0] += 30;
        }

        if (y[0] > 580) {
            canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
            y[0] = 110;
        }
        y[0] += 10;
        canvas[0].drawLine(40, y[0], 550, y[0], paint);
        y[0] += 30;
        if (!isLabourOnly) {
            canvas[0].drawText("Material Total:", 300, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(materialTotal), 500, y[0], paint);
            y[0] += 20;
        }
        canvas[0].drawText("Labour Total:", 300, y[0], paint);
        canvas[0].drawText(Formatter.formatNumber(labourTotal), 500, y[0], paint);
        y[0] += 20;

        double subtotal = materialTotal + labourTotal;
        if (project.isIncludeVat()) {
            double vatAmount = subtotal * 0.16;
            double grandTotal = subtotal + vatAmount;

            canvas[0].drawText("Subtotal:", 300, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(subtotal), 500, y[0], paint);
            y[0] += 20;

            canvas[0].drawText("VAT (16%):", 300, y[0], paint);
            canvas[0].drawText(Formatter.formatNumber(vatAmount), 500, y[0], paint);
            y[0] += 30;

            paint.setTextSize(15);
            paint.setColor(Color.rgb(25, 118, 210));
            canvas[0].drawText("TOTAL (INCL. 16% VAT):", 240, y[0], paint);
            canvas[0].drawText(currency + " " + Formatter.formatNumber(grandTotal), 440, y[0], paint);
        } else {
            y[0] += 10;
            paint.setTextSize(16);
            paint.setColor(Color.rgb(25, 118, 210));
            canvas[0].drawText("GRAND TOTAL:", 300, y[0], paint);
            canvas[0].drawText(currency + " " + Formatter.formatNumber(subtotal), 440, y[0], paint);
        }

        if (paymentMethods != null && !paymentMethods.isEmpty()) {
            if (y[0] > CONTENT_END_LIMIT - 60) {
                canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                y[0] = 110;
            } else {
                y[0] += 35;
            }
            paint.setColor(Color.BLACK);
            paint.setTextSize(11);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas[0].drawText("PAYMENT INSTRUCTIONS:", 40, y[0], paint);
            y[0] += 18;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setTextSize(9.5f);
            for (PaymentMethod pm : paymentMethods) {
                if (y[0] > CONTENT_END_LIMIT) {
                    canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                    y[0] = 110;
                }
                canvas[0].drawText(pm.getDisplayText(), 40, y[0], paint);
                y[0] += 15;
            }
        }

        String rules = project.getRulesOfEngagement() != null ? project.getRulesOfEngagement().trim() : "";
        if (!rules.isEmpty()) {
            if (y[0] > CONTENT_END_LIMIT - 90) {
                canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                y[0] = 110;
            } else {
                y[0] += 20;
            }

            final int boxLeft = 35;
            final int boxRight = 555;
            final int textLeft = 48;
            final float maxLineWidth = 495f;
            final float lineHeight = 13.5f;

            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.rgb(203, 213, 225));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1f);

            Paint headerBgPaint = new Paint();
            headerBgPaint.setColor(Color.rgb(241, 245, 249));
            headerBgPaint.setStyle(Paint.Style.FILL);

            Paint accentBarPaint = new Paint();
            accentBarPaint.setColor(Color.rgb(0, 137, 123));
            accentBarPaint.setStyle(Paint.Style.FILL);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.rgb(30, 41, 59));
            textPaint.setTextSize(9f);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            Paint headerTextPaint = new Paint();
            headerTextPaint.setColor(Color.rgb(15, 23, 42));
            headerTextPaint.setTextSize(9.5f);
            headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            Paint contNotePaint = new Paint();
            contNotePaint.setColor(Color.rgb(100, 116, 139));
            contNotePaint.setTextSize(7.5f);
            contNotePaint.setTextAlign(Paint.Align.RIGHT);
            contNotePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));

            int[] boxStartY = {y[0]};

            drawRulesHeader(canvas[0], boxLeft, boxRight, boxStartY[0], headerBgPaint, borderPaint, accentBarPaint, headerTextPaint, false);
            y[0] = boxStartY[0] + 32;

            String[] paragraphs = rules.split("\\r?\\n");
            for (String paragraph : paragraphs) {
                String trimmed = paragraph.trim();
                if (trimmed.isEmpty()) {
                    if (y[0] + lineHeight > CONTENT_END_LIMIT - 15) {
                        int curBoxEnd = CONTENT_END_LIMIT - 10;
                        canvas[0].drawLine(boxLeft, boxStartY[0], boxLeft, curBoxEnd, borderPaint);
                        canvas[0].drawLine(boxRight, boxStartY[0], boxRight, curBoxEnd, borderPaint);
                        canvas[0].drawLine(boxLeft, curBoxEnd, boxRight, curBoxEnd, borderPaint);
                        canvas[0].drawText("[Continued on next page...]", boxRight - 8, curBoxEnd - 4, contNotePaint);

                        canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                        y[0] = 110;
                        boxStartY[0] = y[0];
                        drawRulesHeader(canvas[0], boxLeft, boxRight, boxStartY[0], headerBgPaint, borderPaint, accentBarPaint, headerTextPaint, true);
                        y[0] = boxStartY[0] + 32;
                    } else {
                        y[0] += 6;
                    }
                    continue;
                }

                String[] words = trimmed.split("\\s+");
                StringBuilder lineBuilder = new StringBuilder();
                for (String word : words) {
                    String testLine = lineBuilder.length() == 0 ? word : lineBuilder + " " + word;
                    if (textPaint.measureText(testLine) <= maxLineWidth) {
                        lineBuilder = new StringBuilder(testLine);
                    } else {
                        if (y[0] + lineHeight > CONTENT_END_LIMIT - 15) {
                            int curBoxEnd = CONTENT_END_LIMIT - 10;
                            canvas[0].drawLine(boxLeft, boxStartY[0], boxLeft, curBoxEnd, borderPaint);
                            canvas[0].drawLine(boxRight, boxStartY[0], boxRight, curBoxEnd, borderPaint);
                            canvas[0].drawLine(boxLeft, curBoxEnd, boxRight, curBoxEnd, borderPaint);
                            canvas[0].drawText("[Continued on next page...]", boxRight - 8, curBoxEnd - 4, contNotePaint);

                            canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                            y[0] = 110;
                            boxStartY[0] = y[0];
                            drawRulesHeader(canvas[0], boxLeft, boxRight, boxStartY[0], headerBgPaint, borderPaint, accentBarPaint, headerTextPaint, true);
                            y[0] = boxStartY[0] + 32;
                        }
                        canvas[0].drawText(lineBuilder.toString(), textLeft, y[0], textPaint);
                        y[0] += lineHeight;
                        lineBuilder = new StringBuilder(word);
                    }
                }
                if (lineBuilder.length() > 0) {
                    if (y[0] + lineHeight > CONTENT_END_LIMIT - 15) {
                        int curBoxEnd = CONTENT_END_LIMIT - 10;
                        canvas[0].drawLine(boxLeft, boxStartY[0], boxLeft, curBoxEnd, borderPaint);
                        canvas[0].drawLine(boxRight, boxStartY[0], boxRight, curBoxEnd, borderPaint);
                        canvas[0].drawLine(boxLeft, curBoxEnd, boxRight, curBoxEnd, borderPaint);
                        canvas[0].drawText("[Continued on next page...]", boxRight - 8, curBoxEnd - 4, contNotePaint);

                        canvas[0] = startNewPage(context, pdfDocument, pageNumber, currentPage, businessName, isLabourOnly, userNumber);
                        y[0] = 110;
                        boxStartY[0] = y[0];
                        drawRulesHeader(canvas[0], boxLeft, boxRight, boxStartY[0], headerBgPaint, borderPaint, accentBarPaint, headerTextPaint, true);
                        y[0] = boxStartY[0] + 32;
                    }
                    canvas[0].drawText(lineBuilder.toString(), textLeft, y[0], textPaint);
                    y[0] += lineHeight;
                }
                y[0] += 4;
            }

            int finalBoxEnd = (int) (y[0] + 6);
            canvas[0].drawLine(boxLeft, boxStartY[0], boxLeft, finalBoxEnd, borderPaint);
            canvas[0].drawLine(boxRight, boxStartY[0], boxRight, finalBoxEnd, borderPaint);
            canvas[0].drawLine(boxLeft, finalBoxEnd, boxRight, finalBoxEnd, borderPaint);
            y[0] = finalBoxEnd + 15;
        }

        pdfDocument.finishPage(currentPage[0]);
        File filePath = createPdfFile(context, isLabourOnly ? "Labour_Invoice" : "Quotation", project, null);
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            pdfDocument.writeTo(fos);
            fos.close();
        } catch (IOException e) {
            return null;
        } finally {
            pdfDocument.close();
        }
        return filePath;
    }

    public static File generateReceipt(Context context, Project project, Payment payment) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        SharedPreferences prefs = AppPrefs.getPreferences(context);
        String businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
        String userNumber = prefs.getString("user_number", "");
        String currency = Formatter.getCurrencySymbol(context);

        drawHeader(context, canvas, businessName, false);
        drawWatermark(canvas, businessName, userNumber);
        drawFooter(canvas, businessName);

        Paint paint = new Paint();
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("OFFICIAL RECEIPT", 297, 65, paint);

        int x = 40;
        int y = 120;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(12);
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
        canvas.drawText("Client: " + project.getClientName(), x, y, paint);
        y += 15;
        canvas.drawText("Contact: " + project.getClientContact(), x, y, paint);

        y += 50;
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.rgb(25, 118, 210));
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

        if (payment.getMpesaMessage() != null && !payment.getMpesaMessage().trim().isEmpty()) {
            y += 40;
            Paint mpesaBoxPaint = new Paint();
            mpesaBoxPaint.setColor(Color.rgb(240, 245, 240));
            canvas.drawRect(35, y - 10, 555, y + 55, mpesaBoxPaint);

            Paint mpesaTitlePaint = new Paint();
            mpesaTitlePaint.setColor(Color.rgb(38, 142, 60));
            mpesaTitlePaint.setTextSize(10);
            mpesaTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("M-PESA PAYMENT VERIFICATION REFERENCE:", x, y + 8, mpesaTitlePaint);

            Paint mpesaTextPaint = new Paint();
            mpesaTextPaint.setColor(Color.DKGRAY);
            mpesaTextPaint.setTextSize(8.5f);
            mpesaTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
            String msg = payment.getMpesaMessage().trim();
            if (msg.length() > 95) {
                canvas.drawText(msg.substring(0, 95), x, y + 26, mpesaTextPaint);
                int secondEnd = Math.min(msg.length(), 190);
                canvas.drawText(msg.substring(95, secondEnd), x, y + 42, mpesaTextPaint);
            } else {
                canvas.drawText(msg, x, y + 26, mpesaTextPaint);
            }
            y += 35;
        }

        y += 80;
        paint.setTextSize(12);
        canvas.drawText("________________________", x, y, paint);
        y += 20;
        canvas.drawText("Signature / Stamp", x, y, paint);

        pdfDocument.finishPage(page);
        MpesaParser.MpesaResult parsed = payment.getMpesaMessage() != null ? MpesaParser.parse(payment.getMpesaMessage()) : null;
        String extra = (parsed != null && parsed.isSuccess && !parsed.transactionCode.isEmpty()) ? parsed.transactionCode : ("Payment_" + payment.getId());
        File filePath = createPdfFile(context, "Receipt", project, extra);
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            pdfDocument.writeTo(fos);
            fos.close();
        } catch (IOException e) {
            return null;
        } finally {
            pdfDocument.close();
        }
        return filePath;
    }

    public static File generateLabourActivityInvoice(Context context, Project project, LabourActivity activity) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        SharedPreferences prefs = AppPrefs.getPreferences(context);
        String businessName = prefs.getString("business_name", "THITIMA ELECTRICALS");
        String userNumber = prefs.getString("user_number", "");
        String currency = Formatter.getCurrencySymbol(context);

        drawHeader(context, canvas, businessName, true);
        drawWatermark(canvas, businessName, userNumber);
        drawFooter(canvas, businessName);

        int x = 40;
        int y = 120;
        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("LABOUR INVOICE DETAILS", x, y, paint);
        y += 25;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Invoice No: LAB-" + activity.getId(), x, y, paint);
        y += 20;
        canvas.drawText("Date: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(activity.getDate())), x, y, paint);
        y += 20;
        canvas.drawText("Project: " + project.getName(), x, y, paint);

        drawProjectAndClientInfo(canvas, project, y + 40);
        y += 120;

        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.rgb(25, 118, 210));
        canvas.drawRect(35, y - 15, 555, y + 10, accentPaint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Service Description", x, y, paint);
        canvas.drawText("Category", x + 300, y, paint);
        canvas.drawText("Cost (" + currency + ")", x + 420, y, paint);

        y += 30;
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(activity.getName(), x, y, paint);
        canvas.drawText("Labour Activity", x + 300, y, paint);
        canvas.drawText(Formatter.formatNumber(activity.getCost()), x + 420, y, paint);

        if (project != null && project.isIncludeVat()) {
            double cost = activity.getCost();
            double vat = cost * 0.16;
            double totalWithVat = cost + vat;

            y += 40;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("Subtotal: " + currency + " " + Formatter.formatNumber(cost), x, y, paint);
            y += 20;
            canvas.drawText("VAT (16%): " + currency + " " + Formatter.formatNumber(vat), x, y, paint);
            y += 30;
            paint.setTextSize(15);
            paint.setColor(Color.rgb(25, 118, 210));
            canvas.drawText("TOTAL AMOUNT (INCL. 16% VAT): " + currency + " " + Formatter.formatNumber(totalWithVat), x, y, paint);
        } else {
            y += 60;
            paint.setTextSize(16);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("TOTAL AMOUNT: " + currency + " " + Formatter.formatNumber(activity.getCost()), x, y, paint);
        }

        pdfDocument.finishPage(page);
        File filePath = createPdfFile(context, "Labour_Activity", project, activity != null ? activity.getName() : null);
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            pdfDocument.writeTo(fos);
            fos.close();
        } catch (IOException e) {
            return null;
        } finally {
            pdfDocument.close();
        }
        return filePath;
    }

    private static void drawHeader(Context context, Canvas canvas, String businessName, boolean isLabourOnly) {
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.rgb(25, 118, 210));
        canvas.drawRect(75, 15, 520, 75, accentPaint);

        if (context != null) {
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.elec_man);
            if (drawable != null) {
                drawable.setTintList(null);
                drawable.setBounds(15, 20, 65, 70);
                drawable.draw(canvas);
                drawable.setBounds(530, 20, 580, 70);
                drawable.draw(canvas);
            }
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
        drawFooter(canvas, businessName, 1);
    }

    private static void drawFooter(Canvas canvas, String businessName, int pageNum) {
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.rgb(25, 118, 210));
        canvas.drawRect(0, 805, PAGE_WIDTH, PAGE_HEIGHT, accentPaint);

        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.WHITE);
        footerPaint.setTextSize(8);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("© Designed by undrix Int | 0716729060 | wilsonundrix@gmail.com | Wilson Maina", PAGE_WIDTH / 2, 822, footerPaint);
        canvas.drawText("Generated via " + businessName + " Management System", PAGE_WIDTH / 2, 835, footerPaint);

        Paint pagePaint = new Paint();
        pagePaint.setColor(Color.WHITE);
        pagePaint.setTextSize(8);
        pagePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Page " + pageNum, PAGE_WIDTH - 25, 835, pagePaint);
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

    private static Canvas startNewPage(Context context, PdfDocument pdfDocument, int[] pageNumber, PdfDocument.Page[] currentPage, String businessName, boolean isLabourOnly, String userNumber) {
        pdfDocument.finishPage(currentPage[0]);
        pageNumber[0]++;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber[0]).create();
        currentPage[0] = pdfDocument.startPage(pageInfo);
        Canvas canvas = currentPage[0].getCanvas();
        canvas.drawColor(Color.WHITE);
        drawHeader(context, canvas, businessName, isLabourOnly);
        drawWatermark(canvas, businessName, userNumber);
        drawFooter(canvas, businessName, pageNumber[0]);
        return canvas;
    }

    private static void drawRulesHeader(Canvas canvas, int boxLeft, int boxRight, int top, Paint bgPaint, Paint borderPaint, Paint accentBarPaint, Paint textPaint, boolean isContinuation) {
        int headerHeight = 22;
        canvas.drawRect(boxLeft, top, boxRight, top + headerHeight, bgPaint);
        canvas.drawLine(boxLeft, top, boxRight, top, borderPaint);
        canvas.drawLine(boxLeft, top + headerHeight, boxRight, top + headerHeight, borderPaint);
        canvas.drawRect(boxLeft, top, boxLeft + 4, top + headerHeight, accentBarPaint);
        String headerTitle = isContinuation ? "RULES OF ENGAGEMENT & TERMS (CONTINUED)" : "RULES OF ENGAGEMENT & PROJECT TERMS";
        canvas.drawText(headerTitle, boxLeft + 12, top + 15, textPaint);
    }

    public static String sanitizeFileName(String input) {
        if (input == null) return "";
        String sanitized = input.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        sanitized = sanitized.replaceAll("_+", "_").replaceAll("^_|_$", "");
        return sanitized;
    }

    private static File createPdfFile(Context context, String prefix, Project project, String extra) {
        StringBuilder sb = new StringBuilder(prefix);
        if (project != null) {
            sb.append("_").append(project.getId());
            String projName = sanitizeFileName(project.getName());
            if (!projName.isEmpty()) {
                if (projName.length() > 20) projName = projName.substring(0, 20);
                sb.append("_").append(projName);
            }
        } else if (extra != null && !extra.trim().isEmpty()) {
            String sanitizedExtra = sanitizeFileName(extra);
            if (!sanitizedExtra.isEmpty()) {
                sb.append("_").append(sanitizedExtra);
            }
        }
        sb.append(".pdf");

        File cacheDir = context.getExternalCacheDir() != null ? context.getExternalCacheDir() : context.getCacheDir();
        return new File(cacheDir, sb.toString());
    }
}
