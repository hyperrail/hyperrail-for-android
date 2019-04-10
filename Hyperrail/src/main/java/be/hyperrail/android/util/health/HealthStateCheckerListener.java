package be.hyperrail.android.util.health;

public interface HealthStateCheckerListener {
    void onSystemHealthChanged(HealthState currentState);
}
