package http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

    private DataOutputStream dos = null;
    private Map<String, String> responseHeader = new HashMap<>();

    public HttpResponse(OutputStream out) {
        dos = new DataOutputStream(out);
    }

    public void forward(String fileName) throws IOException {
        byte[] body = Files.readAllBytes(new File("./webapp" + fileName).toPath());
        response200Header(dos, body.length);
        responseBody(dos, body);
    }

    public void sendRedirect(String fileName) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            if(!responseHeader.get("Set-Cookie").equals("")){
                dos.writeBytes("Set-Cookie: " + responseHeader.get("Set-Cookie") + " \r\n");
            }
            dos.writeBytes("Location: " + fileName + " \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void addHeader(String headerKey, String headerValue) {
        responseHeader.put(headerKey, headerValue);
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

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
