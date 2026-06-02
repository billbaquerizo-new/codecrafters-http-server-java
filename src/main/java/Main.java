import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // TODO: Uncomment the code below to pass the first stage

     try (ServerSocket serverSocket = new ServerSocket(4221)) {

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       while (true) {
           Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
           //accept.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
           System.out.println("accepted new connection");

           // Set up readers and writers for text communication
           BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

           // 1. Read the very first line (The Request Line)
           String requestLine = reader.readLine();

           if (requestLine != null && !requestLine.isEmpty()) {
               // 2. Split the line by spaces: ["GET", "/path", "HTTP/1.1"]
               String[] parts = requestLine.split(" ");
               String path = parts[1]; // The second element is the target URL path

               System.out.println("Requested path: " + path);

               // 3. Route based on the path and send the response
               if (path.equals("/")) {
                   clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
               } else {
                   clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
               }
           }

           // 4. Flush and cleanly close the connection for this client
           clientSocket.getOutputStream().flush();
           clientSocket.close();
       }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
