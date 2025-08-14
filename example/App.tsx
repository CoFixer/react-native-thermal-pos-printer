import { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  Alert,
  StyleSheet,
} from 'react-native';
import ReactNativePosPrinter, {
  ThermalPrinterDevice,
} from '../src/index';

const App = () => {
  const [devices, setDevices] = useState<ThermalPrinterDevice[]>([]);
  const [connectedDevice, setConnectedDevice] = useState<ThermalPrinterDevice | null>(null);
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

  const connectToDevice = async (device: ThermalPrinterDevice) => {
    try {
      await device.connect({ timeout: 5000 });
      setConnectedDevice(device);
      setIsConnected(true);
      Alert.alert('Success', `Connected to ${device.getName()}`);
    } catch (error) {
      Alert.alert('Error', 'Failed to connect to device');
    }
  };

  const disconnect = async () => {
    try {
      if (connectedDevice) {
        await connectedDevice.disconnect();
        setConnectedDevice(null);
        setIsConnected(false);
        Alert.alert('Success', 'Disconnected from printer');
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to disconnect');
    }
  };

  const printTestReceipt = async () => {
    if (!isConnected || !connectedDevice) {
      Alert.alert('Error', 'Please connect to a printer first');
      return;
    }

    try {
      await connectedDevice.printText('=== TEST RECEIPT ===', {
        align: 'CENTER',
        size: 24,
        bold: true,
      });
      await connectedDevice.printText('Thank you for your purchase!', {
        align: 'CENTER'
      });
      await connectedDevice.cutPaper();
      Alert.alert('Success', 'Test receipt printed!');
    } catch (error) {
      Alert.alert('Error', 'Failed to print');
      console.error('Print error:', error);
    }
  };

  const renderDevice = ({ item }: { item: ThermalPrinterDevice }) => (
    <TouchableOpacity
      style={styles.deviceItem}
      onPress={() => connectToDevice(item)}
    >
      <Text style={styles.deviceName}>{item.getName()}</Text>
      <Text style={styles.deviceAddress}>{item.getAddress()}</Text>
      <Text style={styles.deviceType}>{item.getType()}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>POS Printer Demo</Text>
      
      {isConnected && connectedDevice && (
        <View style={styles.connectedDevice}>
          <Text style={styles.connectedText}>
            Connected to: {connectedDevice.getName()}
          </Text>
          <TouchableOpacity style={styles.button} onPress={disconnect}>
            <Text style={styles.buttonText}>Disconnect</Text>
          </TouchableOpacity>
        </View>
      )}

      <Text style={styles.subtitle}>Available Devices:</Text>
      <FlatList
        style={styles.deviceList}
        data={devices}
        keyExtractor={(item) => item.getAddress()}
        renderItem={renderDevice}
        refreshing={false}
        onRefresh={loadDevices}
      />

      {isConnected && (
        <TouchableOpacity style={styles.printButton} onPress={printTestReceipt}>
          <Text style={styles.buttonText}>Print Test Receipt</Text>
        </TouchableOpacity>
      )}

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