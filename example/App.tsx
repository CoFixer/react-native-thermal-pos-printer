import React, { useEffect, useState } from 'react';
import { View, Text, ActivityIndicator, FlatList, TouchableOpacity, Alert, StyleSheet } from 'react-native';
import ReactNativePosPrinter, { ThermalPrinterDevice } from 'react-native-thermal-pos-printer';

interface PrinterItem {
  device: ThermalPrinterDevice;
}

export default function App() {
  const [printers, setPrinters] = useState<PrinterItem[]>([]);
  const [selectedPrinter, setSelectedPrinter] = useState<PrinterItem | null>(null);
  const [loading, setLoading] = useState(false);
  const base64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAYAAAACxCAYAAADNlmoDAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmVkYTJiM2ZhYywgMjAyMS8xMS8xNy0xNzoyMzoxOSAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIDIzLjEgKFdpbmRvd3MpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOjc1MDNDRDgxNUJCNjExRjBBOUJEQURDNkRBOUJDNUZCIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOjc1MDNDRDgyNUJCNjExRjBBOUJEQURDNkRBOUJDNUZCIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6NzUwM0NEN0Y1QkI2MTFGMEE5QkRBREM2REE5QkM1RkIiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6NzUwM0NEODA1QkI2MTFGMEE5QkRBREM2REE5QkM1RkIiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4PESTcAABcKElEQVR42uzdB9x35fwH8NNA2j3tPbQ0VSRCIRooMjJTRkaUTbayVyhSZpS9KSEhkZaKkgqlNGhRUSH//3mf+/reXfd5zm/dq2dcn9frev3W+Z19vuPzHdci//d//1cVTC+22WabSf1viSWWqK666qrqyiuvLCdxlrHvvvtWxx577GT/fq96rF+P3+dfrrXWWs31XJhwz3ves9pss82qxRZbrLrzzjsntY5zzz233JCzhEXLKSgomDI+U48H5V8wrP7xj3+UM1MwT2PxcgoKCqaEo2tL92kHHnjgO6677rpq6aWXbqzfm2++ubr11lvL2SkoCqCgYAF8bp5ej9fWY7PTTz+9Ovroo5dadtllq3vc4x7V4osvXv3vf/+rFl100ea1oKAogIKCeRj//Oc/By2ybj22r8cj6vG4eqwZP3z0ox+tlltuucU22GCDRuAvtdRS1R133FFdf/315cQWFAVQUHA3YMl6LFWPZdJ7gdp7VGNxL5kPIpT/rsdt5P8KK6zwr/p1iXqsUo/V6rF2PTaox8as/Hps2rWR448/vvrGN75R3fe+970M9XPve9+7+te//lVdccUVVUmwKCgKoKBgZkCw3ycb6yahTXjPqcfylllyySXveZ/73KfacMMNq/XXX79aZ511qtVWW61aaaWVqmWWWaax1vH2K6644r+Sklhs2B34wQ9+UL3kJS+x3gvrdV1L8P/5z3+urrnmmkL9FBQFUFAwjdb8Nmncrx5bJst8+faCm266abX99ttX97///av73e9+LPNG2A+5jaHxzne+szrssMOqNddcE/3zCoL/2muvrf7zn/+Uq1VQFEBBwRSxQz12rsfDqjHufcWuheSd77LLLtXuu+9ePfzhD68233zzoVbOQv/Nb37TDOmae++9dyPM++Hf//539cUvfrF697vfXf3+92Mp/3j+K6+8cu/6t5Xrj6fWoxRxFBQFUFAwIpaux671eEw1Fmhdt9/CO+64Y/WsZz2r2nPPPavVV199qA385S9/qU4++eSGuvnFL34xoeAOX3/QQQfN9Z+//vWv1e9+97vqpz/9afW5z32uuvzyyyf8nnL9X5AG3uf0epxUjxPqUSqaCooCKCjoAXy7jJq9k+CfM+gPT3jCE6qDDz642mmnnYbawGWXXVadcMIJ1Xe/+93qlFNOqf773/92LnfUUUc1/D0r/6abbmoEvypewdw8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';

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        await ReactNativePosPrinter.init();
        
        const devices = await ReactNativePosPrinter.getDeviceList();
        console.log('Available printers:', devices);
        
        const printerItems: PrinterItem[] = devices.map(device => ({ device }));
        setPrinters(printerItems || []);
      } catch (e) {
        console.error('Error initializing or getting printer list:', e);
        setPrinters([]);
        Alert.alert('Error', 'Failed to initialize or get printer list.');
      }
      setLoading(false);
    })();
  }, []);

  const handleSelectPrinter = (printer: PrinterItem) => {
    setSelectedPrinter(printer);
  };

  const print = async () => {
    if (!selectedPrinter) {
      Alert.alert('Error', 'Please select a printer first');
      return;
    }

    try {
      setLoading(true);
      await ReactNativePosPrinter.connectPrinter(selectedPrinter.device.getAddress());
      
      // Test text alignment (Fixed issue)
      await ReactNativePosPrinter.printText('=== ALIGNMENT TEST ===', {
        align: 'CENTER',
        size: 12,
        fontType: 'A'
      });
      
      await ReactNativePosPrinter.printText('Left Aligned Text', {
        align: 'LEFT',
        size: 12,
        fontType: 'A',
        bold: true
      });
      
      await ReactNativePosPrinter.printText('Center Aligned Text', {
        align: 'CENTER',
        size: 12,
        fontType: 'A'
      });
      
      await ReactNativePosPrinter.printText('Right Aligned Text', {
        align: 'RIGHT',
        size: 12,
        fontType: 'A'
      });
      
      await ReactNativePosPrinter.printText('Font A (monospace)', {
        align: 'LEFT',
        size: 12,
        fontType: 'A'
      });
      
      await ReactNativePosPrinter.printText('Font B (sans-serif)', {
        align: 'LEFT',
        size: 12,
        fontType: 'B'
      });
      
      await ReactNativePosPrinter.printText('Font C (serif)', {
        align: 'LEFT',
        size: 12,
        fontType: 'C'
      });

      await ReactNativePosPrinter.newLine();
      
      // Barcode tests
      await ReactNativePosPrinter.printText('=== BARCODE TESTS ===', {
        align: 'CENTER',
        size: 12,
        fontType: 'A',
        bold: true
      });
      
      await ReactNativePosPrinter.newLine();
      
      // CODE128 Barcode
      await ReactNativePosPrinter.printText('CODE128 Barcode:', {
        align: 'LEFT',
        size: 10
      });
      await ReactNativePosPrinter.printBarcode('123456789012', 'CODE128', {
        align: 'CENTER',
        height: 60,
        width: 2,
        textPosition: 'BELOW',
        fontSize: 8
      });
      
      await ReactNativePosPrinter.newLine();
      
      // EAN13 Barcode
      await ReactNativePosPrinter.printText('EAN13 Barcode:', {
        align: 'LEFT',
        size: 10
      });
      await ReactNativePosPrinter.printBarcode('1234567890123', 'EAN13', {
        align: 'CENTER',
        height: 50,
        width: 2,
        textPosition: 'BELOW'
      });
      
      await ReactNativePosPrinter.newLine();
      
      // CODE39 Barcode
      await ReactNativePosPrinter.printText('CODE39 Barcode:', {
        align: 'LEFT',
        size: 10
      });
      await ReactNativePosPrinter.printBarcode('HELLO123', 'CODE39', {
        align: 'CENTER',
        height: 50,
        width: 2,
        textPosition: 'BELOW'
      });
      
      await ReactNativePosPrinter.newLine();
      
      // QR Code tests
      await ReactNativePosPrinter.printText('=== QR CODE TESTS ===', {
        align: 'CENTER',
        size: 12,
        fontType: 'A',
        bold: true
      });
      
      await ReactNativePosPrinter.newLine();
      
      // Small QR Code
      await ReactNativePosPrinter.printText('Small QR Code (Size 3):', {
        align: 'LEFT',
        size: 10
      });
      await ReactNativePosPrinter.printQRCode('https://example.com', {
        align: 'CENTER',
        size: 3,
        errorLevel: 'M'
      });
      
      await ReactNativePosPrinter.newLine();
      
      // Medium QR Code
      await ReactNativePosPrinter.printText('Medium QR Code (Size 5):', {
        align: 'LEFT',
        size: 10
      });
      await ReactNativePosPrinter.printQRCode('Hello World! This is a test QR code with more data.', {
        align: 'CENTER',
        size: 5,
        errorLevel: 'H'
      });
      
      await ReactNativePosPrinter.newLine();
      
      // Large QR Code with URL
      await ReactNativePosPrinter.printText('Large QR Code (Size 6):', {
        align: 'LEFT',
        size: 10
      });
      await ReactNativePosPrinter.printQRCode('https://github.com/react-native-thermal-pos-printer', {
        align: 'CENTER',
        size: 6,
        errorLevel: 'L'
      });

      await ReactNativePosPrinter.newLine();
      
      // Add image printing test after QR codes
      await ReactNativePosPrinter.printText('=== IMAGE PRINTING TEST ===', {
        align: 'CENTER',
        size: 12,
        fontType: 'A',
        bold: true
      });
      
      await ReactNativePosPrinter.newLine();
      
      // Print the base64 image
      await ReactNativePosPrinter.printText('Base64 Image:', {
        align: 'LEFT',
        size: 10
      });
      
      await ReactNativePosPrinter.printImage(base64, {
        align: 'CENTER',
        width: 200,  // Adjust width as needed
        height: 200  // Adjust height as needed
      });
      
      await ReactNativePosPrinter.newLine();
      await ReactNativePosPrinter.feedLine();
      await ReactNativePosPrinter.feedLine();
      await ReactNativePosPrinter.disconnectPrinter();
      Alert.alert('Success', 'Printed successfully!');
    } catch (err) {
      console.log('Print Error:', err);
      Alert.alert('Print Error', err instanceof Error ? err.message : 'Failed to print');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Thermal Printer Test</Text>
      </View>
      
      <View style={styles.content}>
        {loading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#2563EB" />
            <Text style={styles.loadingText}>Loading printers...</Text>
          </View>
        ) : (
          <FlatList
            data={printers}
            keyExtractor={item => item.device.getAddress() + (item.device.getName() || '')}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={[
                  styles.printerItem,
                  selectedPrinter?.device.getAddress() === item.device.getAddress() && styles.selectedPrinter
                ]}
                onPress={() => handleSelectPrinter(item)}
                activeOpacity={0.8}
              >
                <View style={styles.printerInfo}>
                  <Text style={styles.printerName}>
                    {item.device.getName() || item.device.getAddress() || 'Unknown Printer'}
                  </Text>
                  <Text style={styles.printerAddress}>{item.device.getAddress()}</Text>
                  <Text style={styles.printerType}>{item.device.getType()}</Text>
                </View>
                {selectedPrinter?.device.getAddress() === item.device.getAddress() && (
                  <Text style={styles.selectedIndicator}>✓</Text>
                )}
              </TouchableOpacity>
            )}
          />
        )}
        
        <TouchableOpacity
          onPress={print}
          style={[
            styles.printButton,
            { backgroundColor: selectedPrinter ? '#2563EB' : '#D1D5DB' }
          ]}
          disabled={!selectedPrinter}
        >
          <Text style={styles.printButtonText}>Test Print</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F9FAFB'
  },
  header: {
    backgroundColor: '#F9FAFB',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E7EB'
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1F2937'
  },
  content: {
    flex: 1,
    padding: 16
  },
  loadingContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center'
  },
  loadingText: {
    marginTop: 16,
    color: '#2563EB',
    fontSize: 16,
    fontWeight: '600'
  },
  printerItem: {
    backgroundColor: 'white',
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#E5E7EB',
    padding: 16,
    marginBottom: 8,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  selectedPrinter: {
    borderColor: '#2563EB'
  },
  printerInfo: {
    flex: 1
  },
  printerName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1F2937'
  },
  printerAddress: {
    fontSize: 12,
    color: '#6B7280'
  },
  printerType: {
    fontSize: 12,
    color: '#6B7280',
    fontStyle: 'italic'
  },
  selectedIndicator: {
    fontSize: 22,
    color: '#2563EB',
    fontWeight: 'bold'
  },
  printButton: {
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginTop: 16
  },
  printButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold'
  }
});