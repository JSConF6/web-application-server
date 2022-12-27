package http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;
import webserver.RequestHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

    private String method = "";
    private String path;
    private Map<String, String> header = new HashMap<>();
    private Map<String, String> parameter = new HashMap<>();

    public HttpRequest(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line = br.readLine();

        if(line == null){
            return;
        }

        String[] tokens = line.split(" ");
        this.method = tokens[0];
        this.path = tokens[1];

        while(!"".equals(line)){
            line = br.readLine();
            HttpRequestUtils.Pair pair = HttpRequestUtils.parseHeader(line);
            if(pair == null){
                return;
            }
            header.put(pair.getKey(), pair.getValue());
        }

        if("GET".equals(this.method)){
            int index = tokens[1].indexOf("?");
            String path = tokens[1].substring(0, index);
            String queryString = tokens[1].substring(index + 1);
            this.path = path;
            parameter = HttpRequestUtils.parseQueryString(queryString);
        }else if("POST".equals(this.method)){
            int contentLength = Integer.parseInt(this.header.get("Content-Length"));
            String body = IOUtils.readData(br, contentLength);
            parameter = HttpRequestUtils.parseQueryString(body);
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHeader(String headerName) {
        return header.get(headerName);
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public String getParameter(String parameterName) {
        return parameter.get(parameterName);
    }

    public void setParameter(Map<String, String> parameter) {
        this.parameter = parameter;
    }
}
