import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.rmi.Naming;

public class BattleServer extends UnicastRemoteObject implements BattleServerInterface {

    private static final int ENEMY_HP = 1000;
    private static final int ENEMY_DAMAGE = 20;
    private static Map<String, Integer> playersHP = new HashMap<>();
    private static Map<String, String> playerNames = new HashMap<>();
    private static Map<String, Boolean> playersDefending = new HashMap<>();
    private static Map<String, Integer> playersDamageBoost = new HashMap<>();
    private static int enemyHP = ENEMY_HP;
    private static List<BattleClientInterface> clients = new ArrayList<>();  // Lista de clientes conectados
    private static List<String> playerOrder = new ArrayList<>(); // Lista para manter a ordem dos jogadores

    protected BattleServer() throws RemoteException {
        super();
    }

    // Registra o jogador e o cliente no servidor
    @Override
    public String registerPlayer(String playerName, BattleClientInterface client) throws RemoteException {
        String clientKey = UUID.randomUUID().toString();
        playerNames.put(clientKey, playerName);
        playersHP.put(clientKey, 100);
        playersDefending.put(clientKey, false);
        playersDamageBoost.put(clientKey, 0);
        clients.add(client);  // Adiciona o cliente à lista de clientes conectados
        playerOrder.add(clientKey);  // Adiciona o jogador à ordem de turnos
        return "Jogador registrado: " + playerName + " com chave: " + clientKey;
    }

    @Override
    public String playerAction(String clientKey, String action) throws RemoteException {
        String response = "";
        // Lógica de ação de ataque, cura, boost, etc.
        if (action.startsWith("ATTACK")) {
            int damage = 20 + playersDamageBoost.get(clientKey);
            enemyHP -= damage;
            playersDamageBoost.put(clientKey, 0); // Reseta o boost de dano após o ataque
            response = playerNames.get(clientKey) + " causou " + damage + " de dano! HP do inimigo agora é: " + enemyHP;
        } else if (action.equals("DEFEND")) {
            playersDefending.put(clientKey, true);  // Jogador está defendendo
            response = playerNames.get(clientKey) + " está defendendo!";
        } else if (action.startsWith("HEAL")) {
            String[] parts = action.split(" ");
            String targetPlayerName = parts[1];
            String targetKey = getClientKeyByName(targetPlayerName);
            if (targetKey != null) {
                int newHP = Math.min(playersHP.get(targetKey) + 30, 100);  // Cura de 30 HP
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
                playersDamageBoost.put(targetKey, 10);  // Boost de 10 para o próximo ataque
                response = playerNames.get(clientKey) + " aumentou o dano de " + targetPlayerName + "!";
            } else {
                response = "Jogador alvo inválido.";
            }
        }

        // O inimigo ataca depois da ação do jogador
        response += "\n" + enemyAttack();

        // Atualiza todos os clientes após a ação
        notifyClients();
        return response;
    }

    // Método para o inimigo atacar um jogador aleatório
    private String enemyAttack() throws RemoteException {
        if (playersHP.isEmpty()) return "O inimigo venceu. Todos os jogadores foram derrotados.";

        Random rand = new Random();
        int targetIndex = rand.nextInt(playersHP.size());  // Selecionar jogador aleatório
        String targetKey = playerOrder.get(targetIndex);

        int damage = ENEMY_DAMAGE;
        if (playersDefending.get(targetKey)) {
            damage *= 0.5;  // Reduz dano pela metade se o jogador estiver defendendo
            playersDefending.put(targetKey, false);  // Reseta defesa após o ataque
        }

        int newHP = playersHP.get(targetKey) - damage;
        playersHP.put(targetKey, newHP);

        String response = "O inimigo atacou " + playerNames.get(targetKey) + " causando " + damage + " de dano! HP restante: " + newHP;

        if (newHP <= 0) {
            response += "\n" + playerNames.get(targetKey) + " foi derrotado!";
            playersHP.remove(targetKey);  // Remove o jogador morto
            playerOrder.remove(targetKey);
        }

        return response;
    }

    // Método que notifica todos os clientes conectados
    private void notifyClients() throws RemoteException {
        String gameStatus = getGameStatus();
        for (BattleClientInterface client : clients) {
            client.updateGameStatus(gameStatus);  // Chama o método updateGameStatus de cada cliente
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
        return status.toString();
    }

    // Função auxiliar para obter o clientKey por nome do jogador
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
