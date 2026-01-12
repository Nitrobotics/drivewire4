# jSerialComm Migration - nrjavaserial to jSerialComm

## Summary
This migration replaces nrjavaserial (which lacks macOS aarch64 support) with jSerialComm 2.10.4,
which has full native support for macOS Apple Silicon (M1/M2/M3).

## Changes Made

### pom.xml
- Replaced nrjavaserial 5.2.1 dependency with jSerialComm 2.10.4

### New Exception Classes (for compatibility)
- `DWPortInUseException` - replaces gnu.io.PortInUseException
- `DWUnsupportedCommOperationException` - replaces gnu.io.UnsupportedCommOperationException
- (DWPortNotValidException already existed)

### Modified Files

1. **DWSerialDevice.java** - Core serial device implementation
   - Import: `com.fazecast.jSerialComm.SerialPort` instead of gnu.io.*
   - `SerialPort.getCommPort(name)` instead of `CommPortIdentifier.getPortIdentifier(name)`
   - `port.openPort()` / `port.closePort()` instead of `port.open()` / `port.close()`
   - `port.setComPortParameters()` instead of `port.setSerialPortParams()`
   - `port.getSystemPortName()` instead of `port.getName()`
   - Flow control: `FLOW_CONTROL_*` constants instead of `FLOWCONTROL_*`
   - Parity: `NO_PARITY`, `EVEN_PARITY`, etc. instead of `PARITY_*`
   - Stop bits: `ONE_STOP_BIT`, `TWO_STOP_BITS`, etc. instead of `STOPBITS_*`
   - DTR/RTS: `setDTR()`/`clearDTR()` instead of `setDTR(boolean)`

2. **DWSerialReader.java** - Serial port event listener
   - Implements `SerialPortDataListener` instead of `SerialPortEventListener`
   - Added `getListeningEvents()` method returning `SerialPort.LISTENING_EVENT_DATA_AVAILABLE`
   - Uses `addDataListener()` instead of `addEventListener()`

3. **DriveWireServer.java** - Main server class
   - `getAvailableSerialPorts()`: Uses `SerialPort.getCommPorts()` array instead of enumeration
   - `getSerialPortStatus()`: Uses `SerialPort.getCommPort(name)` and `openPort()`/`closePort()`
   - `checkRXTXLoaded()`: Simplified to just verify jSerialComm works

4. **DWCmdServerShowSerial.java** - Show serial devices command
   - Complete rewrite using jSerialComm API
   - Uses `SerialPort.getCommPorts()` for enumeration

5. **DWUtils.java** - Utility class
   - `getPortNames()`: Uses jSerialComm's `SerialPort.getCommPorts()` array

6. **DWAPISerial.java** - Serial API for virtual serial ports
   - Complete rewrite using jSerialComm API
   - Updated constant names for parity, stop bits, flow control

7. **DWAPISerialPortDef.java** - Serial port definition
   - Updated for jSerialComm methods (`setComPortParameters()`, `setFlowControl()`)

8. **DWProtocolHandler.java, VModemProtocolHandler.java, MCXProtocolHandler.java**
   - Updated imports to use DW exception classes

9. **DWCmdServerTurbo.java**
   - Updated import to use DWUnsupportedCommOperationException

## API Mapping Reference

| nrjavaserial (gnu.io)                | jSerialComm                          |
|--------------------------------------|--------------------------------------|
| CommPortIdentifier.getPortIdentifiers() | SerialPort.getCommPorts()         |
| CommPortIdentifier.getPortIdentifier(name) | SerialPort.getCommPort(name)    |
| portId.open(appName, timeout)        | port.openPort()                      |
| port.close()                         | port.closePort()                     |
| port.setSerialPortParams(...)        | port.setComPortParameters(...)       |
| port.setFlowControlMode(...)         | port.setFlowControl(...)             |
| port.getName()                       | port.getSystemPortName()             |
| port.getDataBits()                   | port.getNumDataBits()                |
| port.getStopBits()                   | port.getNumStopBits()                |
| port.getFlowControlMode()            | port.getFlowControlSettings()        |
| SerialPort.STOPBITS_1                | SerialPort.ONE_STOP_BIT              |
| SerialPort.STOPBITS_2                | SerialPort.TWO_STOP_BITS             |
| SerialPort.STOPBITS_1_5              | SerialPort.ONE_POINT_FIVE_STOP_BITS  |
| SerialPort.PARITY_NONE               | SerialPort.NO_PARITY                 |
| SerialPort.PARITY_EVEN               | SerialPort.EVEN_PARITY               |
| SerialPort.PARITY_ODD                | SerialPort.ODD_PARITY                |
| SerialPort.FLOWCONTROL_NONE          | SerialPort.FLOW_CONTROL_DISABLED     |
| SerialPort.FLOWCONTROL_RTSCTS_IN     | SerialPort.FLOW_CONTROL_RTS_ENABLED  |
| SerialPort.FLOWCONTROL_RTSCTS_OUT    | SerialPort.FLOW_CONTROL_CTS_ENABLED  |
| port.setDTR(true)                    | port.setDTR()                        |
| port.setDTR(false)                   | port.clearDTR()                      |
| port.isCD()                          | port.getCD()                         |
| port.isCTS()                         | port.getCTS()                        |
| port.isDSR()                         | port.getDSR()                        |
| port.isDTR()                         | port.getDTR()                        |
| port.isRTS()                         | port.getRTS()                        |

## Testing
After building, test on macOS aarch64 (Apple Silicon) to verify:
1. Serial port enumeration works in wizard
2. Serial port connections work properly
3. All serial communication features function correctly
