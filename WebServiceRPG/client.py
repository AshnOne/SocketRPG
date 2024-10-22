import tkinter as tk
import requests

player_name = "player1"  # Será alterado para cada cliente

def update_log(log_entries):
    """Atualiza o log de batalha"""
    log_text.delete(1.0, tk.END)
    for entry in log_entries:
        log_text.insert(tk.END, entry + '\n')
    log_text.yview(tk.END)

def update_status(players, enemy):
    """Atualiza o status dos jogadores e do inimigo"""
    status_text.delete(1.0, tk.END)
    for player, stats in players.items():
        status_text.insert(tk.END, f"{player} ({stats['class']}): {stats['hp']} HP\n")
    status_text.insert(tk.END, f"\nInimigo: {enemy['name']} - {enemy['hp']} HP")

def get_battle_status():
    """Faz uma requisição ao servidor para obter o estado atual da batalha"""
    response = requests.get('http://localhost:5000/status')
    if response.status_code == 200:
        data = response.json()
        battle_log = data['battle']['log']
        players = data['players']
        enemy = data['enemy']
        update_log(battle_log)
        update_status(players, enemy)

def send_player_action(action):
    """Envia uma ação do jogador ao servidor e atualiza o status"""
    response = requests.post('http://localhost:5000/turn', json={'player': player_name, 'action': action})
    if response.status_code == 200:
        get_battle_status()

# Configurando a interface gráfica
root = tk.Tk()
root.title(f"RPG de Turno - {player_name}")

# Frame para o log de batalha
log_frame = tk.Frame(root)
log_frame.pack(padx=10, pady=10)

scrollbar = tk.Scrollbar(log_frame)
scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

log_text = tk.Text(log_frame, height=20, width=60, yscrollcommand=scrollbar.set)
log_text.pack(side=tk.LEFT, fill=tk.BOTH)
scrollbar.config(command=log_text.yview)

# Frame para o status dos jogadores e do inimigo
status_frame = tk.Frame(root)
status_frame.pack(padx=10, pady=10)

status_text = tk.Text(status_frame, height=10, width=60)
status_text.pack()

# Botão para enviar a ação de ataque
action_button = tk.Button(root, text=f"{player_name} - Atacar", command=lambda: send_player_action('attack'))
action_button.pack(pady=5)

# Botão de atualização para o status da batalha
update_button = tk.Button(root, text="Atualizar Status", command=get_battle_status)
update_button.pack(pady=5)

# Inicia o cliente com o status atualizado
get_battle_status()

root.mainloop()
