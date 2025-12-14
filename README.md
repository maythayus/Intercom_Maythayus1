# Intercom_Maythayus1
Building a Radio Intercom with Android and Raspberry Pi Pico
Complete Tutorial: Pico-Android Communication via UART
1. Required Components
Raspberry Pi Pico
USB-C/Micro-USB cable
DRA818V radio module (or any UART radio module)
Jumper wires (Dupont)
OTG-supported Android smartphone
2. Hardware Wiring
Pico	Radio Module
GP0 (TX)	RX
GP1 (RX)	TX
GND	GND
3.3V	VCC
3. MicroPython Installation
Download Firmware:
Visit micropython.org
Download the latest .uf2 file
Install Firmware:
Press and hold BOOTSEL, then connect Pico
A "RPI-RP2" drive appears
Drag and drop the .uf2 file
Pico will reboot automatically
4. MicroPython Script
Create a main.py file with this content:

python
from machine import UART, Pin
import utime
# UART Configuration
uart = UART(0, baudrate=9600, tx=Pin(0), rx=Pin(1))
def process_command(cmd):
    cmd = cmd.strip()
    print("Command:", cmd)
    
    if cmd == "PTT_ON":
        uart.write("PTT_ACK\n")
    elif cmd == "PTT_OFF":
        uart.write("PTT_ACK\n")
    elif cmd.startswith("CH"):
        try:
            ch = int(cmd[2:])
            if 1 <= ch <= 16:
                uart.write(f"CH{ch:02d}_OK\n")
        except:
            pass
    elif cmd == "PING":
        uart.write("PONG\n")
print("Pico Radio - Ready")
while True:
    if uart.any():
        try:
            cmd = uart.readline().decode('utf-8')
            if cmd:
                process_command(cmd)
        except:
            pass
    utime.sleep_ms(10)
5. Script Installation
Using Thonny IDE:
Install Thonny
Connect Pico
In Thonny: Tools > Options > Interpreter
Select "MicroPython (Raspberry Pi Pico)"
Port should be auto-detected
Copy-paste the code
File > Save as... > "Raspberry Pi Pico"
Name the file main.py
6. Android Setup
Option 1: Serial USB Terminal App
Install "Serial USB Terminal" from Play Store
Connect Pico using OTG cable
Configure:
Baud rate: 9600
Data bits: 8
Parity: None
Stop bits: 1
Option 2: Custom App
Create an Android app with this PTT button code:

java
// In your main activity
public class MainActivity extends AppCompatActivity {
    private UsbSerialPort usbSerialPort;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // PTT Button setup
        Button pttButton = findViewById(R.id.pttButton);
        pttButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand("PTT_ON");
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand("PTT_OFF");
                return true;
            }
            return false;
        });
    }
    private void sendCommand(String cmd) {
        if (usbSerialPort != null) {
            try {
                usbSerialPort.write((cmd + "\n").getBytes(), 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
7. Communication Test
Connect Pico to your phone
Open serial terminal app
Send test commands:
PING → Should reply "PONG"
CH1 → Should reply "CH01_OK"
PTT_ON → Activate transmission
PTT_OFF → Deactivate transmission
8. Troubleshooting
Pico Not Responding
Check RX/TX connections (must be crossed)
Verify ground connection
Ensure proper power supply
Corrupted Data
Verify baud rate matches (9600)
Check logic levels (3.3V for Pico)
Try reducing transmission speed
USB Connection Issues
Try different USB cable
Verify OTG mode is enabled on phone
Check USB permissions in Android settings
9. Possible Improvements
Error Handling:
python
def safe_send(cmd, max_retries=3):
    for attempt in range(max_retries):
        try:
            uart.write(cmd + "\n")
            return True
        except:
            utime.sleep_ms(100)
    return False
Logging:
python
def log(message):
    try:
        with open('log.txt', 'a') as f:
            f.write(f"{utime.time()}: {message}\n")
    except:
        pass
Channel Configuration:
python
channels = {
    1: {"freq": 144.390, "name": "Channel 1"},
    2: {"freq": 144.400, "name": "Channel 2"},
    # ... add more channels
}
10. Useful Resources
MicroPython Documentation
USB Serial for Android
Raspberry Pi Pico Documentation
