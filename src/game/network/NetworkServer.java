package game.network;

//NetworkServer.java
import java.io.*;
import java.net.*;
import java.util.*;

public class NetworkServer {

 private ServerSocket serverSocket;
 private final List<ClientHandler> clients = new ArrayList<>();

 public static void main(String[] args) {
     new NetworkServer().start(30000);
 }

 public void start(int port) {
     try {
         serverSocket = new ServerSocket(port);
         System.out.println("[SERVER] Started on port " + port);

         while (clients.size() < 2) {
             Socket socket = serverSocket.accept();
             ClientHandler ch = new ClientHandler(socket, clients.size() + 1);
             clients.add(ch);
             ch.start();
             System.out.println("[SERVER] Player connected: P" + ch.playerId);
         }

         // 두 명 다 들어왔으면 ID 배정
         broadcast("/setid/1");
         broadcast("/setid/2");

     } catch (IOException e) {
         e.printStackTrace();
     }
 }

 private void broadcast(String msg) {
     for (ClientHandler c : clients)
         c.send(msg);
 }

 class ClientHandler extends Thread {
     Socket socket;
     int playerId;
     DataInputStream in;
     DataOutputStream out;

     public ClientHandler(Socket socket, int pid) {
         this.socket = socket;
         this.playerId = pid;

         try {
             in = new DataInputStream(socket.getInputStream());
             out = new DataOutputStream(socket.getOutputStream());
         } catch (Exception e) {
         }
     }

     public void send(String msg) {
         try {
             out.writeUTF(msg);
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

     public void run() {
         while (true) {
             try {
                 String msg = in.readUTF();
                 // 서버는 받은 패킷을 그대로 브로드캐스트만 한다
                 broadcast(msg);
             } catch (Exception e) {
                 break;
             }
         }
     }
 }
}

