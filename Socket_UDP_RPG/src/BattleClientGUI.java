import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
//oiii
public class BattleClientGUI {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9876;
    private static final int BUFFER_SIZE = 1024;
    private static DatagramSocket clientSocket;
    private static InetAddress IPAddress;
    private static String clientKey;
    private static JTextArea console;
    private static JButton readyButton, attackButton, defendButton, healButton, boostButton;
    private static JLabel enemyLabel, playerLabel;
    private static String playerName;
    private static Map<String, Integer> availablePlayers = new HashMap<>(); // Nome e HP dos jogadores

    public static void main(String[] args) {
        try {
            clientSocket = new DatagramSocket();
            IPAddress = InetAddress.getByName(SERVER_ADDRESS);
            clientKey = UUID.randomUUID().toString();

            JFrame frame = new JFrame("Battle Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);

            console = new JTextArea();
            console.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(console);
            frame.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridLayout(3, 2));

            readyButton = new JButton("READY");
            readyButton.setEnabled(false);  // Desativado até que o nome seja inserido
            readyButton.addActionListener(e -> sendMessage("READY"));
            buttonPanel.add(readyButton);

            attackButton = new JButton("ATTACK");
            attackButton.setEnabled(false);  // Desativado até que o nome seja inserido
            attackButton.addActionListener(e -> sendMessage("ATTACK 10 20"));
            buttonPanel.add(attackButton);

            defendButton = new JButton("DEFEND");
            defendButton.setEnabled(false);  // Desativado até que o nome seja inserido
            defendButton.addActionListener(e -> sendMessage("DEFEND"));
            buttonPanel.add(defendButton);

            healButton = new JButton("HEAL");
            healButton.setEnabled(false);  // Desativado até que o nome seja inserido
            healButton.addActionListener(e -> {
                String targetPlayer = showPlayerSelectionDialog(frame, "Selecione o jogador para curar:");
                if (targetPlayer != null && !targetPlayer.isEmpty()) {
                    sendMessage("HEAL " + targetPlayer);
                }
            });
            buttonPanel.add(healButton);

            boostButton = new JButton("BOOST");
            boostButton.setEnabled(false);  // Desativado até que o nome seja inserido
            boostButton.addActionListener(e -> {
                String targetPlayer = showPlayerSelectionDialog(frame, "Selecione o jogador para dar BOOST:");
                if (targetPlayer != null && !targetPlayer.isEmpty()) {
                    sendMessage("BOOST " + targetPlayer);
                }
            });
            buttonPanel.add(boostButton);

            enemyLabel = new JLabel("HP do Inimigo: 100");
            playerLabel = new JLabel("Jogadores: ");
            frame.add(enemyLabel, BorderLayout.NORTH);
            frame.add(playerLabel, BorderLayout.SOUTH);
            frame.add(buttonPanel, BorderLayout.EAST);

            frame.setVisible(true);

            // Solicita o nome do jogador
            playerName = JOptionPane.showInputDialog(frame, "Digite seu nome:");
            if (playerName != null && !playerName.isEmpty()) {
                sendMessage(playerName);
                readyButton.setEnabled(true);
                attackButton.setEnabled(true);
                defendButton.setEnabled(true);
                healButton.setEnabled(true);
                boostButton.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(frame, "Nome não pode ser vazio. Por favor, reinicie o jogo.");
                System.exit(0);
            }

            new Thread(() -> receiveMessages()).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(String message) {
        try {
            byte[] sendData = (clientKey + " " + message).getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT);
            clientSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void receiveMessages() {
        try {
            byte[] receiveData = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                console.append(response + "\n");

                if (response.startsWith("END")) {
                    showEndDialog(response.substring(4));
                    break;
                }

                // Atualizar HP do inimigo e jogadores com base na resposta do servidor
                if (response.contains("HP do inimigo:") || response.contains("Jogadores disponíveis:")) {
                    parseServerResponse(response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseServerResponse(String response) {
        String[] lines = response.split("\n");
        availablePlayers.clear(); // Limpar a lista antes de atualizar
        for (String line : lines) {
            if (line.contains("HP do inimigo:")) {
                enemyLabel.setText(line);
            } else if (line.contains("(HP:")) {
                String[] playerInfo = line.split(" \\(HP: ");
                String playerName = playerInfo[0].trim();
                int playerHP = Integer.parseInt(playerInfo[1].replace(")", "").trim());
                availablePlayers.put(playerName, playerHP);
            }
        }

        // Atualizar a label dos jogadores
        StringBuilder playerStatus = new StringBuilder("Jogadores disponíveis:");
        for (Map.Entry<String, Integer> entry : availablePlayers.entrySet()) {
            playerStatus.append("\n").append(entry.getKey()).append(" (HP: ").append(entry.getValue()).append(")");
        }
        playerLabel.setText(playerStatus.toString());
    }

    private static String showPlayerSelectionDialog(JFrame frame, String message) {
        String[] playersArray = availablePlayers.keySet().toArray(new String[0]);
        return (String) JOptionPane.showInputDialog(frame, message, "Seleção de Jogador", JOptionPane.PLAIN_MESSAGE, null, playersArray, playersArray[0]);
    }

    private static void showEndDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Fim do Jogo", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }
}
