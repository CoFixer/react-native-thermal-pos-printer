import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  Alert,
  StyleSheet,
} from 'react-native';
import ReactNativePosPrinter, {
  PrinterDevice,
  TextOptions,
} from 'react-native-thermal-pos-printer';

const App = () => {
  const [devices, setDevices] = useState<PrinterDevice[]>([]);
  const [connectedDevice, setConnectedDevice] = useState<PrinterDevice | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    initializePrinter();
  }, []);

  const initializePrinter = async () => {
    try {
      await ReactNativePosPrinter.init();
      loadDevices();
    } catch (error) {
      Alert.alert('Error', 'Failed to initialize printer');
    }
  };

  const loadDevices = async () => {
    try {
      const deviceList = await ReactNativePosPrinter.getDeviceList();
      setDevices(deviceList);
    } catch (error) {
      Alert.alert('Error', 'Failed to load devices');
    }
  };

  const connectToDevice = async (device: PrinterDevice) => {
    try {
      const success = await ReactNativePosPrinter.connectPrinter(
        device.address,
        device.type
      );
      if (success) {
        setConnectedDevice(device);
        setIsConnected(true);
        Alert.alert('Success', `Connected to ${device.name}`);
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to connect to device');
    }
  };

  const disconnect = async () => {
    try {
      await ReactNativePosPrinter.disconnectPrinter();
      setConnectedDevice(null);
      setIsConnected(false);
      Alert.alert('Success', 'Disconnected from printer');
    } catch (error) {
      Alert.alert('Error', 'Failed to disconnect');
    }
  };

  const printReceipt = async () => {
    if (!isConnected) {
      Alert.alert('Error', 'Please connect to a printer first');
      return;
    }

    try {
      // Print header
      const headerOptions: TextOptions = {
        align: 'CENTER',
        size: 'LARGE',
        bold: true,
      };
      await ReactNativePosPrinter.printText('RECEIPT', headerOptions);
      await ReactNativePosPrinter.printText('================================');
      
      // Print items
      await ReactNativePosPrinter.printText('Item 1                    $10.00');
      await ReactNativePosPrinter.printText('Item 2                    $15.00');
      await ReactNativePosPrinter.printText('Item 3                     $5.00');
      await ReactNativePosPrinter.printText('================================');
      
      // Print total
      const totalOptions: TextOptions = {
        align: 'RIGHT',
        bold: true,
      };
      await ReactNativePosPrinter.printText('TOTAL: $30.00', totalOptions);
      
      // Print QR code
      await ReactNativePosPrinter.printQRCode('https://example.com/receipt/123', {
        align: 'CENTER',
        size: 6,
      });
      
      // Cut paper
      await ReactNativePosPrinter.cutPaper();
      
      Alert.alert('Success', 'Receipt printed successfully');
    } catch (error) {
      Alert.alert('Error', 'Failed to print receipt');
    }
  };

  const renderDevice = ({ item }: { item: PrinterDevice }) => (
    <TouchableOpacity
      style={styles.deviceItem}
      onPress={() => connectToDevice(item)}
    >
      <Text style={styles.deviceName}>{item.name}</Text>
      <Text style={styles.deviceAddress}>{item.address}</Text>
      <Text style={styles.deviceType}>{item.type}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>POS Printer Demo</Text>
      
      {isConnected && connectedDevice && (
        <View style={styles.connectedDevice}>
          <Text style={styles.connectedText}>
            Connected to: {connectedDevice.name}
          </Text>
          <TouchableOpacity style={styles.button} onPress={disconnect}>
            <Text style={styles.buttonText}>Disconnect</Text>
          </TouchableOpacity>
        </View>
      )}
      
      {isConnected && (
        <TouchableOpacity style={styles.printButton} onPress={printReceipt}>
          <Text style={styles.buttonText}>Print Test Receipt</Text>
        </TouchableOpacity>
      )}
      
      <Text style={styles.subtitle}>Available Devices:</Text>
      <FlatList
        data={devices}
        renderItem={renderDevice}
        keyExtractor={(item) => item.address}
        style={styles.deviceList}
      />
      
      <TouchableOpacity style={styles.button} onPress={loadDevices}>
        <Text style={styles.buttonText}>Refresh Devices</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 20,
  },
  subtitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginTop: 20,
    marginBottom: 10,
  },
  connectedDevice: {
    backgroundColor: '#e8f5e8',
    padding: 15,
    borderRadius: 8,
    marginBottom: 20,
  },
  connectedText: {
    fontSize: 16,
    color: '#2e7d32',
    marginBottom: 10,
  },
  deviceList: {
    flex: 1,
    marginBottom: 20,
  },
  deviceItem: {
    backgroundColor: 'white',
    padding: 15,
    marginBottom: 10,
    borderRadius: 8,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  deviceName: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  deviceAddress: {
    fontSize: 14,
    color: '#666',
  },
  deviceType: {
    fontSize: 12,
    color: '#999',
  },
  button: {
    backgroundColor: '#2196F3',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
  },
  printButton: {
    backgroundColor: '#4CAF50',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 20,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default App;