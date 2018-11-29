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
        while((line = reader.readLine()) != null){
            //Now we are going to split, 'line' into individual tokens to distribute user controls
            String[] tokens = line.split("\\s");
            if(tokens != null && tokens.length > 0) {
                //Given the first token to command, aka cmd
                String cmd = tokens[0];
                //Defined in my RFC document for exitting the chat application
                if ("/exit".equals(cmd) || "/logout".equals(cmd)) {
                    handleLogoff();
                    break;
                }else if("/login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                }else if ("/w".equalsIgnoreCase(cmd)) {
                    //for messages split the tokens into only 3 indecies so the 3rd index
                    //contains the message body
                    String[] tokensMsg = line.split("\\s", 3);
                    handleMessage(tokensMsg);
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

    //sending a message, format: "msg" "login" mesg...
    private void handleMessage(String[] tokens) throws IOException {
        //tokens[0] is the command, so 1 and 2 are the joice of the information
        String sendTo = tokens[1];
        String msgBody = tokens[2];

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList){
            if(!login.equals(worker.getLogin())) {
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

