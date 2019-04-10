package be.hyperrail.android.util.health;

import org.json.JSONException;
import org.json.JSONObject;

public class HealthState {
    private boolean healthy = true;
    private String nl;
    private String fr;
    private String en;

    HealthState(JSONObject json) {
        try {
            healthy = json.getBoolean("healthy");
            nl = json.getString("nl");
            fr = json.getString("fr");
            en = json.getString("en");
        } catch (JSONException e) {
            // Ignored
        }
    }

    public boolean isHealthy() {
        return healthy;
    }

    public String getEn() {
        return en;
    }

    public String getFr() {
        return fr;
    }

    public String getNl() {
        return nl;
    }
}