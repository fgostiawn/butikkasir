package com.example.butikkasir.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;

public class PrintUtils {

    /** Scale bitmap to fit A4-width, then send to PrintManager. */
    public static void printBitmap(Context ctx, Bitmap bmp, String jobName) {
        final int W = 595, MARGIN = 36;

        float scale = (float) (W - 2 * MARGIN) / bmp.getWidth();
        int sw = Math.round(bmp.getWidth()  * scale);
        int sh = Math.round(bmp.getHeight() * scale);
        int ph = sh + 2 * MARGIN;

        PdfDocument pdf = new PdfDocument();
        PdfDocument.Page page = pdf.startPage(
                new PdfDocument.PageInfo.Builder(W, ph, 1).create());

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        page.getCanvas().drawBitmap(
                bmp,
                new Rect(0, 0, bmp.getWidth(), bmp.getHeight()),
                new RectF(MARGIN, MARGIN, MARGIN + sw, MARGIN + sh),
                paint);

        pdf.finishPage(page);
        sendToPrinter(ctx, pdf, jobName);
    }

    /** Send an already-built PdfDocument to PrintManager. */
    public static void printPdf(Context ctx, PdfDocument pdf, String jobName) {
        sendToPrinter(ctx, pdf, jobName);
    }

    private static void sendToPrinter(Context ctx, PdfDocument pdf, String jobName) {
        PrintManager pm = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        if (pm == null) {
            Toast.makeText(ctx, "Printer tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show();
            return;
        }

        pm.print(jobName, new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs,
                                 CancellationSignal cancel,
                                 LayoutResultCallback cb, Bundle extras) {
                if (cancel.isCanceled()) { cb.onLayoutCancelled(); return; }
                cb.onLayoutFinished(
                        new PrintDocumentInfo.Builder(jobName)
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                                .build(),
                        !newAttrs.equals(oldAttrs));
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor dest,
                               CancellationSignal cancel, WriteResultCallback cb) {
                try (FileOutputStream fos = new FileOutputStream(dest.getFileDescriptor())) {
                    pdf.writeTo(fos);
                    cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (IOException e) {
                    cb.onWriteFailed(e.getMessage());
                } finally {
                    pdf.close();
                }
            }
        }, null);  // null = use print dialog defaults (user picks paper size / printer)
    }
}
