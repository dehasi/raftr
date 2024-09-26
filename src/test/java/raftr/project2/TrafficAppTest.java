package raftr.project2;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import raftr.project2.Traffic.TrafficApp;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficAppTest {

    TrafficApp trafficApp = new TrafficApp();

    @Disabled("Switched completely to events")
    @Test void switches_state_full_circle() {
        assertThat(trafficApp.changeLights()).containsExactly('G', 'R'); // NS, EW

        for (int i = 0; i < 29; i++) trafficApp.changeLights();
        assertThat(trafficApp.changeLights()).containsExactly('Y', 'R'); // NS, EW

        for (int i = 0; i < 4; i++) trafficApp.changeLights();
        assertThat(trafficApp.changeLights()).containsExactly('R', 'G'); // NS, EW

        for (int i = 0; i < 59; i++) trafficApp.changeLights();
        assertThat(trafficApp.changeLights()).containsExactly('R', 'Y'); // NS, EW

        for (int i = 0; i < 4; i++) trafficApp.changeLights();
        assertThat(trafficApp.changeLights()).containsExactly('G', 'R'); // NS, EW
    }
}