package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import jdk.internal.util.xml.impl.Input;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = br.readLine();
            log.info("request line : {}", line);

            if(line == null){
                return;
            }

            String[] tokens = line.split(" ");
            String method = tokens[0];
            String url = tokens[1];
            int contentLength = 0;
            Map<String, String> cookiesMap = new HashMap<>();
            String accept = "";

            while(!"".equals(line)){
                line = br.readLine();
                if(line.contains("Content-Length")){
                    contentLength = Integer.parseInt(line.split(" ")[1]);
                }else if(line.contains("Cookie")){
                    cookiesMap = HttpRequestUtils.parseCookies(line.split(":")[1]);
                } else if(line.split(":")[0].equals("Accept")){
                    accept = line.split(",")[0];
                }
                log.info("header : {}", line);
            }

            if("POST".equals(method)){
                if(tokens[1].contains("/user/create")){
                    String params = IOUtils.readData(br, contentLength);
                    Map<String, String> userMap = HttpRequestUtils.parseQueryString(params);
                    User user = new User(userMap.get("userId"), userMap.get("password"), userMap.get("name"), userMap.get("email"));
                    DataBase.addUser(user);
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = Files.readAllBytes(new File("./webapp/index.html").toPath());
                    response302Header(dos, body.length);
                    responseBody(dos, body);
                } else if(tokens[1].contains("/user/login")){
                    String params = IOUtils.readData(br, contentLength);
                    Map<String, String> userMap = HttpRequestUtils.parseQueryString(params);
                    User user = DataBase.findUserById(userMap.get("userId"));
                    if(user == null){
                        DataOutputStream dos = new DataOutputStream(out);
                        byte[] body = Files.readAllBytes(new File("./webapp/user/login_failed.html").toPath());
                        responseLogin200Header(dos, body.length, "logined=false");
                        responseBody(dos, body);
                    }else{
                        if(!user.getPassword().equals(userMap.get("password"))){
                            DataOutputStream dos = new DataOutputStream(out);
                            byte[] body = Files.readAllBytes(new File("./webapp/user/login_failed.html").toPath());
                            responseLogin200Header(dos, body.length, "logined=false");
                            responseBody(dos, body);
                        }else{
                            DataOutputStream dos = new DataOutputStream(out);
                            byte[] body = Files.readAllBytes(new File("./webapp/index.html").toPath());
                            responseLogin302Header(dos, body.length, "logined=true");
                            responseBody(dos, body);
                        }
                    }
                }
            }else if(tokens[1].contains("/user/list")){
                boolean isLogin = Boolean.parseBoolean(cookiesMap.get("logined"));
                if(isLogin){
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                }else{
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = Files.readAllBytes(new File("./webapp/user/login.html").toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                }
            } else if("text/css".equals(accept.split(" ")[1].trim())){
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                responseCss200Header(dos, body.length);
                responseBody(dos, body);
            }else{
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseCss200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseLogin200Header(DataOutputStream dos, int lengthOfBodyContent, String isLogin) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("Set-Cookie: " + isLogin + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 302 \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseLogin302Header(DataOutputStream dos, int lengthOfBodyContent, String isLogin) {
        try {
            dos.writeBytes("HTTP/1.1 302 \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("Set-Cookie: " + isLogin + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
