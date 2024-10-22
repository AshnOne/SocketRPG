from flask import Flask, request, jsonify
import random
import threading
import time

app = Flask(__name__)

# Definição dos atributos do inimigo
enemy = {
    'name': 'Dragão Antigo',
    'hp': 300,
    'ac': 15,
    'initiative': 0
}

# Definição dos jogadores (base inicial)
players = {
    'player1': {'class': 'Guerreiro', 'hp': 100, 'ac': 18, 'str': 16, 'dex': 12, 'int': 10, 'initiative': 0, 'actions': []},
    'player2': {'class': 'Mago', 'hp': 60, 'ac': 12, 'str': 8, 'dex': 14, 'int': 18, 'initiative': 0, 'actions': []},
}

initiative_order = []
battle_state = {
    'turn': 0,
    'log': [],
    'current_turn_player': None  # Jogador ou inimigo que deve jogar
}

def roll_d20():
    """Rola um dado de 20 lados"""
    return random.randint(1, 20)

@app.route('/start_battle', methods=['POST'])
def start_battle():
    global initiative_order

    for player, stats in players.items():
        initiative = roll_d20() + (stats['dex'] // 2) - 5
        players[player]['initiative'] = initiative
        battle_state['log'].append(f"{player} rolou {initiative} de iniciativa.")

    enemy['initiative'] = roll_d20()
    battle_state['log'].append(f"{enemy['name']} rolou {enemy['initiative']} de iniciativa.")

    initiative_order = sorted(
        [(player, stats['initiative']) for player, stats in players.items()] + [('enemy', enemy['initiative'])],
        key=lambda x: x[1], reverse=True
    )

    battle_state['current_turn_player'] = initiative_order[0][0]
    battle_state['log'].append(f"{battle_state['current_turn_player']} começa a batalha!")

    return jsonify({'initiative_order': initiative_order, 'battle_state': battle_state})

@app.route('/status', methods=['GET'])
def get_status():
    return jsonify({
        'battle': battle_state,
        'enemy': enemy,
        'players': players
    })

@app.route('/turn', methods=['POST'])
def process_turn():
    data = request.get_json()
    player = data.get('player')
    action = data.get('action')

    if player != battle_state['current_turn_player']:
        return jsonify({'error': 'Não é o turno do jogador'}), 400

    if player in players:
        result = calculate_attack(player, enemy)
        battle_state['log'].append(result)

        if enemy['hp'] <= 0:
            battle_state['log'].append(f"{enemy['name']} foi derrotado!")
        else:
            if next_turn_is_enemy():
                threading.Timer(2.0, enemy_action).start()

    advance_turn()
    return jsonify({
        'battle': battle_state,
        'enemy': enemy,
        'players': players
    })

def calculate_attack(player, target):
    if players[player]['class'] == 'Guerreiro':
        attack_roll = roll_d20() + (players[player]['str'] // 2) - 5
    elif players[player]['class'] == 'Mago':
        attack_roll = roll_d20() + (players[player]['int'] // 2) - 5

    if attack_roll >= target['ac']:
        if players[player]['class'] == 'Guerreiro':
            damage = random.randint(5, 12)
        elif players[player]['class'] == 'Mago':
            damage = random.randint(8, 20)

        target['hp'] -= damage
        return f"{player} acertou {target['name']} causando {damage} de dano!"
    else:
        return f"{player} errou o ataque contra {target['name']}."

def enemy_action():
    target = random.choice(list(players.keys()))
    enemy_attack_roll = roll_d20() + 7
    if enemy_attack_roll >= players[target]['ac']:
        damage = random.randint(10, 25)
        players[target]['hp'] -= damage
        battle_state['log'].append(f"{enemy['name']} atacou {target} causando {damage} de dano.")
    else:
        battle_state['log'].append(f"{enemy['name']} errou o ataque contra {target}.")

    if all(player['hp'] <= 0 for player in players.values()):
        battle_state['log'].append("Todos os jogadores foram derrotados!")

    advance_turn()

def next_turn_is_enemy():
    next_turn = (battle_state['turn'] + 1) % len(initiative_order)
    return initiative_order[next_turn][0] == 'enemy'

def advance_turn():
    battle_state['turn'] += 1
    next_turn = battle_state['turn'] % len(initiative_order)
    battle_state['current_turn_player'] = initiative_order[next_turn][0]
    battle_state['log'].append(f"Agora é a vez de {battle_state['current_turn_player']}!")

if __name__ == '__main__':
    app.run(debug=True)
