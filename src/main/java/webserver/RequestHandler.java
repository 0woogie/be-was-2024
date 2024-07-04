package webserver;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import db.Database;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private static final String BASE_DIR = "src/main/resources/static";

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder requestHeader = new StringBuilder();

            //HTTP 요청 헤더를 읽고 모든 내용을 로거를 사용하여 출력
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                requestHeader.append(line).append("\n");
            }
            logger.debug("HTTP Request Headers:\n{}", requestHeader.toString());

            String[] requestLines = requestHeader.toString().split("\n");
            if (requestLines.length > 0) {
                String[] requestLineTokens = requestLines[0].split(" ");
                String method = requestLineTokens[0];
                String path = requestLineTokens[1];
                logger.debug("Request Method: {}, Path: {}", method, path);

                if ("/".equals(path)) {
                    path = "/index.html";
                }
                if ("/registration".equals(path)) {
                    path = "/registration/index.html";
                }

                if (path.startsWith("/create")) { //회원가입 요청 처리
                    handleUserCreation(path, out);
                } else { //정적 리소스 처리
                    serveStaticFile(path, out);
                }
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void serveStaticFile(String path, OutputStream out) throws IOException {
        File file = new File(BASE_DIR + path);
        if (file.exists()) { //정적 리소스 존재하는 경우
            byte[] body = readFileToByteArray(file);
            DataOutputStream dos = new DataOutputStream(out);
            String ext = getFileExtension(file.getName());
            String contentType = ContentType.getContentTypeByExtension(ext);
            response200Header(dos, body.length, contentType);
            responseBody(dos, body);
        } else { //파일이 없는 경우
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = "<h1>File Not Found</h1>".getBytes();
            response404Header(dos, body.length);
            responseBody(dos, body);
        }
    }

    public void handleUserCreation(String path, OutputStream out) throws IOException {

        //사용자가 입력한 값을 파싱해 User 클래스에 저장
        String queryString = path.split("\\?")[1];
        Map<String, String> params = parseQueryString(queryString);
        User user = new User(params.get("username"), params.get("password"), params.get("nickname"), params.get("email"));
        Database.addUser(user);
        logger.debug("User Created: {}", user);

        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = "<h1>Registration Successful</h1>".getBytes();
        response200Header(dos, body.length, "text/html");
        responseBody(dos, body);
    }

    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private byte[] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String contentType) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + contentType + "\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void response404Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 404 Not Found\r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName.lastIndexOf('.') > 0) {
            return fileName.substring(fileName.lastIndexOf('.') + 1);
        } else {
            return "";
        }
    }

    private enum ContentType {
        HTML("html", "text/html;charset=utf-8"),
        CSS("css", "text/css"),
        JS("js", "application/javascript"),
        ICO("ico", "image/x-icon"),
        PNG("png", "image/png"),
        JPG("jpg", "image/jpeg"),
        SVG("svg", "image/svg+xml"),
        DEFAULT("", "application/octet-stream");

        private final String extension;
        private final String contentType;

        ContentType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public static String getContentTypeByExtension(String extension) {
            for (ContentType type : values()) {
                if (type.extension.equals(extension)) {
                    return type.contentType;
                }
            }
            return DEFAULT.contentType;
        }
    }
}
