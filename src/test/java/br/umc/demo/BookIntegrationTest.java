package br.umc.demo;

import br.umc.demo.dto.AuthResponse;
import br.umc.demo.dto.AuthorRequest;
import br.umc.demo.dto.AuthorResponse;
import br.umc.demo.dto.BookRequest;
import br.umc.demo.dto.BookResponse;
import br.umc.demo.dto.LoginRequest;
import br.umc.demo.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient client;
    private String tokenAdmin;
    private String tokenUser;

    @BeforeEach
    void autenticar() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> {})
                .build();

        LoginRequest loginAdmin = new LoginRequest();
        loginAdmin.setEmail("admin@biblioteca.com");
        loginAdmin.setPassword("Admin@123");
        tokenAdmin = client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginAdmin)
                .retrieve()
                .toEntity(AuthResponse.class)
                .getBody().getToken();

        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Usuario Book Test");
        reg.setEmail("user.book." + System.nanoTime() + "@test.com");
        reg.setPhone("11900000003");
        reg.setPassword("Senha@Book1");
        tokenUser = client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(reg)
                .retrieve()
                .toEntity(AuthResponse.class)
                .getBody().getToken();
    }

    // -------------------------------------------------------
    // GET /api/books
    // -------------------------------------------------------

    @Test
    void listBooks_userAutenticado_deveRetornar200() {
        ResponseEntity<Void> response = client.get().uri("/api/books")
                .header("Authorization", "Bearer " + tokenUser)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listBooks_adminAutenticado_deveRetornar200() {
        ResponseEntity<Void> response = client.get().uri("/api/books")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // -------------------------------------------------------
    // GET /api/books/{id}
    // -------------------------------------------------------

    @Test
    void getBook_comIdExistente_deveRetornar200ComDados() {
        long ts = System.nanoTime();
        Long id = criarLivro("Livro Busca " + ts, "LB-" + ts);

        ResponseEntity<BookResponse> response = client.get().uri("/api/books/" + id)
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toEntity(BookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(id);
    }

    @Test
    void getBook_comIdInexistente_deveRetornar404() {
        ResponseEntity<Void> response = client.get().uri("/api/books/99999")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Long criarLivro(String titulo, String isbn) {
        Long authorId = criarAutor("Autor " + isbn);
        BookRequest request = new BookRequest();
        request.setTitle(titulo);
        request.setAuthorIds(List.of(authorId));
        request.setPublisher("Editora Teste");
        request.setIsbn(isbn);
        request.setSummary("Resumo");
        return client.post().uri("/api/books")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(BookResponse.class)
                .getBody().getId();
    }

    private Long criarAutor(String nome) {
        AuthorRequest request = new AuthorRequest();
        request.setName(nome);
        return client.post().uri("/api/authors")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(AuthorResponse.class)
                .getBody().getId();
    }
}
