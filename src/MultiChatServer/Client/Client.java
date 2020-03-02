package MultiChatServer.Client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private Socket socketClient;
    private DataInputStream in;
    private DataOutputStream out;
    private static Scanner keyboard = new Scanner(System.in);
    private ExecutorService threads;

    private Client(InetAddress IP, int portNumber) throws IOException {
        socketClient = new Socket(IP, portNumber);
        in = new DataInputStream(socketClient.getInputStream());
        out = new DataOutputStream(socketClient.getOutputStream());
        start();
    }

    public static void main(String[] args) {

        try {
            InetAddress IP;

            System.out.print("What is the servitor address? ");
            //IP = InetAddress.getByName(keyboard.next());
            IP = InetAddress.getByName("127.0.0.1");
            System.out.print("What is the port? ");
            //int portNumber = keyboard.nextInt();

           int portNumber = 5000;

            new Client(IP, portNumber);

        } catch (UnknownHostException e) {
            System.out.println("Host don't found");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void start() {
        threads = Executors.newFixedThreadPool(2);
        threads.submit(new Receive());
        threads.submit(new Send());
    }

    private boolean commands(String message) {
        synchronized (this) {
            Boolean decision = false;
            try {
                String[] messageVerification = message.split(" ");
                switch (messageVerification[0]) {
                    case "/Quit":
                        out.writeUTF("/Quit");
                        System.out.println("You have quit");
                        socketClient.close();
                        System.exit(1);
                        break;

                    case "/File":
                        out.writeUTF(message);
                        sendFile(messageVerification[2]);
                        decision = true;
                        break;

                    default:
                        out.writeUTF(message);
                        decision = true;
                        break;
                }
            } catch (IOException e) {
                commands("/Quit");
            }
            return decision;
        }
    }

    private void sendFile(String path) {
        try {
            byte[] buffer = new byte[2048];
            File file = new File(path);
            FileInputStream readFile = new FileInputStream(file);
            int fileLenght;
            while ((fileLenght = readFile.read(buffer)) > 0){
                out.write(buffer, 0, fileLenght);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException ex) {
            System.out.println("Impossible to send the file");
        }
    }

    private class Receive implements Runnable {

        @Override
        public void run() {
            String receive = "";
            try {
                while (true) {
                    receive = in.readUTF();
                    if (receive.equals("/File")) {
                        receiveFile();
                    }
                    else if (receive != null) {
                        System.out.println(receive);
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        private void receiveFile() {
            try {
                byte[] buffer = new byte[2048];
                FileOutputStream write = new FileOutputStream("/");
                int fileLength;
                while ((fileLength = in.read(buffer)) > 0) {
                        write.write(buffer, 0, fileLength);
                }
                System.out.println("File received");
                write.close();
            } catch (FileNotFoundException e) {
                System.out.println("Not possible to save the file.");
            } catch (IOException ex) {
                System.out.println("Not possible to read da DataStream");
            }
        }
    }

    private class Send implements Runnable {

        @Override
        public void run() {
            String message;
            try {
                while (true) {
                    message = keyboard.nextLine();
                    if (!commands(message) && !message.isEmpty()) {
                        commands(message);
                        out.writeUTF(message);
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
