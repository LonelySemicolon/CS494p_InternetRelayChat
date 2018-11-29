package com.Lonelysemicolon;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread{
    private final int serverPort;

    private ArrayList<ServerWorker> workerList = new ArrayList();

    public Server(int serverPort){
        this.serverPort = serverPort;
    }

    //Adding a way for serverWorkers to see all other serverWorkers
    public List<ServerWorker> getWorkerList(){
        return workerList;
    }



    @Override
    public void run() {
        try {
            //Create a new instance of the server socket for the server to host a connection
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server up and running: " + serverSocket);
            //Create an instance for a client to connect to the server socket created above
            //and accept the connection
            while(true) {
                //This will loop over new connections and accept them and create a new
                //instance of the client socket each time
                Socket clientSocket = serverSocket.accept();


                //Create a thread to run this instance of a user logging in
                //Ever connection will have its own thread, this thread will allow multiple
                //connections for multiple users.
                ServerWorker worker = new ServerWorker(this, clientSocket);

                //After a connection is made we are going to start up the threads
                //for the serverWorkers and get them started with user logins and we pass
                //The ServerWorking into the list declared above.
                workerList.add(worker);

                //This will create a new thread for ever connection allowing for multiple user.
                worker.start();


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeWorker(ServerWorker serverWorker) {
        workerList.remove(serverWorker);
    }
}
