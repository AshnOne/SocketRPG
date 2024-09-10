import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class BattleClientGUI extends UnicastRemoteObject implements BattleClientInterface {

    private static BattleServerInterface server;
    private static String clientKey;
    private static JTextArea console;

    // Construtor para o cliente
    protected BattleClientGUI() throws RemoteException {
        super();
    }

    // Método para receber as atualizações do servidor
    @Override
    public void updateGameStatus(String status) throws RemoteException {
        console.append(status + "\n");  // Atualiza o console do cliente com o novo status do jogo
    }

    public static void main(String[] args) {
        try {
            // Localizar o servidor no RMI registry
            server = (BattleServerInterface) Naming.lookup("rmi://localhost/BattleServer"); //substitua localhost pelo seu ip

            JFrame frame = new JFrame("Battle Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);

            console = new JTextArea();
            console.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(console);
            frame.add(scrollPane, BorderLayout.CENTER);

            // Registro do jogador
            String playerName = JOptionPane.showInputDialog(frame, "Digite seu nome:");
            if (playerName != null && !playerName.isEmpty()) {
                // Cria uma instância do cliente e se registra no servidor
                BattleClientGUI client = new BattleClientGUI();
                String response = server.registerPlayer(playerName, client);
                console.append(response + "\n");
                clientKey = response.split(": ")[2]; // Extrai o clientKey da resposta
            } else {
                JOptionPane.showMessageDialog(frame, "Nome não pode ser vazio. Por favor, reinicie o jogo.");
                System.exit(0);
            }

            // Botão de ataque
            JButton attackButton = new JButton("ATTACK");
            attackButton.addActionListener(e -> {
                try {
                    String actionResponse = server.playerAction(clientKey, "ATTACK");
                    console.append(actionResponse + "\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // Botão de defesa
            JButton defendButton = new JButton("DEFEND");
            defendButton.addActionListener(e -> {
                try {
                    String actionResponse = server.playerAction(clientKey, "DEFEND");
                    console.append(actionResponse + "\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // Botão de cura
            JButton healButton = new JButton("HEAL");
            healButton.addActionListener(e -> {
                String targetPlayer = JOptionPane.showInputDialog(frame, "Digite o nome do jogador para curar:");
                if (targetPlayer != null && !targetPlayer.isEmpty()) {
                    try {
                        String actionResponse = server.playerAction(clientKey, "HEAL " + targetPlayer);
                        console.append(actionResponse + "\n");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            // Botão de boost
            JButton boostButton = new JButton("BOOST");
            boostButton.addActionListener(e -> {
                String targetPlayer = JOptionPane.showInputDialog(frame, "Digite o nome do jogador para dar BOOST:");
                if (targetPlayer != null && !targetPlayer.isEmpty()) {
                    try {
                        String actionResponse = server.playerAction(clientKey, "BOOST " + targetPlayer);
                        console.append(actionResponse + "\n");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(attackButton);
            buttonPanel.add(defendButton);
            buttonPanel.add(healButton);
            buttonPanel.add(boostButton);

            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
