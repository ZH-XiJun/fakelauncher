package com.wtbruh.fakelauncher.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * 程序绘制的功能机电池图标，严格复刻原 ic_battery_* 的位置与外框：
 * 45x27 横条圆角外框 + 右侧白色电极帽 + 1-4 个竖格。
 * 仅把格子填充改为绿色/橙色/红色渐变 + 高光（立体感），
 * 并保持原档位配色规则：3-4 格绿、2 格橙、1 格红、0 格红框红帽。
 */
public class BatteryDrawable extends Drawable {

    private static final int W = 54;   // 外框 45 + 电极帽 9，与原 layer-list 实际占宽一致（决定摆放位置）
    private static final int H = 27;

    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int level = 4;

    public BatteryDrawable(int level) {
        setBatteryLevel(level);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2f);
        outlinePaint.setColor(Color.WHITE);
        capPaint.setStyle(Paint.Style.FILL);
        capPaint.setColor(Color.WHITE);
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setColor(0x66FFFFFF);
    }

    public void setBatteryLevel(int level) {
        this.level = Math.max(0, Math.min(4, level));
        invalidateSelf();
    }

    /** 格子渐变颜色：3-4 格绿 / 2 格橙 / 1 格红（0 格只画红框红帽）。 */
    private int[] colorsForLevel() {
        if (level <= 0) return new int[]{0xFFFF8A80, 0xFFC62828};
        if (level == 1) return new int[]{0xFFFF8A80, 0xFFC62828};   // 红
        if (level == 2) return new int[]{0xFFFFB74D, 0xFFE65100};   // 橙
        return new int[]{0xFF8FE38F, 0xFF1E7A2E};                   // 绿
    }

    @Override
    public void draw(Canvas canvas) {
        // 关键：平移画布到 bounds 左上角（与系统 drawable 行为一致），
        // 否则自定义绘制会画在宿主 View 的 (0,0) 处。
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);

        // 阴影层：整个图标向右下偏移 2px 画一遍半透明黑
        canvas.save();
        canvas.translate(2f, 2f);
        drawBody(canvas, true);
        canvas.restore();

        // 正常图标
        drawBody(canvas, false);

        canvas.restoreToCount(canvas.save() - 1);
    }

    /** 主体绘制。shadow=true 时全部用半透明黑填充（阴影层）。 */
    private void drawBody(Canvas canvas, boolean shadow) {
        if (shadow) {
            Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
            sp.setColor(0x55000000); // 约 33% 黑
            sp.setStyle(Paint.Style.FILL);
            RectF body = new RectF(0f, 0f, 45f, 27f);
            canvas.drawRoundRect(body, 2f, 2f, sp);
            RectF cap = new RectF(45f, 6f, 54f, 21f);
            canvas.drawRoundRect(cap, 2f, 2f, sp);
            if (level > 0) {
                for (int i = 0; i < level; i++) {
                    float l = 6f + 9f * i;
                    canvas.drawRect(new RectF(l, 6f, l + 6f, 21f), sp);
                }
            }
            return;
        }

        // 外框：45x27 圆角矩形（0 格时红色描边，其余白色）；帽在 45-54 右侧
        RectF body = new RectF(0f, 0f, 45f, 27f);
        if (level == 0) outlinePaint.setColor(0xFFF44336);
        canvas.drawRoundRect(body, 2f, 2f, outlinePaint);

        // 电极帽：右侧 (45,6)-(54,21)，圆角 2
        if (level == 0) capPaint.setColor(0xFFF44336);
        RectF cap = new RectF(45f, 6f, 54f, 21f);
        canvas.drawRoundRect(cap, 2f, 2f, capPaint);

        if (level <= 0) return;

        // 竖格：每个 6x15，位置 left=6+9*i, top=6（与原图一致）
        int[] cols = colorsForLevel();
        for (int i = 0; i < level; i++) {
            float l = 6f + 9f * i;
            RectF cell = new RectF(l, 6f, l + 6f, 21f);
            // 垂直渐变：上浅下深
            fillPaint.setShader(new LinearGradient(
                    cell.left, cell.top, cell.left, cell.bottom,
                    cols[0], cols[1], Shader.TileMode.CLAMP));
            canvas.drawRect(cell, fillPaint);
            fillPaint.setShader(null);
            // 顶部高光（立体感）
            RectF hl = new RectF(l + 0.5f, 7f, l + 5.5f, 9f);
            canvas.drawRect(hl, highlightPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) { }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return W;
    }

    @Override
    public int getIntrinsicHeight() {
        return H;
    }
}
