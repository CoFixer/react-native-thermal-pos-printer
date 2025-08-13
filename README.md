# @siam/react-native-pos-printer

A comprehensive React Native package for thermal printer integration, supporting popular brands like Xprinter and other ESC/POS compatible printers.

## Features

- 🖨️ Support for thermal POS printers (Xprinter, Epson, Star, etc.)
- 📱 Cross-platform (iOS & Android)
- 🔗 Multiple connection types (Bluetooth, USB, WiFi, Ethernet)
- 📄 Text printing with advanced formatting options
- 🖼️ Base64 image printing with automatic resizing
- 📊 Comprehensive barcode support (CODE128, CODE39, EAN13, UPC-A, etc.)
- 🔲 QR code printing with customizable size and error correction
- 💰 Cash drawer control
- ✂️ Paper cutting
- 🔧 Raw ESC/POS command support
- 🎨 Font size control and text alignment
- 📐 Image scaling and positioning

## Installation

```bash
npm install @siam/react-native-pos-printer
```

### iOS Setup

1. Run pod install:
```bash
cd ios && pod install
```

2. Add Bluetooth permissions to `Info.plist`:
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app needs Bluetooth access to connect to printers</string>
<key>NSBluetoothPeripheralUsageDescription</key>
<string>This app needs Bluetooth access to connect to printers</string>
```

### Android Setup

1. Add permissions to `android/app/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

## Usage

### Basic Example

```typescript
import ReactNativePosPrinter from '@siam/react-native-pos-printer';

// Initialize the printer
await ReactNativePosPrinter.init();

// Get available devices
const devices = await ReactNativePosPrinter.getDeviceList();

// Connect to a printer
await ReactNativePosPrinter.connectPrinter(device.address, 'BLUETOOTH');

// Print text with formatting
await ReactNativePosPrinter.printText('Hello World!', {
  align: 'CENTER',
  size: 'LARGE',
  bold: true,
  fontType: 'A'
});

// Cut paper
await ReactNativePosPrinter.cutPaper();
```

### Advanced Usage

```typescript
// Print a complete receipt with images, barcodes, and QR codes
const printAdvancedReceipt = async () => {
  try {
    // Print logo (base64 image)
    const logoBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==';
    await ReactNativePosPrinter.printImage(logoBase64, {
      align: 'CENTER',
      width: 200,
      height: 100
    });
    
    // Header
    await ReactNativePosPrinter.printText('MY STORE', {
      align: 'CENTER',
      size: 'XLARGE',
      bold: true,
      fontType: 'A'
    });
    
    await ReactNativePosPrinter.printText('123 Main St, City, State', {
      align: 'CENTER',
      size: 'NORMAL'
    });
    
    await ReactNativePosPrinter.printText('Tel: (555) 123-4567', {
      align: 'CENTER'
    });
    
    await ReactNativePosPrinter.printText('\n' + '='.repeat(32) + '\n');
    
    // Items
    await ReactNativePosPrinter.printText('Item 1                    $10.00');
    await ReactNativePosPrinter.printText('Item 2                    $15.00');
    await ReactNativePosPrinter.printText('Tax                        $2.50');
    
    await ReactNativePosPrinter.printText('\n' + '-'.repeat(32) + '\n');
    
    // Total
    await ReactNativePosPrinter.printText('TOTAL                     $27.50', {
      bold: true,
      size: 'LARGE'
    });
    
    // Print barcode
    await ReactNativePosPrinter.printBarcode('1234567890123', 'EAN13', {
      align: 'CENTER',
      height: 60,
      width: 2,
      textPosition: 'BELOW'
    });
    
    // QR Code with high error correction
    await ReactNativePosPrinter.printQRCode('https://mystore.com/receipt/123', {
      align: 'CENTER',
      size: 6,
      errorLevel: 'H'
    });
    
    await ReactNativePosPrinter.printText('\nThank you for your business!\n', {
      align: 'CENTER'
    });
    
    // Cut paper
    await ReactNativePosPrinter.cutPaper();
    
  } catch (error) {
    console.error('Print error:', error);
  }
};
```

## API Reference

### Methods

#### `init(): Promise<boolean>`
Initialize the printer module.

