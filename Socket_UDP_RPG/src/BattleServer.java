import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class BattleServer {
    private static final int SERVER_PORT = 9876;
    private static final int BUFFER_SIZE = 1024;
    private static final int ENEMY_DAMAGE = 20;
    private static Map<String, Integer> playersHP = new HashMap<>();
    private static Map<String, Boolean> playersDefending = new HashMap<>();
    private static Map<String, Integer> playersDamageBoost = new HashMap<>();
    private static List<String> playerOrder = new ArrayList<>();
    private static Map<String, InetAddress> playerAddresses = new HashMap<>();
    private static Map<String, Integer> playerPorts = new HashMap<>();
    private static Map<String, Boolean> playerReady = new HashMap<>();
    private static Map<String, String> playerNames = new HashMap<>();  // Nome do jogador associado ao clientKey
    private static int currentPlayerIndex = 0;
    private static int enemyHP = 1000;
    private static boolean gameStarted = false;

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            byte[] receiveData = new byte[BUFFER_SIZE];

            System.out.println("Servidor de Batalha iniciado. Aguardando jogadores...");

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientKey = clientAddress.toString() + ":" + clientPort;

                System.out.println("Recebido de " + clientKey + ": " + message);

                // Mapeamento do jogador
                if (!playerAddresses.containsKey(clientKey)) {
                    playerAddresses.put(clientKey, clientAddress);
                    playerPorts.put(clientKey, clientPort);
                    playersHP.put(clientKey, 100);
                    playersDefending.put(clientKey, false);
                    playersDamageBoost.put(clientKey, 0);
                    playerReady.put(clientKey, false);
                    playerOrder.add(clientKey);
                }

                String response = handleClientMessage(message, clientKey);

                // Enviar resposta para todos os jogadores
                for (String player : playerOrder) {
                    InetAddress address = playerAddresses.get(player);
                    int port = playerPorts.get(player);
                    byte[] sendData = response.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
                    serverSocket.send(sendPacket);
                }

                // Reiniciar o jogo se todos os jogadores ou o inimigo foram derrotados
                if (enemyHP <= 0 || playersHP.isEmpty()) {
                    System.out.println("Fim do jogo. Reiniciando servidor...");
                    resetGame();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resetGame() {
        playersHP.clear();
        playersDefending.clear();
        playersDamageBoost.clear();
        playerOrder.clear();
        playerAddresses.clear();
        playerPorts.clear();
        playerReady.clear();
        playerNames.clear();  // Resetar nomes
        currentPlayerIndex = 0;
        enemyHP = 100;
        gameStarted = false;
        System.out.println("Jogo reiniciado.");
    }

    private static String handleClientMessage(String message, String clientKey) {
        // Extraia o comando correto, ignorando o clientKey
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            return "Comando inválido.";
        }
        message = parts[1];

        // Verificar se o jogador já forneceu um nome
        if (!playerNames.containsKey(clientKey)) {
            playerNames.put(clientKey, message);  // Armazena o nome do jogador
            return "Nome registrado: " + message + ". Agora você pode dar READY.";
        }

        // Checar se o comando é READY
        if (message.contains("READY")) {
            playerReady.put(clientKey, true);
            if (playerReady.values().stream().allMatch(Boolean::booleanValue)) {
                gameStarted = true;
                return "Todos os jogadores estão prontos! A batalha começou!";
            } else {
                return "Esperando todos os jogadores ficarem prontos...";
            }
        }

        if (!gameStarted) {
            return "Aguardando todos os jogadores ficarem prontos...";
        }

        if (!playerOrder.get(currentPlayerIndex).equals(clientKey)) {
            return "Não é o seu turno.";
        }

        String response = "";

        if (message.startsWith("ATTACK")) {
            String[] attackParts = message.split(" ");
            int minDamage = Integer.parseInt(attackParts[1]);
            int maxDamage = Integer.parseInt(attackParts[2]);
            int damage = minDamage + (int) (Math.random() * ((maxDamage - minDamage) + 1));
            damage += playersDamageBoost.get(clientKey);
            enemyHP -= damage;
            response = playerNames.get(clientKey) + " causou " + damage + " de dano ao inimigo! HP do inimigo: " + enemyHP;

            if (enemyHP <= 0) {
                response += "\nOs jogadores venceram!";
                endGame("Os jogadores venceram!");
                return response;
            }

            playersDamageBoost.put(clientKey, 0);
        } else if (message.equals("DEFEND")) {
            playersDefending.put(clientKey, true);
            response = playerNames.get(clientKey) + " está defendendo e reduzirá o dano recebido no próximo ataque.";
        } else if (message.startsWith("HEAL")) {
            String[] healParts = message.split(" ");
            String targetName = healParts[1];
            String targetPlayer = getClientKeyByName(targetName);
            if (targetPlayer != null) {
                int healAmount = 30;
                int newHP = Math.min(playersHP.get(targetPlayer) + healAmount, 100);
                playersHP.put(targetPlayer, newHP);
                response = playerNames.get(clientKey) + " curou " + targetName + " em " + healAmount + " HP. HP atual de " + targetName + ": " + newHP;
            } else {
                response = "Jogador alvo inválido para cura.";
            }
        } else if (message.startsWith("BOOST")) {
            String[] boostParts = message.split(" ");
            String targetName = boostParts[1];
            String targetPlayer = getClientKeyByName(targetName);
            if (targetPlayer != null) {
                playersDamageBoost.put(targetPlayer, 10);
                response = playerNames.get(clientKey) + " aumentou o dano de " + targetName + " para o próximo ataque.";
            } else {
                response = "Jogador alvo inválido para aumento de dano.";
            }
        } else {
            return "Comando desconhecido.";
        }

        // Passa para o próximo jogador ou realiza o turno do inimigo
        currentPlayerIndex = (currentPlayerIndex + 1) % playerOrder.size();
        if (currentPlayerIndex == 0) {
            response += "\nAgora é o turno do inimigo.";
            response += enemyTurn();
        } else {
            response += "\nAgora é a vez de: " + playerNames.get(playerOrder.get(currentPlayerIndex));
        }

        response += "\nJogadores disponíveis:";
        for (String playerKey : playerOrder) {
            response += "\n" + playerNames.get(playerKey) + " (HP: " + playersHP.get(playerKey) + ")";
        }

        return response;
    }

    private static String enemyTurn() {
        if (playersHP.isEmpty()) return "O inimigo venceu.";

        int targetIndex = (int) (Math.random() * playersHP.size());
        String targetPlayer = playerOrder.get(targetIndex);

        Random rand = new Random();
        int damage = rand.nextInt(15) + 1;
        if (playersDefending.get(targetPlayer)) {
            damage *= 0.2;
            playersDefending.put(targetPlayer, false);
        }

        int newHP = playersHP.get(targetPlayer) - damage;
        playersHP.put(targetPlayer, newHP);

        String response = "\nO inimigo atacou " + playerNames.get(targetPlayer) + " causando " + damage + " de dano. HP de " + playerNames.get(targetPlayer) + ": " + newHP;

        if (newHP <= 0) {
            response += "\n" + playerNames.get(targetPlayer) + " foi derrotado!";
            playersHP.remove(targetPlayer);
            playersDefending.remove(targetPlayer);
            playersDamageBoost.remove(targetPlayer);
            playerOrder.remove(targetPlayer);
        }

        if (playersHP.isEmpty()) {
            response += "\nO inimigo venceu.";
            endGame("O inimigo venceu.");
        }

        return response;
    }

    private static String getClientKeyByName(String playerName) {
        for (Map.Entry<String, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void endGame(String message) {
        try (DatagramSocket serverSocket = new DatagramSocket()) {
            for (String player : playerOrder) {
                InetAddress address = playerAddresses.get(player);
                int port = playerPorts.get(player);
                byte[] sendData = ("END " + message).getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
                serverSocket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}