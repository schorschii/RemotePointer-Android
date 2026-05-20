package systems.sieber.remotespotlight;

import android.content.Context;

public class FeatureCheck extends BaseFeatureCheck {

    FeatureCheck(Context c) {
        super(c);
    }

    @Override
    void init() {
        super.init();

        // no additional purchase store checks
        isReady = true;
        if(listener != null) {
            listener.featureCheckReady(true);
        }
    }

}

