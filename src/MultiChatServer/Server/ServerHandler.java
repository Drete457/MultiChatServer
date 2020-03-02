package MultiChatServer.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class ServerHandler implements Runnable {

    private Server server;
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;
    private boolean run = true;
    private Commands commands;
    private boolean inGroup;
    private Group groupName;

    ServerHandler(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
        try {
            in = new DataInputStream(this.clientSocket.getInputStream());
            out = new DataOutputStream(this.clientSocket.getOutputStream());
            commands = new Commands();
            inGroup = false;
            out.writeUTF("What is your nickName?");
            name = in.readUTF();
            while (server.sameNickName(name)) {
                out.writeUTF("Name already exist, choose another");
                name = in.readUTF();
            }
        } catch (IOException e) {
            commands("/Quit");
        }
    }

    @Override
    public void run() {
        while (run) {
            getMessage();
        }
    }

    private void getMessage() {
            try {
                String messageReceive;
                while (true) {
                    messageReceive = in.readUTF();
                    boolean decision = commands(messageReceive); // server can't handel this in the if
                    if (!decision) {
                        server.replyMessage(name + ": " + messageReceive);
                    }
                }
            } catch (IOException ex) {
                commands("/Quit");
            }
    }

    void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            commands("/Quit");
        }
    }

    private boolean commands(String messageReceive) {
        synchronized (this) {
            boolean decision = false;
            try {
                switch (messageReceive) {
                    case "/Quit":
                        run = false;
                        in.close();
                        out.close();
                        clientSocket.close();
                        server.closeConnection(this);
                        break;

                    default:
                        decision = commands.listen(messageReceive, in, out, name, server, this);
                    break;
                }
            } catch (IOException e) {
                System.out.println("Connection problem: " + e.getMessage() + "whit the client: " + clientSocket);
                commands("/Quit");
            }
            notifyAll();
            return decision;
        }
    }

    String getName() {
        return name;
    }

    void setName(String name){
        this.name = name;
    }

    void setInGroup (boolean createOrFinish) {
        inGroup = createOrFinish;
    }

    boolean getInGroup() {
        return inGroup;
    }

    Group getGroupName() {
        return groupName;
    }

    void setGroupName(Group group) {
        this.groupName = group;
    }

    DataOutputStream getOut() {
        return out;
    }
}
