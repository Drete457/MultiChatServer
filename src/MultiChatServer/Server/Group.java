package MultiChatServer.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

class Group implements Runnable{

    private String groupName;
    private String adminName;
    private HashMap<String, DataOutputStream> members = new HashMap<>();
    private Boolean run = true;

    Group(String groupName, String name) {
        this.groupName = groupName;
        this.adminName = name;
    }

    @Override
    public void run() {
        while (run) {
            notifyAll();
        }
    }

    String getName() {
        return groupName;
    }

    void updateMemberName(String oldName, String newName, DataOutputStream out) {
        members.remove(oldName);
        joinGroup(newName, out);
    }

    String membersList() {
        StringBuilder list = new StringBuilder();
        list.append("Member: " + members.keySet());
        return list.toString();
    }

    void receiveMessage(String message) {
    sendMessage(message);
    }

    void sendMessage(String message) {
        try {
            for (DataOutputStream send : members.values()) {
                send.writeUTF(message);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    void joinGroup(String name, DataOutputStream out) {
            for (String membersVerification : members.keySet()) {
                if (membersVerification.equals(name)) {
                    break;
                }
            }
            members.put(name, out);
            sendMessage("Member: " + name + " have enter the group.");
    }

    void kickMember(String name, String kickMember, Server server, DataOutputStream out) throws IOException {
        if (name.equals(adminName)) {
            if (members.containsKey(kickMember) == true && (kickMember != adminName)) {
                members.remove(kickMember);
                server.kickGroup(kickMember);
                sendMessage("Member: " + kickMember + " was expelled from the group");
            } else {
                out.writeUTF("Member don't exist or you can't kick yourself");
            }
        } else {
            sendMessage("Member: " + name + " try to expelled the member: " + kickMember);
        }
    }

    void exitTheGroup(String name, ServerHandler client) throws IOException {
        if (name.equals(adminName)){
            members.get(adminName).writeUTF("You can't exit the group, only close");
        } else {
            sendMessage("Member: " + name + " exit the group");
            members.remove(name);
            client.setInGroup(false);
        }
;    }

    void closeTheGroup(String name, Server server) {
        if (name.equals(adminName)) {
            sendMessage("The group was close");
            run = false;
            server.setInGroup(this);
            members.clear();

        } else {
            sendMessage("The member: " + name + " tried to close the group, kick him");
        }
    }
}