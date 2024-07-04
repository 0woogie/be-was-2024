package webserver;

import db.Database;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestHandlerTest {
    private Socket socket;
    private ByteArrayOutputStream out;
    private RequestHandler requestHandler;

    @BeforeEach
    public void setUp() throws IOException {
        socket = Mockito.mock(Socket.class);
        out = new ByteArrayOutputStream();
        Mockito.when(socket.getOutputStream()).thenReturn(out);
        requestHandler = new RequestHandler(socket);
    }

    @Test
    public void testHandleUserCreation() throws IOException {
        //Given
        String path = "/create?username=0woogie&password=password&nickname=%EC%98%81%EC%9A%B1&email=aaa111%40naver.com";

        //When
        requestHandler.handleUserCreation(path, out);

        //Then
        User user = Database.findUserById("0woogie");
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo("0woogie");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getName()).isEqualTo("영욱");
        assertThat(user.getEmail()).isEqualTo("aaa111@naver.com");

        //response 생성
        String response = out.toString();
        assertThat(response).contains("HTTP/1.1 200 OK");
        assertThat(response).contains("Registration Successful");
    }
}
