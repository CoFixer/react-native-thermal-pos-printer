module.exports = {
  dependencies: {
    'react-native-thermal-pos-printer': {
      platforms: {
        android: {
          sourceDir: '../node_modules/react-native-thermal-pos-printer/android/',
          packageImportPath: 'import com.reactnativeposprinter.PosPrinterPackage;',
        },
        ios: {
          podspecPath: '../node_modules/react-native-thermal-pos-printer/react-native-pos-printer.podspec',
        },
      },
    },
  },
};