#### `getDeviceList(): Promise<PrinterDevice[]>`
Get list of available printer devices.

#### `connectPrinter(address: string, type: string): Promise<boolean>`
Connect to a printer device.

#### `disconnectPrinter(): Promise<boolean>`
Disconnect from the current printer.

#### `isConnected(): Promise<boolean>`
Check if printer is connected.

#### `printText(text: string, options?: TextOptions): Promise<boolean>`
Print text with formatting options.

#### `printImage(base64: string, options?: ImageOptions): Promise<boolean>`
Print image from base64 string with automatic resizing and format conversion.

#### `printQRCode(data: string, options?: QRCodeOptions): Promise<boolean>`
Print QR code with customizable size and error correction.

#### `printBarcode(data: string, type: string, options?: BarcodeOptions): Promise<boolean>`
Print barcode with support for multiple formats.

#### `cutPaper(): Promise<boolean>`
Cut paper.

#### `openCashDrawer(): Promise<boolean>`
Open cash drawer.

### Types

```typescript
interface PrinterDevice {
  name: string;
  address: string;
  type: 'BLUETOOTH' | 'USB' | 'WIFI' | 'ETHERNET';
  connected: boolean;
}

interface PrintOptions {
  encoding?: string;
  codepage?: number;
}

interface TextOptions {
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  size?: 'SMALL' | 'NORMAL' | 'LARGE' | 'XLARGE';
  bold?: boolean;
  underline?: boolean;
  fontType?: 'A' | 'B' | 'C';
}

interface ImageOptions {
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  width?: number;
  height?: number;
}

interface BarcodeOptions {
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  height?: number;
  width?: number;
  textPosition?: 'NONE' | 'ABOVE' | 'BELOW';
}

interface QRCodeOptions {
  size?: number;
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  errorLevel?: 'L' | 'M' | 'Q' | 'H';
}
```

### Supported Barcode Types

- **CODE128** - High-density linear barcode
- **CODE39** - Alphanumeric barcode
- **EAN13** - European Article Number (13 digits)
- **EAN8** - European Article Number (8 digits)
- **UPC_A** - Universal Product Code
- **UPC_E** - Universal Product Code (compact)
- **ITF** - Interleaved 2 of 5
- **CODABAR** - Variable-length barcode

### QR Code Error Correction Levels

- **L** - Low (~7% correction)
- **M** - Medium (~15% correction) - Default
- **Q** - Quartile (~25% correction)
- **H** - High (~30% correction)

## Supported Printers

- Xprinter (XP-58, XP-80, XP-365B, etc.)
- Epson (TM series)
- Star Micronics
- Citizen
- Bixolon
- Any ESC/POS compatible thermal printer

## Image Printing

The package supports base64 image printing with automatic:
- Format conversion to monochrome
- Resizing to fit printer width
- ESC/POS raster format conversion
- Alignment control

```typescript
// Print image with custom dimensions
const imageBase64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==';

await ReactNativePosPrinter.printImage(imageBase64, {
  align: 'CENTER',
  width: 300,
  height: 200
});
```

## Troubleshooting

### Common Issues

1. **Bluetooth connection fails**
   - Ensure the printer is paired with the device
   - Check Bluetooth permissions
   - Make sure the printer is in pairing mode

2. **Print quality issues**
   - Check paper alignment
   - Ensure proper paper type (thermal paper)
   - Verify printer settings

3. **Characters not printing correctly**
   - Set proper encoding (UTF-8, GB2312, etc.)
   - Use appropriate codepage for your region

4. **Image not printing**
   - Ensure base64 string is valid
   - Check image dimensions (recommended max width: 384px)
   - Verify image format is supported (PNG, JPEG)

5. **Barcode/QR code issues**
   - Verify data format matches barcode type
   - Check size parameters are within printer limits
   - Ensure sufficient paper width for barcode

## Publishing

To publish this package:

```bash
# Build the package
npm run prepack

# Login to npm (if not already logged in)
npm login

# Publish with public access
npm publish --access public
```

## Contributing

Contributions are welcome! Please read our contributing guidelines and submit pull requests.

## License

MIT License - see LICENSE file for details.