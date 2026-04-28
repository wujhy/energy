import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class DioController {

    private static final String GPIO = "42";
    private static final String DIR = "/sys/class/gpio/gpio" + GPIO + "/direction";
    private static final String VAL = "/sys/class/gpio/gpio" + GPIO + "/value";

    private static void log(String msg) {
        System.out.println(LocalDateTime.now() + " [GPIO" + GPIO + "] " + msg);
    }

    private static void write(String path, String value) throws IOException {
        Files.write(Paths.get(path), value.getBytes());
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path))).trim();
    }

    public static void setOutput() throws Exception {
        write(DIR, "out");
        log("Set as OUTPUT");
    }

    public static void setInput() throws Exception {
        write(DIR, "in");
        log("Set as INPUT");
    }

    public static void on() throws Exception {
        write(VAL, "1");
        log("OUTPUT = HIGH (1)");
    }

    public static void off() throws Exception {
        write(VAL, "0");
        log("OUTPUT = LOW (0)");
    }

    public static void readValue() throws Exception {
        String value = read(VAL);
        log("INPUT value = " + value);
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            log("Usage: java -jar dio.jar [in|out|on|off|read]");
            return;
        }

        switch (args[0]) {
            case "in":
                setInput();
                break;

            case "out":
                setOutput();
                break;

            case "on":
                setOutput();
                on();
                break;

            case "off":
                setOutput();
                off();
                break;

            case "read":
                setInput();
                readValue();
                break;

            default:
                log("Unknown command");
        }
    }
}