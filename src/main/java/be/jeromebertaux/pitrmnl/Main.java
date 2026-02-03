package be.jeromebertaux.pitrmnl;

import be.jeromebertaux.pitrmnl.client.PiHoleClient;
import be.jeromebertaux.pitrmnl.client.TrmnlClient;
import be.jeromebertaux.pitrmnl.type.HistoryData;
import be.jeromebertaux.pitrmnl.type.PaddData;
import be.jeromebertaux.pitrmnl.type.ScreenVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "pi-trmnl", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Fetches PADD data from a Pi-hole server and publishes it to a TRMNL plugin.")
public class Main implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-e", "--pihole-endpoint"}, required = true, 
            description = "The endpoint of your Pi-hole server")
    private String piholeEndpoint;

    @Option(names = {"-p", "--pihole-password"}, required = true, 
            description = "The application password of your Pi-hole server")
    private String piholePassword;

    @Option(names = {"-t", "--trmnl-plugin"}, required = true, 
            description = "The plugin UUID of your TRMNL plugin")
    private String trmnlPlugin;

    static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        LOGGER.info("★ Pi-Trmnl");

        try {
            PiHoleClient client = new PiHoleClient(piholeEndpoint, piholePassword);
            TrmnlClient trmnlClient = new TrmnlClient(trmnlPlugin);

            final PaddData paddData = client.getPaddData();
            final HistoryData history = client.getHistory();

            final ScreenVariables screenVariables = new ScreenVariables(paddData, history);
            trmnlClient.sendData(screenVariables);

            return 0;
        } catch (Exception e) {
            LOGGER.error("Error in execution", e);
            return 1;
        }
    }
}