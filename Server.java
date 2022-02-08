/* Notes: 
 * This code is modified from the original to work with 
 * the CS 352 chat client:
 *
 * 1. added args to allow for a command line to the port 
 * 2. Added 200 OK code to the sendResponse near line 77
 * 3. Changed default file name in getFilePath method to ./ from www 
 */ 

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Read the full article https://dev.to/mateuszjarzyna/build-your-own-http-server-in-java-in-less-than-one-hour-only-get-method-2k02
public class Server {

    static String currUser = "";
    static boolean loggedIn, validCookie;
    public static void main( String[] args ) throws Exception {

	if (args.length != 1) 
        {
            System.err.println("Usage: java Server <port number>");
            System.exit(1);
        }
        //create server socket given port number
        int portNumber = Integer.parseInt(args[0]);
	
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Connected to port: " + portNumber);
            while (true) {
                try (Socket client = serverSocket.accept()) {
                    handleClient(client);
                }
            }
        }
    }

    private static void handleClient(Socket client) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
        StringBuilder requestBuilder = new StringBuilder();
        while(br.ready()){
            char character = (char) br.read();
            requestBuilder.append(character);
        }
        String request = requestBuilder.toString();
        System.out.printf("The request is: %s \n", request);

	    String[] requestsLines = request.split("\r\n");
        String[] requestLine = requestsLines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine[1];
        String version = requestLine[2];
        String host = requestsLines[1].split(" ")[1];

	// build the reponse here 
        if (method.equals("POST")){
            if(path.equals("/chat/")){
                String userNum = "", currCookie = "";
                
                List<String> headers = new ArrayList<>();
                for(int i = 2; i < requestsLines.length - 2; i++){
                    String header = requestsLines[i];

                    if (header.contains("Cookie")) {
                        currCookie = header;
                        String cookieSplit = currCookie.split(" ")[1];
                        userNum = cookieSplit.split("=")[1];
                        currUser = userNum;
                    }
    
                    headers.add(header);
                }

                String msg = requestsLines[requestsLines.length - 1];
			    String realMsg = msg.split("=")[1];

			    String logs = String.format("Client %s, method %s, " + " path %s, version %s, host %s, headers %s ," + " body %s, userNum %s\n", 
                    client.toString(), method, path, version, host, headers.toString(), msg, userNum);
			    System.out.println(logs + "\n");

                if (!userNum.equals("")){
				loggedIn = true;
				BufferedWriter out = null;

                    try {
                        FileWriter fileStream = new FileWriter("./chat/log.txt", true); 
                        out = new BufferedWriter(fileStream);
                        out.write(currUser + "," + realMsg + "\n");
                        out.close();
                    } catch (IOException e) {
                        final String fName = "log.txt";
                        final File file = new File("./chat/", fName);
                        file.createNewFile();

                        FileWriter fileStream = new FileWriter("/chat/log.txt", true);
                        out = new BufferedWriter(fileStream);
                        out.write(currUser + "," + realMsg + "\n");
                        out.close();
                    }
			    }

                if (loggedIn) {
                    String cPath = "/chat/chat.html";
                    Path fPath = getFilePath(cPath);
                    if (Files.exists(fPath)) {
                        String contentType = guessContentType(fPath);
                        validCookie = true;
                        sendResponse(client, "200 OK", contentType, 
                            Files.readAllBytes(fPath), currUser);
                    }
                }else{
                    String ePath = "/login/error.html";
                    Path fPath = getFilePath(ePath);
                    if (Files.exists(fPath)) {
                        String contentType = guessContentType(fPath);
                        sendResponse(client, "200 OK", contentType, 
                            Files.readAllBytes(fPath), "");
                    }
                }
            } else if (path.equals("/login/")) {
                List<String> headers = new ArrayList<>();
                for (int h = 2; h < requestsLines.length - 2; h++) {
                    String header = requestsLines[h];
                    headers.add(header);
                }
    
                String body = requestsLines[requestsLines.length - 1];
    
                String logs = String.format("Client %s, method %s, " + 
                    "path %s, version %s, host %s, headers %s, body %s\n",
                    client.toString(), method, path, 
                    version, host, headers.toString(), body);
                System.out.println(logs + "\n");
    
                String[] loginInfo = body.split("&");
                String[] user = loginInfo[0].split("=");
                String[] password = loginInfo[1].split("=");
    
                String credentialsPath = "login/credentials.txt";
                Scanner scanner = new Scanner(new File(credentialsPath));
    
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] currCreds = line.split(",");
    
                    if (user[1].equals(currCreds[0])) 
                    {
                        if (password[1].equals(currCreds[1])) {
                            loggedIn = true;
                            currUser = user[1];
                            break;
                        }
                    }
                }
    
                if (loggedIn) {
                    System.out.println("Successfully logged in.");
                } else {
                    System.out.println("Sorry, invalid login.");
                }

                if (loggedIn) {
                    String cPath = "/chat/chat.html";
                    Path fPath = getFilePath(cPath);
                    if (Files.exists(fPath)) {
                        String contentType = guessContentType(fPath);
                        validCookie = true;
                        sendResponse(client, "200 OK", contentType, 
                        Files.readAllBytes(fPath), currUser);
                    }
                } else {
                    String ePath = "/login/error.html";
                    Path fPath = getFilePath(ePath);
                    if (Files.exists(fPath)) {
                        String contentType = guessContentType(fPath);
                        sendResponse(client, "200 OK", contentType, 
                        Files.readAllBytes(fPath), "");
                    }
                }
            }
        } else if(method.equals("GET")){
            if (path.equals("/chat/") || path.equals("/login/")) {
                List<String> headers = new ArrayList<>();
                for (int h = 2; h < requestsLines.length; h++) {
                    String header = requestsLines[h];
                    headers.add(header);
                }
    
                String accessLog = String.format("Client %s, method %s, "
                    + "path %s, version %s, host %s, headers %s\n",
                    client.toString(), method, path, version, host, headers.toString());
                System.out.println(accessLog + "\n");
            }

            if (path.equals("/login/")) {
                String lPath = "/login/login.html";
                Path fPath = getFilePath(lPath);
                if (Files.exists(fPath)) {
                    String contentType = guessContentType(fPath);
                    sendResponse(client, "200 OK", contentType, 
                        Files.readAllBytes(fPath), "");
                } else {
                    byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
                    sendResponse(client, "404 Not Found", "text/html", notFoundContent, "");
                }
            } else if (path.equals("/chat/")) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                String logPath = "./chat/log.txt";
                Scanner scanner = new Scanner(new File(logPath));

                byte[] html1 = ("<html>\n<body>\n<h1>Chat Page for CS352</h1>\n<p>\n"
                    + "    Chat Space  :\n</p>\n<div id=\"chat-window\">\n").getBytes();
                output.write(html1);

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] msg = line.split(",");
                    byte[] msgByte = (" <p>" + msg[0] + ": " 
                        + msg[1] + "</p>\n").getBytes();
                    output.write(msgByte);
                }

                byte[] html2 = ("</div>\n<form action=\"/\" method=\"post\">\n"
                    + "   <p>Enter msg : </p>\n   <input type=\"text\" name=\"msg\">\n"
                    + "    <p></p>\n   <input type=\"submit\" value=\"Enter\">\n</form>\n</body>\n"
                    + "</html>\n").getBytes();
                output.write(html2);
                byte[] out = output.toByteArray();
                sendResponse(client, "200 OK", "text/html", out, "");
            }
        }
    }

    private static void sendResponse(Socket client, String status, String contentType, byte[] content, String userNum) throws IOException {

        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 200 OK\r\n" + status).getBytes());
        if(validCookie){
            clientOutput.write(("CookieID: " + userNum + "\r\n").getBytes());
        }
        clientOutput.write(("ContentType: " + contentType + "\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }

    private static Path getFilePath(String path) {
        if ("/".equals(path)) {
            path = "/index.html";
        }

        return Paths.get("./", path);
    }

    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }

}
