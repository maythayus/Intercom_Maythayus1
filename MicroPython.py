# main.py - Configuration pour Pico avec câblage direct
from machine import UART, Pin
import utime

# Configuration UART
uart = UART(0, baudrate=9600, tx=Pin(0), rx=Pin(1))  # GP0=TX, GP1=RX

# Variables d'état
current_channel = 1
is_transmitting = False

def send_response(response):
    """Envoie une réponse formatée"""
    try:
        uart.write(f"{response}\n")
        return True
    except:
        return False

def process_command(cmd):
    """Traite les commandes reçues"""
    global current_channel, is_transmitting
    
    cmd = cmd.strip()
    print("RX:", cmd)  # Debug
    
    try:
        if cmd == "PTT_ON":
            is_transmitting = True
            # Activer la transmission radio ici
            send_response("PTT_ACK")
            
        elif cmd == "PTT_OFF":
            is_transmitting = False
            # Désactiver la transmission radio ici
            send_response("PTT_ACK")
            
        elif cmd.startswith("CH"):
            try:
                ch = int(cmd[2:])
                if 1 <= ch <= 16:
                    current_channel = ch
                    # Configurer le canal radio ici
                    send_response(f"CH{ch:02d}_OK")
            except:
                send_response("CH_ERR")
                
        elif cmd == "PING":
            send_response("PONG")
            
    except Exception as e:
        print("Erreur:", e)
        send_response("CMD_ERR")

# Initialisation
print("Démarrage du système...")
print("En attente de commandes...")

# Boucle principale
while True:
    try:
        if uart.any():
            cmd = uart.readline()
            if cmd:
                process_command(cmd.decode('utf-8', 'ignore'))
    except Exception as e:
        print("Erreur lecture UART:", e)
    
    utime.sleep_ms(10)  # Réduit la charge CPU