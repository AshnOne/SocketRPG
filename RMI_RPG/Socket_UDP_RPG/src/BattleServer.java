import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.rmi.Naming;

public class BattleServer extends UnicastRemoteObject implements BattleServerInterface {

    private static final int ENEMY_HP = 1000;
    private static final int ENEMY_DAMAGE = 10;
    private static Map<String, Integer> playersHP = new HashMap<>();
    private static Map<String, String> playerNames = new HashMap<>();
    private static Map<String, Boolean> playersDefending = new HashMap<>();
    private static Map<String, Integer> playersDamageBoost = new HashMap<>();
    private static int enemyHP = ENEMY_HP;
    private static List<BattleClientInterface> clients = new ArrayList<>();
    private static List<String> playerOrder = new ArrayList<>();
    private static int currentPlayerIndex = 0;
    private static StringBuilder actionLog = new StringBuilder();  // Para manter um log das ações

    protected BattleServer() throws RemoteException {
        super();
    }

    @Override
    public String registerPlayer(String playerName, BattleClientInterface client) throws RemoteException {
        String clientKey = UUID.randomUUID().toString();
        playerNames.put(clientKey, playerName);
        playersHP.put(clientKey, 100);
        playersDefending.put(clientKey, false);
        playersDamageBoost.put(clientKey, 0);
        clients.add(client);
        playerOrder.add(clientKey);
        return "Jogador registrado: " + playerName + " com chave: " + clientKey;
    }
    @Override
    public String playerAction(String clientKey, String action) throws RemoteException {
        String response = "";

        // Verifica se é o turno do jogador
        if (!playerOrder.get(currentPlayerIndex).equals(clientKey)) {
            return "Não é o seu turno!";
        }

        // Limpa o log para o novo turno
        actionLog.setLength(0);

        // Lógica de ações
        if (action.startsWith("ATTACK")) {
            int damage = 20 + playersDamageBoost.get(clientKey);
            enemyHP -= damage;
            playersDamageBoost.put(clientKey, 0);
            response = playerNames.get(clientKey) + " causou " + damage + " de dano! HP do inimigo agora é: " + enemyHP;
        } else if (action.equals("DEFEND")) {
            playersDefending.put(clientKey, true);
            response = playerNames.get(clientKey) + " está defendendo!";
        } else if (action.startsWith("HEAL")) {
            String[] parts = action.split(" ");
            String targetPlayerName = parts[1];
            String targetKey = getClientKeyByName(targetPlayerName);
            if (targetKey != null) {
                int newHP = Math.min(playersHP.get(targetKey) + 30, 100);
                playersHP.put(targetKey, newHP);
                response = playerNames.get(clientKey) + " curou " + targetPlayerName + " para " + newHP + " HP.";
            } else {
                response = "Jogador alvo inválido.";
            }
        } else if (action.startsWith("BOOST")) {
            String[] parts = action.split(" ");
            String targetPlayerName = parts[1];
            String targetKey = getClientKeyByName(targetPlayerName);
            if (targetKey != null) {
                playersDamageBoost.put(targetKey, 10);
                response = playerNames.get(clientKey) + " aumentou o dano de " + targetPlayerName + "!";
            } else {
                response = "Jogador alvo inválido.";
            }
        }

        // Atualiza o log de ações apenas para este turno
        actionLog.append(response).append("\n");

        // O inimigo ataca após a ação do jogador
        response += "\n" + enemyAttack();

        // Passa o turno para o próximo jogador
        currentPlayerIndex = (currentPlayerIndex + 1) % playerOrder.size();

        // Atualiza todos os clientes após a ação
        notifyClients();
        return response;
    }

    private String enemyAttack() throws RemoteException {
        if (playersHP.isEmpty()) return "O inimigo venceu. Todos os jogadores foram derrotados.";

        Random rand = new Random();
        int targetIndex = rand.nextInt(playersHP.size());
        String targetKey = playerOrder.get(targetIndex);

        int damage = ENEMY_DAMAGE;
        if (playersDefending.get(targetKey)) {
            damage *= 0.5;
            playersDefending.put(targetKey, false);
        }

        int newHP = playersHP.get(targetKey) - damage;
        playersHP.put(targetKey, newHP);

        String response = "O inimigo atacou " + playerNames.get(targetKey) + " causando " + damage + " de dano! HP restante: " + newHP;

        if (newHP <= 0) {
            response += "\n" + playerNames.get(targetKey) + " foi derrotado!";
            playersHP.remove(targetKey);
            playerOrder.remove(targetKey);
            if (currentPlayerIndex >= playerOrder.size()) {
                currentPlayerIndex = 0;  // Ajusta o índice se necessário
            }
        }

        actionLog.append(response).append("\n");

        return response;
    }

    private void notifyClients() throws RemoteException {
        String gameStatus = getGameStatus();
        String currentPlayerKey = playerOrder.get(currentPlayerIndex); // Jogador atual (que jogará no próximo turno)

        for (int i = 0; i < clients.size(); i++) {
            BattleClientInterface client = clients.get(i);
            String playerKey = playerOrder.get(i);

            // Não notifica o jogador que acabou de jogar
            if (!playerKey.equals(currentPlayerKey)) {
                client.updateGameStatus(gameStatus);
            }
        }
    }

    @Override
    public String getGameStatus() throws RemoteException {
        StringBuilder status = new StringBuilder();
        status.append("Inimigo HP: ").append(enemyHP).append("\n");
        for (Map.Entry<String, Integer> entry : playersHP.entrySet()) {
            String playerKey = entry.getKey();
            status.append(playerNames.get(playerKey)).append(" (HP: ").append(entry.getValue()).append(")\n");
        }
        status.append("\nAção do turno atual:\n").append(actionLog.toString());
        return status.toString();
    }

    private String getClientKeyByName(String playerName) {
        for (Map.Entry<String, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            BattleServer server = new BattleServer();
            Naming.rebind("rmi://localhost/BattleServer", server);
            System.out.println("Servidor RMI de batalha iniciado...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
