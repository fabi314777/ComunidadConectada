import com.comunidadconectada.app.alert.AlertCodec;
import com.comunidadconectada.app.alert.AlertPolicy;
import com.comunidadconectada.app.model.CommunityAlert;
import com.comunidadconectada.app.util.DuplicateDetector;
import java.io.*;

/** Lee payloads MQTT (uno por línea) y aplica las clases REALES de la app: AlertCodec + AlertPolicy. */
public class PolicyHarness {
    public static void main(String[] args) throws Exception {
        DuplicateDetector det = new DuplicateDetector(200);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            CommunityAlert a = AlertCodec.fromJson(line);
            AlertPolicy.Decision d = AlertPolicy.evaluate(a, "comunidad01", det, System.currentTimeMillis(), 300000L);
            System.out.println(d + (a != null ? "|" + a.message() : ""));
        }
    }
}
