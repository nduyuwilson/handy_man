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

    public static File generateInvoice(Context context, Project project, List<ProjectItem> projectItems, Map<Integer, Item> itemMap) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        
        SharedPreferences prefs = context.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
        String userName = prefs.getString("user_name", "Thitima Professional");
        String userNumber = prefs.getString("user_number", "");
        String paymentDetails = prefs.getString("payment_details", "N/A");
        String currency = Formatter.getCurrencySymbol(context);

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Watermark - Fixed: Paint doesn't have setRotation, use Canvas.rotate instead
        Paint watermarkPaint = new Paint();
        watermarkPaint.setColor(Color.LTGRAY);
        watermarkPaint.setAlpha(30);
        watermarkPaint.setTextSize(60);
        watermarkPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.save();
        canvas.rotate(-45, 297, 421); // Rotate -45 degrees around the center of the A4 page (595/2, 842/2)
        canvas.drawText(userName, 50, 421, watermarkPaint);
        canvas.restore();

        int x = 40;
        int y = 50;

        // Draw Icon
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.elec_man);
        if (drawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
            Canvas bitmapCanvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, bitmapCanvas.getWidth(), bitmapCanvas.getHeight());
            drawable.draw(bitmapCanvas);
            canvas.drawBitmap(bitmap, x, y - 25, null);
            x += 50;
        }

        // Header
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.BLUE);
        canvas.drawText("THITIMA ELECTRICALS", x, y, titlePaint);
        
        x = 40; // Reset x
        y += 30;
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        canvas.drawText("Issued by: " + userName + " (" + userNumber + ")", x, y, paint);

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Project: " + project.getName(), x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Location: " + project.getLocation(), x, y, paint);
        y += 20;
        canvas.drawText("Date: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), x, y, paint);

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Client Information:", x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Name: " + project.getClientName(), x, y, paint);
        y += 15;
        canvas.drawText("Contact: " + project.getClientContact(), x, y, paint);

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Item Name", x, y, paint);
        canvas.drawText("Qty", x + 280, y, paint);
        canvas.drawText("Unit Price (" + currency + ")", x + 330, y, paint);
        canvas.drawText("Total (" + currency + ")", x + 450, y, paint);
        
        y += 10;
        canvas.drawLine(x, y, 550, y, paint);
        y += 20;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        double materialTotal = 0;
        if (projectItems != null) {
            for (ProjectItem pi : projectItems) {
                Item item = itemMap.get(pi.getItemId());
                String name = item != null ? item.getName() : "Unknown Item";
                double total = pi.getQuantity() * pi.getQuotedPrice();
                materialTotal += total;

                canvas.drawText(name, x, y, paint);
                canvas.drawText(String.valueOf(pi.getQuantity()), x + 280, y, paint);
                canvas.drawText(Formatter.formatNumber(pi.getQuotedPrice()), x + 330, y, paint);
                canvas.drawText(Formatter.formatNumber(total), x + 450, y, paint);
                
                y += 20;
                if (y > 650) break; 
            }
        }

        y += 20;
        canvas.drawLine(x, y, 550, y, paint);
        y += 30;

        // Calculate Labour based on % if available, otherwise fixed
        double labourCost = project.getLabourCost();
        if (project.getLabourPercentage() > 0) {
            labourCost = (project.getLabourPercentage() / 100.0) * materialTotal;
        }

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Material Total (" + currency + "):", x + 280, y, paint);
        canvas.drawText(Formatter.formatNumber(materialTotal), x + 450, y, paint);
        
        y += 20;
        canvas.drawText("Labour Cost (" + (project.getLabourPercentage() > 0 ? project.getLabourPercentage() + "%" : "Fixed") + "):", x + 280, y, paint);
        canvas.drawText(Formatter.formatNumber(labourCost), x + 450, y, paint);
        
        y += 30;
        paint.setTextSize(16);
        canvas.drawText("Grand Total (" + currency + "):", x + 280, y, paint);
        canvas.drawText(Formatter.formatNumber(materialTotal + labourCost), x + 450, y, paint);

        y += 40;
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Payment Options:", x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(paymentDetails, x, y, paint);

        y += 40;
        canvas.drawText("Rules of Engagement:", x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        String rules = project.getRulesOfEngagement() != null ? project.getRulesOfEngagement() : "N/A";
        if (rules.length() > 80) {
             canvas.drawText(rules.substring(0, 80), x, y, paint);
             y += 15;
             canvas.drawText(rules.substring(80), x, y, paint);
        } else {
             canvas.drawText(rules, x, y, paint);
        }

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
