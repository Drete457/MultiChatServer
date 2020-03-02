package MultiChatServer.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

class Commands {

    Boolean listen(String command, DataInputStream in, DataOutputStream out, String name, Server server,
                     ServerHandler client)
    throws IOException {
        boolean decision = false;
        boolean inGroup = client.getInGroup(); //if can't handle the call of the method
        String[] commandSplit = command.split(" ");
        switch (commandSplit[0]) {
            case "/Alias":
                if (server.sameNickName(commandSplit[1])) {
                    out.writeUTF("Name already exist, choose another");
                    commandSplit[1] = in.readUTF();
                }
                client.setName(commandSplit[1]);
                if (inGroup) {
                    Group currentGroup = client.getGroupName();
                    currentGroup.updateMemberName(name, commandSplit[1], out);
                }
                out.writeUTF("Your new nickName is: " + commandSplit[1]);
                server.replyMessage("The user: " + name + " have change is nickname for: " + commandSplit[1]);
                decision = true;
                break;

            case "/Help":
                help(out);
                decision = true;
                break;

            case "/List":
                list(server, out);
                decision = true;
                break;

            case "/Whisper":
                boolean result = server.verificationAndSend(commandSplit, name);
                if (result) {
                    out.writeUTF("Message Send");
                } else {
                    out.writeUTF("The user was not found");
                }
                decision = true;
                break;

            case "/Create":
                boolean groupExist = false;
                if (!inGroup) {
                    Set<Group> group = server.getGroup();
                    for (Group verifyGroup : group) {
                        if (commandSplit[1].equals(verifyGroup.getName())) {
                            groupExist = true;
                        }
                    }
                    if (!groupExist) {
                        ExecutorService groupsConnections = server.getGroupConnection();
                        client.setInGroup(true);
                        out.writeUTF("Group create, you are the admin");
                        Group newGroup = new Group(commandSplit[1], name);
                        client.setGroupName(newGroup);
                        newGroup.joinGroup(name, out);
                        group.add(newGroup);
                        groupsConnections.submit(newGroup);
                    } else {
                        out.writeUTF("The group already exist");
                    }
                } else {
                    out.writeUTF("You already in a group.");
                }
                decision = true;
                break;

            case "/Join":
                Set<Group> groups = server.getGroup();
                String messageJoin = "";
                if (inGroup) {
                    out.writeUTF("You are already in a group.");
                } else if (!inGroup) {
                    for (Group verifyGroup : groups) {
                        if (commandSplit[1].equals(verifyGroup.getName())) {
                            verifyGroup.joinGroup(name, out);
                            client.setInGroup(true);
                            client.setGroupName(verifyGroup);
                            messageJoin = "Join the group";
                            break;
                        } else {
                            messageJoin = "Group don't exit";
                        }
                    }
                }
                out.writeUTF(messageJoin);
                decision = true;
                break;

            case "/Room":
                if (inGroup) {
                    StringBuilder message = new StringBuilder();
                    Group playerGroup = client.getGroupName();
                    for (int i = 1; i < commandSplit.length; i++) {
                        message.append(" ").append(commandSplit[i]);
                    }
                    System.out.println(message);
                    playerGroup.receiveMessage("Member: " + name + " send: " + message);
                } else {
                    out.writeUTF("You aren't in a group.");
                }
                decision = true;
                break;

            case "/MemberList":
                if (inGroup) {
                    out.writeUTF(client.getGroupName().membersList());
                } else {
                    out.writeUTF(" You aren't in a group.");
                }
                decision = true;
                break;

            case "/Kick":
                if (inGroup) {
                    client.getGroupName().kickMember(name, commandSplit[1], server, out);
                } else {
                    out.writeUTF("You aren't in a group.");
                }
                decision = true;
                break;

            case "/Exit":
                if (inGroup) {
                    client.getGroupName().exitTheGroup(name, client);
                } else {
                    out.writeUTF("You aren't in a group.");
                }
                decision = true;
                break;

            case "/Close":
                if (inGroup) {
                    Group playerGroup = client.getGroupName();
                    playerGroup.closeTheGroup(name, server);
                } else {
                    out.writeUTF("You aren't in a group");
                }
                decision = true;
                break;

            case "/GroupList":
                server.listGroups(out);
                decision = true;
                break;

            case "/File":
                byte[] buffer = new byte[2048];
                DataOutputStream outReceiver = server.getOut(commandSplit[1]);
                outReceiver.writeUTF("/File");
                int fileLength;
                while((fileLength = in.read(buffer)) > 0) {
                    outReceiver.write(buffer, 0, fileLength);
                }
                decision = true;
                break;
        }

        return  decision;
    }

    private void list(Server server, DataOutputStream out){
        server.listOnline(out);
    }

    private void help(DataOutputStream out) throws IOException {
        String list = "/Quit - close the program and end the connections whit the server. \n" +
                      "/Alias - Change your nickname. /Alias newNickName \n" +
                      "/Help - Give the commands list you can use. \n" +
                      "/List - Give the list of all the users online. \n" +
                      "/Whisper - Send personal message. /Whisper nickName message. \n" +
                      "/Create - Create a group. /Create groupName \n" +
                      "/Join - Join a group. /Join groupName \n" +
                      "/Room - Send the message to the group. /Room message. \n" +
                      "/MemberList - Gives a list of all members in the group \n" +
                      "/Kick - Kick the member of the group. /Kick nickName \n" +
                      "/Exit - Exit the group. \n" +
                      "/Close - Close the group. /Close groupName \n" +
                      "/GroupList - Show all the groups the server have. \n" +
                      "/File - Send a file another member. /File nickName file_address \n";
        out.writeUTF(list);
    }
}
