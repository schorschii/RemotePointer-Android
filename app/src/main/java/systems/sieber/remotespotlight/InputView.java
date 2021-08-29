package systems.sieber.remotespotlight;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class InputView extends View {

    public InputView(Context context) {
        super(context);
        commonInit();
    }
    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        commonInit();
    }
    private void commonInit() {
        setFocusableInTouchMode(true);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        BaseInputConnection fic = new BaseInputConnection(this, false);
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_NULL;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NEXT;
        return fic;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

}
