package daslab.com.fieldmonitoringv2;

import android.content.Context;
import android.graphics.Bitmap;

public class ContextAndBitmap {
    Bitmap bitmap;
    Context context;

    /**
     * A wrapper data structure to pass through multiple things to a async task
     * @param bitmap The image that will be processed
     * @param context The current application context in order to know where to go back to.
     */
    public ContextAndBitmap(Bitmap bitmap, Context context){
        this.bitmap = bitmap;
        this.context = context;
    }

}
