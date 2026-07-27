import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Scanner;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

    private static final String PRE_PROMPT = """
            Você é um especialista em análise de segurança digital e detecção de fraudes.
            
            Sua tarefa é analisar a frase enviada pelo usuário e determinar se ela representa um possível risco de segurança.
            
            Considere os seguintes critérios:
            
            - Tentativas de golpe, fraude, phishing ou engenharia social.
            - Solicitação de informações sensíveis, como senhas, códigos de autenticação, CPF, dados bancários, número de cartão ou documentos pessoais.
            - Linguagem manipulativa, intimidação, ameaças ou coação.
            - Criação de senso de urgência artificial (ex.: "aja agora", "sua conta será bloqueada", "última chance", "rodadas grátis").
            - Solicitação para clicar em links, instalar aplicativos ou realizar pagamentos sem contexto confiável.
            - Promessas de vantagens irreais ou ofertas excessivamente vantajosas.
            
            ### Sistema de Pontuação
            
            Identifique as palavras ou expressões suspeitas e atribua uma pontuação conforme o nível de risco:
            
            - 1 ponto: indício leve de manipulação ou urgência.
            - 2 pontos: solicitação suspeita ou comportamento típico de fraude.
            - 3 pontos: forte evidência de golpe, phishing, roubo de dados ou ameaça.
            
            Some a pontuação total.
            
            ### Classificação
            
            - 0 a 2 pontos → NAO_SUSPEITA
            - 3 a 5 pontos → SUSPEITA
            - 6 pontos ou mais → ALTAMENTE_SUSPEITA
            
            ### Resposta
            
            Responda obrigatoriamente no seguinte formato:
            
            CLASSIFICACAO: <NAO_SUSPEITA | SUSPEITA | ALTAMENTE_SUSPEITA>
            
            PONTUACAO_TOTAL: <valor>
            
            PALAVRAS_SUSPEITAS:
            - "<palavra ou expressão>" -> <1|2|3> ponto(s)
            - ...
            
            MOTIVO:
            <Explicação breve justificando a classificação com base nos critérios identificados.>
            
            Frase para análise:
            "%s"
            """;

    public static String requisicao(String fraseUsuario) {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("GEMINI_API_KEY");
        String url = dotenv.get("URL_CONEXAO");

        String promptFinal = PRE_PROMPT.formatted(escapeJson(fraseUsuario));

        String json = """
                {
                  "contents": [{
                    "parts": [{
                      "text": "%s"
                    }]
                  }]
                }
                """.formatted(escapeJson(promptFinal));

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(url + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(json))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public static String extrairTexto(String respostaBruta) {
        try {
            JSONObject root = new JSONObject(respostaBruta);
            JSONArray candidates = root.getJSONArray("candidates");
            JSONObject primeiroCandidato = candidates.getJSONObject(0);
            JSONObject content = primeiroCandidato.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            return parts.getJSONObject(0).getString("text");
        } catch (Exception e) {
            return "Erro ao extrair texto da resposta: " + e.getMessage()
                    + "\nResposta bruta: " + respostaBruta;
        }
    }

    private static String escapeJson(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("======== BEM-VINDO AO SMART SCAM SEARCHER ========");
        System.out.println("Digite a frase para análise: ");
        String frase = scanner.nextLine();

        String respostaBruta = requisicao(frase);
        String textoExtraido = extrairTexto(respostaBruta);

        System.out.println("\n=== RESULTADO DA ANÁLISE ===");
        System.out.println(textoExtraido);
    }
}