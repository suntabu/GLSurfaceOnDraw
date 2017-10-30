package test.nexa.com.test;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Created by gouzhun on 17/10/30.
 */

public class CustomTextView extends View {
    public CustomTextView(Context context) {
        super(context);
    }

    public CustomTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        CharSequence lable = "^";
        Paint paint = new Paint();
        paint.setColor(Color.CYAN);
        paint.setTextSize(40);
        canvas.drawText(lable,0,lable.length(),0,0,paint);
    }
}
