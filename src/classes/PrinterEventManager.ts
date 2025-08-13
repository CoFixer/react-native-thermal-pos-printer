import { NativeEventEmitter } from 'react-native';
import { PosPrinter } from '../utils/native';
import { PrinterEvent, PrinterEventData } from '../types/events';

export class PrinterEventManager {
  private static eventEmitter = new NativeEventEmitter(PosPrinter);
  private static listeners: Map<string, Function[]> = new Map();

  static addListener(event: PrinterEvent, callback: (data: PrinterEventData) => void): () => void {
    const eventName = event.toString();
    
    if (!this.listeners.has(eventName)) {
      this.listeners.set(eventName, []);
    }
    
    this.listeners.get(eventName)!.push(callback);
    
    const subscription = this.eventEmitter.addListener(eventName, callback);
    
    return () => {
      subscription.remove();
      const callbacks = this.listeners.get(eventName);
      if (callbacks) {
        const index = callbacks.indexOf(callback);
        if (index > -1) {
          callbacks.splice(index, 1);
        }
      }
    };
  }

  static removeAllListeners(event?: PrinterEvent): void {
    if (event) {
      const eventName = event.toString();
      this.eventEmitter.removeAllListeners(eventName);
      this.listeners.delete(eventName);
    } else {
      Array.from(this.listeners.keys()).forEach(eventName => {
        this.eventEmitter.removeAllListeners(eventName);
      });
      this.listeners.clear();
    }
  }

  static emit(event: PrinterEvent, data: PrinterEventData): void {
    const eventName = event.toString();
    const callbacks = this.listeners.get(eventName);
    if (callbacks) {
      callbacks.forEach(callback => callback(data));
    }
  }
}