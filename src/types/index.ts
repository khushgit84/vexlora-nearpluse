export type ScreenTab = 'home' | 'radar' | 'chat' | 'sos' | 'circle' | 'settings';

export type SignalStrength = 'EXCELLENT' | 'STRONG' | 'MODERATE' | 'WEAK' | 'MARGINAL';

export type BlePowerMode = 'BALANCED' | 'LOW_POWER' | 'BATTERY_SAVER';

export interface SimulatedPeer {
  id: string;
  name: string;
  pseudonym: string;
  address: string;
  rssi: number;
  rssiHistory: number[];
  signalStrength: SignalStrength;
  lastSeen: string;
  isSos: boolean;
  circleCode?: string;
  connected: boolean;
  batteryLevel: number; // 0 - 100
  powerMode: BlePowerMode;
  batteryHealth: 'OPTIMAL' | 'NORMAL' | 'LOW' | 'CRITICAL';
}

export interface ChatMessage {
  id: string;
  senderPseudonym: string;
  recipientAddress: string;
  content: string;
  timestamp: string;
  isOutgoing: boolean;
  status: 'PENDING' | 'SENDING' | 'DELIVERED' | 'FAILED';
}

export interface SosAlertState {
  alertId: string;
  broadcasterName: string;
  broadcasterAddress: string;
  details: string;
  timestamp: string;
  isAcknowledged: boolean;
}
