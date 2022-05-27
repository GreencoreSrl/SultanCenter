package com.ncr.ssco.communication;

import com.ncr.ssco.communication.manager.SscoMessageHandler;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * Created by stefanobertarello on 28/02/17.
 */
public class SscoGSPSocketServer implements Runnable {
    private ServerSocket listener;
    private int port = 6696;
    private SscoMessageHandler handler = null;
    private boolean interrupted;
    private static final Logger logger = Logger.getLogger(SscoGSPSocketServer.class);
    private boolean canAccept = true;
    private String encoding = "UTF-8";

    public SscoGSPSocketServer(int port, SscoMessageHandler handler, String encoding) {
        try {
            this.port = port;
            this.handler = handler;
            this.encoding = encoding;
            listener = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        DataInputStream input = null;
        OutputStream output = null;

        try {
            Socket serviceSocket = listener.accept();
            serviceSocket.setSoTimeout(100);
            input = new DataInputStream(serviceSocket.getInputStream());
            output = serviceSocket.getOutputStream();

            while (!interrupted) {
                if (canAccept) {
                    try {
                        int messageLength = input.readInt();
                        if (messageLength > 0) {
                            logger.info("Length of Message: " + messageLength);
                            byte[] buffer = new byte[4096];
                            int len = 0;
                            do {
                                len = input.read(buffer, len, messageLength);
                            } while (len < messageLength);
                            String message = new String(buffer, 0, len);
                            logger.info("Message: " + message);
                            handler.handleMessage(message);
                        }
                    } catch (SocketTimeoutException e) {
                        canAccept = true;
                    }
                } else {
                    Thread.sleep(250);
                }

                Vector<String> responses = handler.peekResponses();
                if (responses.size() > 0) {
                    if (responses.get(0).equals("VOID")) {
                        logger.info("------------------------");
                        logger.info("------------------------");
                        logger.info("----------VOID Detected-");
                        logger.info("------------------------");
                        logger.info("------------------------");
                        canAccept = true;
                        continue;
                    }
                    for (int k = 0; k < responses.size(); k++) {
                        String response = responses.get(k);
                        if (!response.equals("EndProcess")) {
                            byte[] outMessage = response.getBytes(encoding);
                            byte[] size = ByteBuffer.allocate(4).putInt(outMessage.length).array();
                            output.write(size);
                            output.write(outMessage);
                            logger.info("mando outMessage: " + new String(outMessage));
                        }
                    }

                    canAccept = true;
                }
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
        } finally {
            try {
                input.close();
                output.close();
                listener.close();
            } catch (Exception fe) {
                fe.printStackTrace();
            }
        }
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public SscoMessageHandler getHandler() {
        return handler;
    }

    public void setHandler(SscoMessageHandler handler) {
        this.handler = handler;
    }

    public boolean isCanAccept() {
        return canAccept;
    }

    public void setCanAccept(boolean canAccept) {
        this.canAccept = canAccept;
    }
}
