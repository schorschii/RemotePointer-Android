package systems.sieber.remotespotlight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class DemoView extends View {
    public DemoView(Context context) {
        super(context);
    }
    public DemoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public DemoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private RectF bounds = new RectF();
    private Paint paint = new Paint();
    float x = 500;
    float y = 500;

    @Override
    protected void onDraw(Canvas canvas) {
        // draw items
        paint.setStyle(Paint.Style.FILL);
        bounds.set((float) x, (float) y, (float) x + 15, (float) y + 15);
        paint.setARGB(0xff, 200, 0, 0);
        canvas.drawRect(bounds, paint);
    }

    public void update(float dx, float dy) {
        x += dx;
        y += dy;
        invalidate();
    }
}
