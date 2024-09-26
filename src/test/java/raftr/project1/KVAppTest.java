package raftr.project1;

import org.junit.jupiter.api.Test;
import raftr.project1.KVServer.KVApp;

import static org.assertj.core.api.Assertions.assertThat;

class KVAppTest {

    KVApp kv = new KVApp();

    @Test void crud() {
        assertThat(kv.runCommand("set x 2")).isEqualTo("ok");
        assertThat(kv.runCommand("get x")).isEqualTo("2");
        assertThat(kv.runCommand("delete x")).isEqualTo("ok");
        assertThat(kv.runCommand("get x")).isEqualTo("");
    }

    @Test void snapshot_restore() {
        assertThat(kv.runCommand("set x old")).isEqualTo("ok");
        assertThat(kv.runCommand("set y 3")).isEqualTo("ok");
        assertThat(kv.runCommand("snapshot kv-snapshot")).isEqualTo("ok");

        assertThat(kv.runCommand("set z 4")).isEqualTo("ok");
        assertThat(kv.runCommand("get z")).isEqualTo("4");
        assertThat(kv.runCommand("set x new")).isEqualTo("ok");
        assertThat(kv.runCommand("get x")).isEqualTo("new");

        assertThat(kv.runCommand("restore kv-snapshot")).isEqualTo("ok");
        assertThat(kv.runCommand("get z")).isEqualTo("");
        assertThat(kv.runCommand("get x")).isEqualTo("old");
    }
}