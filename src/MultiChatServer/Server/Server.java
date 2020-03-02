package MultiChatServer.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Server {

    private Set<ServerHandler> clients;
    private Set<Group> groups;
    private ExecutorService connection;
    private ExecutorService groupConnection;
    private ServerSocket serverSocket;

    private Server() {
        listen();
    }

    public static void main(String[] args) {
        new Server();
    }

    private void listen() {
        int portNumber;
        startServer();

        Scanner listen = new Scanner(System.in);
        System.out.print("What port the server will listen? Default Port is 5000 ");
        //portNumber = listen.nextInt();
        portNumber = 5000;
        try {
            serverSocket = new ServerSocket(portNumber);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connect: " + clientSocket);
                addClient(clientSocket);
                System.out.println("The server have " + clients.size() + " connected.");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("You need to write a port, between 1 and 65536");
        }
    }

    private void startServer() {
        clients = Collections.synchronizedSet(new HashSet());
        groups = Collections.synchronizedSet(new HashSet());
        connection = Executors.newFixedThreadPool(100);
        groupConnection = Executors.newCachedThreadPool();
        groups.add(new Group("null", "null"));
    }

    private void addClient(Socket clientSocket) {
        ServerHandler newClient = new ServerHandler(this, clientSocket);
        connection.submit(newClient);
        clients.add(newClient);
    }

    boolean sameNickName(String name) {
        synchronized (clients) {
            for (ServerHandler verify : clients) {
                String nickName = verify.getName();
                if (name.equals(nickName)) {
                    return true;
                }
            }
            return false;
        }
    }

    Set getGroup() {
        return groups;
    }

    ExecutorService getGroupConnection() {
        return groupConnection;
    }

    void replyMessage(String message) {
        try {
            synchronized (clients) {
                for (ServerHandler send : clients) {
                    send.sendMessage(message);
                }

            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Null sender don't exist");
        }
    }

    void listOnline(DataOutputStream out) {
        synchronized (clients) {
            try {
                out.writeUTF("User Online: ");
                for (ServerHandler send : clients) {
                    out.writeUTF(send.getName());
                }
            } catch (IOException e) {
                System.out.println("Not possible to send user's online list");
            }
        }
    }

    void listGroups(DataOutputStream out) {
        try {
            StringBuilder list = new StringBuilder();
            for ( Group groupsList : groups) {
                if (!groupsList.getName().equals("null")) {
                    list.append(" ").append(groupsList.getName() + ",");
                }
            }
            out.writeUTF("Groups: " + list);
        } catch (IOException e) {
            System.out.println("Impossible to send the Group List request");
          }
    }

    boolean verificationAndSend(String[] nickname, String name) {
        synchronized (clients) {
            for (ServerHandler verify : clients) {
                String getName = verify.getName();
                if (nickname[1].equals(getName)) {
                    StringBuilder message = new StringBuilder();
                    for (int i = 2; i < nickname.length; i++) {
                        message.append(" ").append(nickname[i]);
                    }
                    verify.sendMessage("Private msg - " + name + ": " + message);
                    return true;
                }
            }
            return false;
        }
    }

    void setInGroup(Group groupName) {
        for (ServerHandler group : clients) {
              if (group.getGroupName().equals(groupName)){
                  group.setInGroup(false);
                  groups.remove(groupName);
              }
        }
    }

    void kickGroup(String kickMember) {
        for (ServerHandler kick : clients) {
            String name = kick.getName();
            if (name.equals(kickMember)) {
                kick.setInGroup(false);
            }
        }
    }

    DataOutputStream getOut(String nickName) {
        DataOutputStream out = null;
        for (ServerHandler getOut : clients){
            if (getOut.getName().equals(nickName)){
                getOut.getOut();
                break;
            }
        }
        return out;
    }

    void closeConnection(ServerHandler connects) {
        synchronized (clients) {
            clients.remove(connects);
            replyMessage("The client: " + connects.getName() + " have close the connection");
            System.out.println("Client close, still have: " + clients.size() + " Connected");
        }
    }
}


