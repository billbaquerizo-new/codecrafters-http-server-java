import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        //1. Parse command-line arguments to find the directory flag
        String directory = "";
        System.out.println(args[0]);
        System.out.println(args[1]);
        System.out.println(args[2]);

        try (ServerSocket serverSocket = new ServerSocket(4221)) {

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                //accept.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                System.out.println("accepted new connection");

                Thread.ofVirtual().start(() -> {
                    try {
                        // Set up readers and writers for text communication
                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        // 1. Read the very first line (The Request Line)
                        String requestLine = reader.readLine();

                        if (requestLine != null && !requestLine.isEmpty()) {
                            // 2. Split the line by spaces: ["GET", "/path", "HTTP/1.1"]
                            String[] parts = requestLine.split(" ");
                            String path = parts[1]; // The second element is the target URL path

                            System.out.println("Requested path: " + path);

                            // 1. Read all headers from the stream so we can inspect them
                            String userAgentValue = "";
                            String headerLine;

                            // An HTTP header block ends with an empty line
                            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                                if (headerLine.startsWith("User-Agent: ")) {
                                    // Extract everything after "User-Agent: " (which is 12 characters long)
                                    userAgentValue = headerLine.substring(12);
                                }
                            }

                            // 2. Route based on the path and send the response
                            if (path.equals("/")) {
                                clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                            } else if (path.startsWith("/echo/")) {
                                // 1. Extract the string following "/echo/"
                                String content = path.substring(6);

                                // 2. Build the precise multi-line HTTP response
                                String response = "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: text/plain\r\n" +
                                        "Content-Length: " + content.length() + "\r\n" +
                                        "\r\n" + // Double CRLF separating headers from body
                                        content;

                                // 3. Write out the entire response string as raw bytes
                                clientSocket.getOutputStream().write(response.getBytes());
                            } else if (path.startsWith("/user-agent")) {
                                String response = "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: text/plain\r\n" +
                                        "Content-Length: " + userAgentValue.length() + "\r\n" +
                                        "\r\n" + // Double CRLF separating headers from body
                                        userAgentValue;
                                clientSocket.getOutputStream().write(response.getBytes());
                            } else {
                                clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                            }
                        }
                        // 4. Flush and cleanly close the connection for this client
                        clientSocket.getOutputStream().flush();
                        clientSocket.close();
                    } catch (IOException e) {
                        System.out.println("IOException: " + e.getMessage());
                    }
                }); // End of thread task definition
            }
        } catch(IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

