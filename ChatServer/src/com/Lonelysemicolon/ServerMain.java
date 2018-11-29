package com.Lonelysemicolon;

/*
    Code adapted from JimOnDemand java tutorials
    Author: Benjamin Baleilevuka 11/28/2018
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static sun.misc.PostVMInitHook.run;


public class ServerMain {
    public static void main(String[] args){
        int port = 1271; //port to pass into server socket to make the connection to the client

        //Create an object of the server, which extends thread, that has serverWorker threads
        //in each server thread
        Server server = new Server(port);

        //Start the Server Thread, allowing for multiple thread to be invoked
        server.start();

    }


}
