package com.example.butikkasir.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    public static class Entry {
        public final String label;
        public final float value;
        public Entry(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }

    private List<Entry> entries = new ArrayList<>();
    private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context) { this(context, null); }
    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        barPaint.setColor(Color.parseColor("#E91E63"));
        barPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(Color.parseColor("#9E9E9E"));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setColor(Color.parseColor("#E91E63"));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);

        gridPaint.setColor(Color.parseColor("#F0F0F0"));
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        emptyPaint.setColor(Color.parseColor("#BDBDBD"));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Entry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        if (entries.isEmpty()) {
            emptyPaint.setTextSize(dp(14));
            canvas.drawText("Belum ada data untuk ditampilkan", w / 2f, h / 2f, emptyPaint);
            return;
        }

        float padL = dp(8), padR = dp(8), padTop = dp(24), padBot = dp(28);
        float chartH = h - padTop - padBot;
        float chartW = w - padL - padR;
        float chartBottom = h - padBot;

        float maxVal = 0;
        for (Entry e : entries) if (e.value > maxVal) maxVal = e.value;
        if (maxVal == 0) maxVal = 1;

        // Grid lines (3 levels)
        gridPaint.setColor(Color.parseColor("#F0F0F0"));
        for (int i = 1; i <= 3; i++) {
            float gy = chartBottom - chartH * i / 3f;
            canvas.drawLine(padL, gy, w - padR, gy, gridPaint);
        }
        canvas.drawLine(padL, chartBottom, w - padR, chartBottom, gridPaint);

        int n = entries.size();
        float slotW = chartW / n;
        float barW  = Math.min(slotW * 0.55f, dp(32));
        float textSize = Math.max(dp(7), Math.min(dp(9), slotW * 0.5f));
        labelPaint.setTextSize(textSize);
        valuePaint.setTextSize(textSize);

        for (int i = 0; i < n; i++) {
            Entry e = entries.get(i);
            float cx = padL + slotW * i + slotW / 2f;
            float left  = cx - barW / 2f;
            float right = cx + barW / 2f;
            float barH  = chartH * (e.value / maxVal);
            float top   = chartBottom - barH;

            // Alternating opacity for readability
            barPaint.setAlpha(i % 2 == 0 ? 255 : 210);
            RectF rect = new RectF(left, top, right, chartBottom);
            canvas.drawRoundRect(rect, dp(3), dp(3), barPaint);

            // Value label above bar
            if (barH > dp(16)) {
                valuePaint.setAlpha(255);
                canvas.drawText(fmtVal(e.value), cx, top - dp(3), valuePaint);
            }

            // Date label below x-axis
            canvas.drawText(e.label, cx, h - dp(6), labelPaint);
        }
    }

    private String fmtVal(float v) {
        if (v >= 1_000_000) return String.format("%.1fJt", v / 1_000_000f);
        if (v >= 1_000)     return String.format("%.0fK",  v / 1_000f);
        return String.format("%.0f", v);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
