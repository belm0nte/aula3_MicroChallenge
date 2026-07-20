
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static String requisicao (String prompt){
        Dotenv dotenv = Dotenv.load();

        String apiKey = dotenv.get("GEMINI_API_KEY");
        String URL = dotenv.get("URL_CONEXAO");

        String json = """
        
        {
          "contents": [{
            "parts": [{
              "text": "%s"
            }]
          }]
        }
        """.formatted(prompt);

        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(URL + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(json))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

       return response.body();
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Digite a frase: ");
        String frase = scanner.nextLine();
        String resposta = requisicao(frase);

        System.out.println(resposta);

        /*
        String[] palavras = frase.split(" ");

        for (String palavra : palavras) {
            System.out.println(palavra);
        }S

        List<String> word_low = new ArrayList<>();
*/

    }
}
