package com.seumod.mcpblock.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class McpHttpClient {
    private static final Logger LOGGER = Logger.getLogger(McpHttpClient.class.getName());
    private static final String LMSTUDIO_URL = "http://localhost:1234/v1/chat/completions";
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(1500);

// System prompt following the AI Controller Spec - STRICT VERSION with enhanced safeguards
    private static final String SYSTEM_PROMPT = "VOCE EH UMA IA PARA MINECRAFT. REGRAS ESTRITAS:\n\n1. QUANDO O USUARIO PEDIR ACAO NO JOGO, RESPONDA APENAS COM:\n   CMD: <comando>\n   NADA MAIS! NAO ADICIONE TEXTO, NAO ADICIONE EXPLICACOES!\n\n2. COMANDOS PERMITIDOS: give, sumon, summon\n\n3. PARA give: use \"give NomedoJogador minecraft:item_id quantidade\"\n   Exemplos corretos:\n   - give medowlucas minecraft:diamond_pickaxe 1\n   - give medowlucas minecraft:diamond_sword 1\n   - give medowlucas minecraft:diamond_shovel 1\n\n4. PARA sumon/summon: use \"summon minecraft:entity_id ~~~\"\n   Use ~~~ (tres til) para coordenadas relativas - o sistema ira calcular automaticamente a posicao na frente do jogador.\n   Exemplos:\n   - sumon minecraft:panda ~~~\n   - summon minecraft:zombie ~~~\n   - sumon minecraft:cow ~~~\n   - summon minecraft:ender_dragon ~~~\n\n5. REGRAS CRITICAS:\n   - IMPORTANTE: Use underscore (_) para item names: diamond_pickaxe, iron_sword\n   - Use underscore (_) para entity names: ender_dragon, zombie, panda\n   - NUNCA use @p, @a, @s, @e, @r\n   - Para sumon/summon, use sempre ~~~ (nao use coordenadas numericas)\n   - SEMPRE use o nome exato do jogador em give\n   - NUNCA adicione texto, emoji, ou formatacao markdown apos o comando\n\n6. SE NAO SOUBE O COMANDO CORRETO, RESPONDA NORMALMENTE (sem CMD:)\n\n7. REGRAS ADICIONAIS DE SEGURANCA:\n   - NAO RESPONDA COM COMANDOS QUE POSSAM CAUSAR DANOS AO SERVIDOR\n   - NAO RESPONDA COM COMANDOS QUE POSSAM AFETAR OUTROS JOGADORES\n   - NAO RESPONDA COM COMANDOS QUE POSSAM CAUSAR LAG OU CRASH\n   - SE TIVER DUVIDAS, RESPONDA APENAS \"NAO ENTENDI\" OU \"NAO POSSO FAZER\"\n\n8. IDIOMA: Responda em portugues brasileiro.\n\n9. SE A RESPOSTA FOR INCORRETA OU PERIGOSA, RESPONDA \"COMANDO INVALIDO\"";

    public static String queryLlm(String playerName, String playerInput) {
        return queryLlm(playerName, playerInput, DEFAULT_TIMEOUT_MS);
    }

    public static String queryLlm(String playerName, String playerInput, int timeoutMs) {
        AtomicInteger retryCount = new AtomicInteger(0);
        while (retryCount.get() < MAX_RETRIES) {
            try {
                return executeRequest(playerName, playerInput, timeoutMs);
            } catch (NetworkException e) {
                int currentRetry = retryCount.incrementAndGet();
                LOGGER.log(Level.WARNING, "Network error on attempt " + currentRetry + ": " + e.getMessage());
                if (currentRetry >= MAX_RETRIES) {
                    LOGGER.log(Level.SEVERE, "Max retries reached. Returning safe response.");
                    return "NAO CONSEGUI CONECTAR. TENTE NOVAMENTE.";
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_BACKOFF.toMillis() * currentRetry);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "ERRO INTERNO. TENTE NOVAMENTE.";
                }
            } catch (ResponseException e) {
                LOGGER.log(Level.SEVERE, "Response error: " + e.getMessage());
                return "ERRO DE RESPOSTA. TENTE NOVAMENTE.";
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error: " + e.getMessage(), e);
                return "ERRO INESPERADO. TENTE NOVAMENTE.";
            }
        }
        return "ERRO DE CONEXAO. TENTE NOVAMENTE.";
    }

    private static String executeRequest(String playerName, String playerInput, int timeoutMs) throws Exception {
        URL url = new URL(LMSTUDIO_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);

        String userText = "Jogador: " + playerName + " | Pedido: " + playerInput;

        String escapedSystem = escapeJson(SYSTEM_PROMPT);
        String escapedUser = escapeJson(userText);

        String jsonBody = String.format(
            "{\"model\":\"model\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":100,\"temperature\":0.0}",
            escapedSystem, escapedUser
        );

        LOGGER.fine("[McpBlock] Sending JSON: " + jsonBody);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return parseOpenAiResponse(response.toString());
            }
        } else {
            throw new NetworkException("LMStudio returned: " + responseCode);
        }
    }

    private static String parseOpenAiResponse(String response) throws ResponseException {
        try {
            LOGGER.fine("[McpBlock] Raw response: " + response.substring(0, Math.min(500, response.length())));

            // Parse JSON using simple string operations with validation
            int contentLabelPos = response.indexOf("\"content\"");
            if (contentLabelPos == -1) {
                throw new ResponseException("Missing content field in response");
            }

            int colonPos = response.indexOf(":", contentLabelPos);
            if (colonPos == -1) {
                throw new ResponseException("Invalid content field format");
            }

            int startQuote = response.indexOf("\"", colonPos);
            if (startQuote == -1) {
                throw new ResponseException("Content value not found");
            }

            StringBuilder content = new StringBuilder();
            int i = startQuote + 1;

            while (i < response.length()) {
                char c = response.charAt(i);
                if (c == '\\') {
                    if (i + 1 < response.length()) {
                        char esc = response.charAt(i + 1);
                        if (esc == 'n') content.append('\n');
                        else if (esc == 't') content.append('\t');
                        else if (esc == 'r') content.append('\r');
                        else if (esc == '\\') content.append('\\');
                        else if (esc == '"') content.append('"');
                        else content.append(esc);
                        i += 2;
                    } else {
                        i++;
                    }
                } else if (c == '"') {
                    break;
                } else {
                    content.append(c);
                    i++;
                }
            }

            String result = content.toString().trim();

            // Extract only the first line (the command)
            int newlinePos = result.indexOf('\n');
            if (newlinePos != -1) {
                result = result.substring(0, newlinePos).trim();
            }

            // Validate the response content
            if (!isValidResponse(result)) {
                throw new ResponseException("Invalid response content");
            }

            LOGGER.fine("[McpBlock] Parsed content: " + result);
            return result;

        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Parse error: " + e.getMessage(), e);
            throw new ResponseException("Failed to parse response");
        }
    }

    private static boolean isValidResponse(String response) {
        // Basic validation to prevent hallucinations
        if (response == null || response.trim().isEmpty()) {
            return false;
        }

        // Check for dangerous patterns
        String lowerResponse = response.toLowerCase();
        if (lowerResponse.contains("@p") || lowerResponse.contains("@a") ||
            lowerResponse.contains("@s") || lowerResponse.contains("@e") ||
            lowerResponse.contains("@r")) {
            return false;
        }

        // Check for suspicious content
        if (lowerResponse.contains("crash") || lowerResponse.contains("lag") ||
            lowerResponse.contains("delete") || lowerResponse.contains("remove") ||
            lowerResponse.contains("ban") || lowerResponse.contains("kick")) {
            return false;
        }

        // Check for non-command responses when CMD: is expected
        if (response.startsWith("CMD:") && !response.matches("CMD:\\s*(give|summon)\\s.*")) {
            return false;
        }

        return true;
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", " ")
                   .replace("\r", " ")
                   .replace("\t", " ");
    }

    // Custom exceptions for better error handling
    private static class NetworkException extends Exception {
        public NetworkException(String message) {
            super(message);
        }
    }

    private static class ResponseException extends Exception {
        public ResponseException(String message) {
            super(message);
        }
    }
}