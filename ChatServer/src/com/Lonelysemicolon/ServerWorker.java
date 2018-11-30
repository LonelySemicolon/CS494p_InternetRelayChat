package com.Lonelysemicolon;

import java.util.StringTokenizer;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.HashSet;
import java.nio.Buffer;
import java.util.Date;

public class ServerWorker extends Thread {

    private final Socket clientSocket;

    private final Server server;

    private String login = null;

    private OutputStream outputStream;

    private HashSet<String> roomList =  new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket) {//constructor to take in a client connection to sever
        //This client instance is given to the client socket
        this.clientSocket = clientSocket;
        //Take the instance of the serverWorker communicating logon/off
        this.server = server;

    }

    @Override
    public void run(){

        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException, InterruptedException{
        //create an inputStream object to send the server information over the sockets
        InputStream inputStream = clientSocket.getInputStream();
        //Create an outputstream object to send the client information over websockets
        this.outputStream = clientSocket.getOutputStream();

        //Read int he input stream and
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        //This loop will continue till broken by either a exit scheme or a connection break
        while((line = reader.readLine()) != null){
            //Now we are going to split, 'line' into individual tokens to distribute user controls
            String[] tokens = line.split("\\s");
            if(tokens != null && tokens.length > 0) {
                //Given the first token to command, aka cmd
                String cmd = tokens[0];
                if("_login".equals(cmd)) {
                    handleAdmin(tokens, reader);
                    break;
                }
                //Defined in my RFC document for exitting the chat application
                if ("/exit".equals(cmd) || "/logout".equals(cmd)) {
                    handleLogoff();
                    break;
                }else if("/login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                }else if ("/w".equalsIgnoreCase(cmd) || "/b".equals(cmd)) {
                    //for messages split the tokens into only 3 indecies so the 3rd index
                    //contains the message body
                    String[] tokensMsg = line.split("\\s", 3);
                    handleMessage(tokensMsg);
                } else if("/j".equals(cmd)){
                    handleJoin(tokens);
                } else if("/x".equals(cmd)){
                    handleExitRoom(tokens);
                } else if("/whoami".equals(cmd)){
                    String you = "logged in as--> " + this.login + "\n";
                    outputStream.write(you.getBytes());
                } else {
                    //echo back the use input
                    String msg = "Uknkown Command: " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        //Logout user
        clientSocket.close();
    }

    private void handleAdmin(String[] tokens, BufferedReader reader) throws IOException {
        String admin = tokens[1];
        String password = tokens[2];
        if(password.equals("1234")){
            String adminLine;
            List<ServerWorker> workerList = server.getWorkerList();
            while((adminLine = reader.readLine()) != null){
                String[] adTokens = adminLine.split("\\s");
                String adCmd = adTokens[0];
                if("_who".equals(adCmd)){
                    for(ServerWorker worker : workerList){
                        String users = worker.getLogin() + "\n";
                        outputStream.write(users.getBytes());
                    }
                }else if("_x".equals(adCmd)){
                    for(ServerWorker worker: workerList){
                        String check = adTokens[1] + "\n";
                        outputStream.write(check.getBytes());
                        if(adTokens[1].equals(worker.getLogin())){
                            String found = "Kicking " + worker.getLogin()+ "\n";
                            outputStream.write(found.getBytes());
                            worker.adminKick();
                            break;
                        }
                    }
                }
            }
        }else clientSocket.close();
    }



    public void adminKick() throws IOException {
        String byebye = "Kicked by admin, disconnecting...\n";
        outputStream.write(byebye.getBytes());
        List<ServerWorker> workerList = server.getWorkerList();

        //Take the list of serverWorkers, aka threads of users that have an instance of//an instance of a user login, and remove that user who logs off from the list.
        server.removeWorker(this);

        //letting others know you logged off
        String offlineMsg = login + " Logged off" + "\n";
        for(ServerWorker worker : workerList){
            if(!login.equalsIgnoreCase(worker.getLogin())){
                worker.send(offlineMsg);
            }
        }
        clientSocket.close();

    }

    private void handleExitRoom(String[] tokens) throws IOException {
        if(tokens.length > 1) {
            //Index at 0 is the command and the room name is in the second index.
            String roomName = tokens[1];
            //remove the user from the chatroom list
            roomList.remove(roomName);
            List<ServerWorker> workerList = server.getWorkerList();
            for(ServerWorker worker : workerList){
                if(worker.memberOfRoom(roomName)) {
                    String leaveMsg = login + " has left " + roomName + "chatroom\n";
                    worker.send(leaveMsg);
                }
            }
        }
    }

    public boolean memberOfRoom(String roomName){
        return roomList.contains(roomName);
    }

    private void handleJoin(String[] tokens) throws IOException {
        if(tokens.length > 1){
            //Index at 0 is the command and the room name is in the second index.
            String roomName = tokens[1];
            //add the room to the roomList.
            roomList.add(roomName);
        }
    }

    //sending a message, format: "msg" "login" mesg...
    private void handleMessage(String[] tokens) throws IOException {
        //tokens[0] is the command, so 1 and 2 are the joice of the information
        String sendTo = tokens[1];
        String msgBody = tokens[2];

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList){
            if(tokens[0].equals("/b") && worker.memberOfRoom(sendTo)){
                String outMsg = "(chatroom " + sendTo + ")" + login + ": " + msgBody + "\n";
                worker.send(outMsg);
            }else if(!login.equals(worker.getLogin())) {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) ;
                String outMsg = login + ": " + msgBody + "\n";
                worker.send(outMsg);
            }
        }
    }

    private void handleLogoff() throws IOException{
        List<ServerWorker> workerList = server.getWorkerList();

        //Take the list of serverWorkers, aka threads of users that have an instance of//an instance of a user login, and remove that user who logs off from the list.
        server.removeWorker(this);

        //letting others know you logged off
        String offlineMsg = login + " Logged off" + "\n";
        for(ServerWorker worker : workerList){
            if(!login.equalsIgnoreCase(worker.getLogin())){
                worker.send(offlineMsg);
            }
        }
        clientSocket.close();
    }

    //Passing the login of this user to other serverWorker objects into the serverWorker list
    public String getLogin(){
        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if(tokens.length == 2){
            //Expecting only 3 arguments for Username and password login.
            //Second argument, at index 1 is the username
            String login = tokens[1];

            //setting up a state for the login
            this.login = login;

            //might not use password, might just have any number of user's login and just have
            //name and join chats as they please.
            //String password = tokens[2];

            //echo back to the user their login name
            String msg = "Logging in as: " + login + "\n";
            outputStream.write(msg.getBytes());

            //When a user logs in they must send their login name to other online users
            List<ServerWorker> workerList = server.getWorkerList();

            //loop through all worker in the workerList and get the online list
            String onlineMsg = login + " has logged in.\n";
            for(ServerWorker worker : workerList){
                //keep from sending a status update to yourself *You know you logged in.
                if(!login.equals(worker.getLogin())) {
                    if(worker.getLogin() != null) {
                        String msg2 = "online " + worker.getLogin() + "\n";
                        send(msg2);
                    }
                }
            }

            //This will let other users this person just logged in.
            for(ServerWorker worker : workerList){
                if(!login.equals(worker.getLogin())) {
                    worker.send(onlineMsg);
                }
            }

        }
    }

    //This is the helper function to send messages/status/.
    private void send(String msg) throws IOException {
        if(login != null) {
            //Send msg/msg2 to other users for status updates.
            outputStream.write(msg.getBytes());
        }
    }
}

