# Intercom_Maythayus1
Building a Radio Intercom with Android and Raspberry Pi Pico


markdown
# Radio Intercom System
A professional-grade radio intercom system that connects an Android device to a Raspberry Pi Pico with DRA818V radio module for reliable two-way communication.
## Features
- **Push-to-Talk (PTT) Functionality**: Instant voice communication with press-and-hold button
- **Multi-Channel Support**: Switch between 16 different channels
- **Real-time Status**: Visual feedback for connection and transmission status
- **USB Communication**: Secure and fast communication with Raspberry Pi Pico
- **Modern UI**: Clean, intuitive interface following Material Design guidelines
- **Low Latency**: Optimized for real-time audio transmission
## Requirements
### Hardware
- Android device (Android 8.0+)
- Raspberry Pi Pico
- DRA818V Radio Module
- VHF/UHF Antenna with SMA connector
- 5V Power Supply
- USB OTG Cable
### Software
- Android Studio (latest version)
- MicroPython (for Raspberry Pi Pico)
- Python 3.7+
## Installation
### For Android App
1. Clone this repository
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Build and run on your Android device
### For Raspberry Pi Pico
1. Install MicroPython on your Pico
2. Upload the `pico/` directory contents to your Pico
3. Connect the Pico to your Android device via USB OTG
## Wiring Guide
| Pico Pin | DRA818V Pin | Connection |
|----------|-------------|------------|
| 3.3V/5V  | VCC (Pin 1) | Power      |
| GND      | GND (Pin 2) | Ground     |
| GP4 (TX) | RXD (Pin 3) | Data TX    |
| GP5 (RX) | TXD (Pin 4) | Data RX    |
| GP1      | PTT (Pin 5) | PTT Control|
## Usage
1. Connect your Android device to the Raspberry Pi Pico via USB OTG
2. Grant USB permissions when prompted
3. Select your desired channel
4. Press and hold the PTT button to transmit
5. Release to listen
## Building from Source
1. Ensure you have the latest Android Studio and SDK tools
2. Clone this repository
3. Open the project in Android Studio
4. Let Gradle sync complete
5. Build the project (Build > Make Project)
6. Run on your device or emulator
## Dependencies
- AndroidX Core KTX
- AndroidX AppCompat
- Material Design Components
- AndroidX ConstraintLayout
- USB Serial for Android
- AndroidX Lifecycle Components
- Kotlin Coroutines
## License
MIT License

Copyright (c) 2025 Your Name

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 
## Contributing
 
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
 
## Support
 
For support, please open an issue on the GitHub repository.
 
## Acknowledgments
 
- MicroPython Team
- Android Open Source Project
- All contributors and testers
 
---
 
*This project was developed as an open-source solution for amateur radio communication.*
To use this README:

Create a new file named README.md in your project's root directory
Copy and paste the entire content above into the file
Save the file
This README provides comprehensive documentation for your project, including:

Project overview and features
Hardware and software requirements
Installation instructions
Wiring guide
Usage instructions
Build instructions
Dependencies
License information
Contribution guidelines


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
    
    utime.sleep_ms(10)  # Réduit la charge CPU​
