import java.net.InetAddress;
import java.net.Socket;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.google.gson.*;

public class ConnectionThread extends Thread {

    private InetAddress connectionAddress;
    private InputStream inputStream;
    public OutputStream outputStream;
    private boolean failedToConnect = false;
    public Global global;

    public ConnectionThread(Socket connectionSocket, Global global) {
        try {
            this.global = global;
            this.connectionAddress = connectionSocket.getInetAddress();
            this.inputStream = connectionSocket.getInputStream();
            this.outputStream = connectionSocket.getOutputStream();
        } catch(IOException e) {
            failedToConnect = true;
        }
    }

    public void run() {
        if (failedToConnect) {
            System.out.println("Failed to get input or output stream. Exiting thread");
            return;
        }
        System.out.println("New connection established with IP " + connectionAddress);

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "connectionEstablished");
        global.sendMessage(jsonToSend, outputStream);

        while (true) {
            try {
                byte[] lengthHeaderBuffer = new byte[4];
                this.inputStream.read(lengthHeaderBuffer); // reads in big endian
                int numDataBytes = ByteBuffer.wrap(lengthHeaderBuffer).getInt();

                byte[] dataBuffer = new byte[numDataBytes];
                this.inputStream.read(dataBuffer);
                String messageReceived = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(dataBuffer)).toString();

                JsonObject jsonReceived = JsonParser.parseString(messageReceived).getAsJsonObject();
                handleReceivedMessage(jsonReceived);
            } catch (IOException e) {
                System.out.println("IOException. Connection terminated with " + this.connectionAddress + ". Exiting thread.");
                return;
            } catch (IllegalStateException e) {
                System.out.println("IllegalStateException. Connection terminated with " + this.connectionAddress + ". Exiting thread.");
                return;
            }

        }
    }

    private void handleReceivedMessage(JsonObject jsonReceived) {
        if (!jsonReceived.has("instruction")) {
            System.out.println("Message received has no 'instruction' property. Cannot handle received message.");
        } else {
            String instruction = jsonReceived.get("instruction").getAsString();
            System.out.println("New message received with instruction: " + instruction);
            ClientInstructionHandler handler = new ClientInstructionHandler(jsonReceived, this);
            handler.handleInstruction();
        }
    }
}