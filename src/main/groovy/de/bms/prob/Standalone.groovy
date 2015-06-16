package de.bms.prob

import de.bms.BMotionServer
import groovy.util.logging.Slf4j

@Slf4j
public class Standalone {

    private static volatile boolean terminated = false;

    private Socket ctrlSocket;

    private PrintWriter output;

    private BufferedReader input;

    public Standalone(args) {

        // Start BMotion Server
        BMotionServer server = ProBServerFactory.getServer(args)
        server.setMode(BMotionServer.MODE_STANDALONE)
        server.start()

        // Start Heartbeat for Client
        def heartbeatport = server.cmdLine.getOptionValue("heartbeatport");
        initCtrlConnection(heartbeatport)
        startHeartBeatListener(heartbeatport)

        // Start ProB Server
        new Thread(new Runnable() {
            public void run() {
                def probargs = ["-s", "-multianimation"]
                if (server.cmdLine.hasOption("local")) {
                    probargs << "-local"
                }
                if (server.cmdLine.hasOption("probPort")) {
                    probargs << "-port"
                    probargs << server.cmdLine.getOptionValue("probPort")
                }
                de.prob.Main.main(probargs.toArray(new String[probargs.size()]))
            }
        }).start();

    }

    public static void main(final String[] args) throws InterruptedException {
        new Standalone(args);
    }

    private void startHeartBeatListener(final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.debug("Send heartbeat");
                    heartBeat();
                    log.debug("Still alive");
                }

            }
        }).start();
    }

    private void heartBeat() {
        String msg = "{\"cmd\":\"skip\"}";
        try {
            log.debug("Sending '{}'", msg);
            String response = sendToUI(msg);
            log.debug("Received '{}'", response);
        } catch (Exception e1) {
            log.error("Exception in Heartbeat:", e1);
            System.exit(-1);
        }
    }

    public synchronized String sendToUI(String msg) throws IOException {
        log.trace("send " + msg);
        output.println(msg);
        output.flush();
        String readLine = input.readLine();
        if (readLine == null) {
            throw new IOException("Response was null.");
        }
        log.trace("received " + readLine);
        return readLine;
    }

    private void initCtrlConnection(int port) {
        try {
            ctrlSocket = new Socket("127.0.0.1", port);
            output = new PrintWriter(ctrlSocket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(
                    ctrlSocket.getInputStream()));

        } catch (UnknownHostException e) {
            e.printStackTrace(); // 127.0.0.1 should be present
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
