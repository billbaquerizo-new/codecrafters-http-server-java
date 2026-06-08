import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        //1. Parse command-line arguments to find the directory flag
        String directory = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--directory") && i + 1 < args.length) {
                directory = args[i + 1];
                break;
            }
        }
        System.out.println("Configured root directory: " + directory);

        // We make a final copy of the directory variable so the threads can access it safely
        final String finalDirectory = directory;

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
                            String method = parts[0]; // Captures "GET" or "POST"
                            String path = parts[1]; // The second element is the target URL path

                            System.out.println("Requested path: " + path);

                            // 1. Read all headers from the stream so we can inspect them
                            String userAgentValue = "";
                            int contentLength = 0;  // Will store the payload size for POST request
                            String headerLine;

                            // An HTTP header block ends with an empty line
                            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                                if (headerLine.startsWith("User-Agent: ")) {
                                    // Extract everything after "User-Agent: " (which is 12 characters long)
                                    userAgentValue = headerLine.substring(12);
                                }
                                // Safely extract the content length value
                                if (headerLine.toLowerCase().startsWith("content-length: ")) {
                                    contentLength = Integer.parseInt(headerLine.substring(16).trim());
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
                            }
                            else if (path.startsWith("/files/")) {
                                String filename = path.substring(7);
                                Path filePath = Path.of(finalDirectory, filename);

                                if (method.equals("GET")) {
                                    if (Files.exists(filePath)) {
                                        // Read file content as raw bytes
                                        byte[] fileBytes = Files.readAllBytes(filePath);

                                        // Build response headers
                                        String header = "HTTP/1.1 200 OK\r\n" +
                                                "Content-Type: application/octet-stream\r\n" +
                                                "Content-Length: " + fileBytes.length + "\r\n" +
                                                "\r\n";

                                        // Write headers first, then immediately write raw file data bytes
                                        clientSocket.getOutputStream().write(header.getBytes());
                                        clientSocket.getOutputStream().write(fileBytes);
                                    } else {
                                        // File wasn't found in the directory
                                        clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                                    }
                                }
                                else if (method.equals("POST")) {
                                    // 1. Initialize a buffer matching the dynamic Content-Length size
                                    char[] bodyBuffer = new char[contentLength];

                                    // 2. Read exactly that amount of data from the stream
                                    reader.read(bodyBuffer, 0, contentLength);
                                    String requestBody = new String(bodyBuffer);

                                    // 3. Write the payload string to disk
                                    Files.writeString(filePath, requestBody);

                                    // 4. Return the official 201 Created confirmation status
                                    clientSocket.getOutputStream().write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
                                }
                            }
                            else {
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

