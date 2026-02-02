package be.jeromebertaux.pitrmnl;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "pi-trmnl", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Fetches PADD data from a Pi-hole server and publishes it to a TRMNL plugin.")
public class Main implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-e", "--pihole-endpoint"}, required = true, 
            description = "The endpoint of your Pi-hole server")
    private String piholeEndpoint;

    @Option(names = {"-p", "--pihole-password"}, required = true, 
            description = "The application password of your Pi-hole server")
    private String piholePassword;

    @Option(names = {"-t", "--trmnl-plugin"}, required = true, 
            description = "The plugin UUID of your TRMNL plugin")
    private String trmnlPlugin;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        logger.info("★ Pi-Trmnl");

        try {
            PiHoleClient client = new PiHoleClient(piholeEndpoint, piholePassword);
            TrmnlClient trmnlClient = new TrmnlClient(trmnlPlugin);

            JSONObject processedData = DataProcessor.processPadd(client.getPaddData());
            JSONObject historyData = DataProcessor.processHistory(client.getHistory());

            // Merge data
            JSONObject dataToSend = new JSONObject(processedData.toString());
            for (String key : historyData.keySet()) {
                dataToSend.put(key, historyData.get(key));
            }

            trmnlClient.sendData(dataToSend);

            return 0;
        } catch (Exception e) {
            logger.error("Error in execution", e);
            return 1;
        }
    }
}